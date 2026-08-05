package kr.sicksick.be.auth.web;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.service.LocalAccountService;
import kr.sicksick.be.auth.service.NextStepResolver;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.auth.token.ClientType;
import kr.sicksick.be.auth.token.RefreshTokenService;

/**
 * 이메일·비밀번호 가입과 로그인.
 *
 * <p>{@link AuthController}(토큰 재발급·로그아웃)와 경로는 같지만 성격이 다르다. 여기는
 * 자격증명을 확인해 <b>세션을 시작</b>하는 쪽이다.
 *
 * <p>소셜 콜백과 달리 fetch 로 호출되므로 리다이렉트가 아니라 JSON 을 준다. 다만 세션을
 * 심는 방식은 완전히 같다 — 리프레시 쿠키를 내려보내고, 액세스 토큰은 프론트가 착지 후
 * {@code /auth/refresh} 로 받아간다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class LocalAuthController {

    private final LocalAccountService localAccounts;
    private final NextStepResolver nextStepResolver;
    private final RefreshTokenService refreshTokens;
    private final AuthCookies authCookies;

    LocalAuthController(LocalAccountService localAccounts, NextStepResolver nextStepResolver,
                        RefreshTokenService refreshTokens, AuthCookies authCookies) {
        this.localAccounts = localAccounts;
        this.nextStepResolver = nextStepResolver;
        this.refreshTokens = refreshTokens;
        this.authCookies = authCookies;
    }

    @PostMapping("/signup")
    ResponseEntity<StepResponse> signUp(
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @Valid @RequestBody SignUpRequest request) {

        Instant now = Instant.now();
        User user = localAccounts.signUp(request.email(), request.password(), now);

        return startSession(user, userAgent, now, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<StepResponse> login(
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            @Valid @RequestBody LoginRequest request) {

        User user = localAccounts.login(request.email(), request.password());

        return startSession(user, userAgent, Instant.now(), HttpStatus.OK);
    }

    /**
     * 리프레시 쿠키를 심고 갈 곳을 알려준다.
     *
     * <p>{@code User-Agent} 를 반드시 넘겨야 한다. 앱(웹뷰)에서 들어온 요청은 토큰 수명이
     * 길게(90일) 발급되는데, 그 판단이 UA 하나에 달려 있다({@link ClientType}).
     */
    private ResponseEntity<StepResponse> startSession(User user, String userAgent,
                                                      Instant now, HttpStatus status) {
        ClientType clientType = ClientType.fromUserAgent(userAgent);
        String refreshToken = refreshTokens.issue(user.getId(), clientType, now);

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE,
                        authCookies.refreshCookie(refreshToken, clientType).toString())
                .body(new StepResponse(nextStepResolver.resolve(user)));
    }

    // 검증 메시지는 그대로 사용자 화면에 뜬다. 기본 문구("공백일 수 없습니다")는 어느
    // 항목인지 알려주지 않으므로 항목마다 직접 적는다.

    record SignUpRequest(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않아요.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Size(min = 8, message = "비밀번호는 8자 이상 입력해 주세요.")
            String password) {
    }

    /**
     * 로그인은 형식을 검사하지 않는다.
     *
     * <p>가입 규칙은 나중에 바뀔 수 있고, 그때 옛 규칙으로 가입한 사람이 형식 검사에 걸려
     * 로그인하지 못하면 안 된다. 비어 있는지만 본다.
     */
    record LoginRequest(
            @NotBlank(message = "이메일을 입력해 주세요.") String email,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {
    }

    record StepResponse(String nextStep) {
    }
}
