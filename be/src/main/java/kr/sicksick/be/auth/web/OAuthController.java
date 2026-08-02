package kr.sicksick.be.auth.web;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.oauth.OAuthClient;
import kr.sicksick.be.auth.oauth.OAuthClientRegistry;
import kr.sicksick.be.auth.oauth.OAuthException;
import kr.sicksick.be.auth.oauth.OAuthTxCookie;
import kr.sicksick.be.auth.oauth.OAuthUserInfo;
import kr.sicksick.be.auth.service.AuthService;
import kr.sicksick.be.auth.service.EmailAlreadyLinkedException;
import kr.sicksick.be.auth.service.NextStepResolver;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.auth.token.ClientType;
import kr.sicksick.be.auth.token.RefreshTokenService;
import kr.sicksick.be.auth.token.Secrets;

/**
 * 소셜 로그인 인가 플로우.
 *
 * <p>브라우저를 실제로 이동시키는 엔드포인트라 JSON 이 아니라 302 를 돌려준다.
 * 프론트는 {@code window.location.href} 로 진입해야 한다. fetch 로 부르면 브라우저가
 * 302 를 따라가면서 카카오 도메인에 CORS 요청을 보내 실패한다.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private static final String LOGIN_PATH = "/login";
    private static final int STATE_BYTES = 32;

    private final OAuthClientRegistry clients;
    private final OAuthTxCookie txCookie;
    private final AuthService authService;
    private final NextStepResolver nextStepResolver;
    private final RefreshTokenService refreshTokens;
    private final AuthCookies authCookies;

    OAuthController(OAuthClientRegistry clients, OAuthTxCookie txCookie, AuthService authService,
                    NextStepResolver nextStepResolver, RefreshTokenService refreshTokens,
                    AuthCookies authCookies) {
        this.clients = clients;
        this.txCookie = txCookie;
        this.authService = authService;
        this.nextStepResolver = nextStepResolver;
        this.refreshTokens = refreshTokens;
        this.authCookies = authCookies;
    }

    /** 1단계 — state 를 만들어 쿠키에 담고 각 사 인가 화면으로 보낸다. */
    @GetMapping("/{provider}/authorize")
    ResponseEntity<Void> authorize(@PathVariable String provider,
                                   @RequestParam(required = false) String returnTo) {
        Optional<OAuthClient> client = Provider.from(provider).flatMap(clients::find);
        if (client.isEmpty()) {
            log.warn("등록되지 않은 프로바이더로 인가 요청: {}", provider);
            return redirect(loginError("provider_not_configured"));
        }

        String state = Secrets.randomUrlSafe(STATE_BYTES);
        OAuthTxCookie.Transaction transaction = new OAuthTxCookie.Transaction(
                state, client.get().provider(), safeReturnTo(returnTo));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(client.get().authorizeUri(state))
                .header(HttpHeaders.SET_COOKIE,
                        txCookie.create(transaction, Instant.now()).toString())
                .build();
    }

    /** 2단계 — 돌아온 인가 코드를 우리 세션으로 바꾼다. */
    @GetMapping("/{provider}/callback")
    ResponseEntity<Void> callback(@PathVariable String provider,
                                  @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String state,
                                  @RequestParam(required = false) String error,
                                  @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                  HttpServletRequest request) {
        Instant now = Instant.now();

        // 사용자가 동의 화면에서 취소한 경우. 토큰 교환을 시도하지 않는다.
        if (error != null) {
            log.info("소셜 인증 취소/거부 — provider={} error={}", provider, error);
            return redirectAndClearTx("access_denied".equals(error)
                    ? loginError("cancelled")
                    : loginError("failed"));
        }

        Optional<OAuthTxCookie.Transaction> transaction = txCookie.read(request, now);
        if (transaction.isEmpty()) {
            // 쿠키가 없거나 만료됐다. 5분을 넘겼거나 SameSite 설정이 잘못된 경우다.
            return redirectAndClearTx(loginError("expired"));
        }

        OAuthTxCookie.Transaction tx = transaction.get();

        // state 가 다르면 내가 시작하지 않은 콜백이다(CSRF).
        if (!Secrets.constantTimeEquals(tx.state(), state)) {
            log.warn("state 불일치 — provider={}", provider);
            return redirectAndClearTx(loginError("invalid_state"));
        }

        // 경로의 프로바이더와 쿠키의 프로바이더가 다르면 조작된 콜백이다.
        if (Provider.from(provider).filter(p -> p == tx.provider()).isEmpty()) {
            log.warn("프로바이더 불일치 — path={} cookie={}", provider, tx.provider());
            return redirectAndClearTx(loginError("invalid_state"));
        }

        if (code == null || code.isBlank()) {
            return redirectAndClearTx(loginError("failed"));
        }

        OAuthClient client = clients.find(tx.provider()).orElse(null);
        if (client == null) {
            return redirectAndClearTx(loginError("provider_not_configured"));
        }

        User user;
        try {
            OAuthUserInfo info = client.fetchUserInfo(code, tx.state());
            user = authService.resolveUser(info, now);
        } catch (EmailAlreadyLinkedException e) {
            log.info("이메일 선점으로 가입 중단 — provider={}", tx.provider());
            return redirectAndClearTx(loginError("email_taken"));
        } catch (OAuthException e) {
            log.warn("소셜 인증 실패 — provider={}: {}", tx.provider(), e.getMessage());
            return redirectAndClearTx(loginError("failed"));
        }

        // 여기서부터는 우리 토큰만 쓴다. 소셜 액세스 토큰은 보관하지 않는다.
        ClientType clientType = ClientType.fromUserAgent(userAgent);
        String refreshToken = refreshTokens.issue(user.getId(), clientType, now);

        String destination = destinationFor(user, tx.returnTo());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(destination))
                // 액세스 토큰은 URL 에 싣지 않는다(히스토리·Referer·로그에 남는다).
                // 프론트가 착지 후 /auth/refresh 로 받아간다.
                .header(HttpHeaders.SET_COOKIE,
                        authCookies.refreshCookie(refreshToken, clientType).toString())
                .header(HttpHeaders.SET_COOKIE, txCookie.expired().toString())
                .build();
    }

    /**
     * 가입을 마친 유저만 원래 가려던 곳으로 보낸다.
     *
     * <p>미완료 유저를 returnTo 로 보내면 온보딩을 건너뛴 채 본문에 진입한다.
     */
    private String destinationFor(User user, String returnTo) {
        String nextStep = nextStepResolver.resolve(user);
        boolean signupComplete = NextStepResolver.HOME.equals(nextStep);
        return signupComplete && returnTo != null ? returnTo : nextStep;
    }

    /**
     * 오픈 리다이렉트 방지.
     *
     * <p>{@code //evil.com} 은 브라우저가 프로토콜 상대 URL 로 해석해 외부로 나가므로
     * 슬래시 하나로 시작하는 내부 경로만 허용한다.
     */
    private String safeReturnTo(String returnTo) {
        if (returnTo == null || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return null;
        }
        return returnTo;
    }

    private String loginError(String reason) {
        return LOGIN_PATH + "?error=" + reason;
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private ResponseEntity<Void> redirectAndClearTx(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(location))
                .header(HttpHeaders.SET_COOKIE, txCookie.expired().toString())
                .build();
    }
}
