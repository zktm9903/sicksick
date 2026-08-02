package kr.sicksick.be.auth.web;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.auth.token.ClientType;
import kr.sicksick.be.auth.token.InvalidRefreshTokenException;
import kr.sicksick.be.auth.token.JwtService;
import kr.sicksick.be.auth.token.RefreshTokenService;

/** 액세스 토큰 재발급과 로그아웃. 인증 없이도 호출할 수 있어야 한다(쿠키로 판단). */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RefreshTokenService refreshTokens;
    private final AuthCookies authCookies;
    private final JwtService jwtService;
    private final UserRepository users;

    AuthController(RefreshTokenService refreshTokens, AuthCookies authCookies,
                   JwtService jwtService, UserRepository users) {
        this.refreshTokens = refreshTokens;
        this.authCookies = authCookies;
        this.jwtService = jwtService;
        this.users = users;
    }

    /**
     * 리프레시 쿠키로 새 액세스 토큰을 받는다.
     *
     * <p>프론트는 앱 진입 시와 401 을 받았을 때 이 엔드포인트를 부른다. 액세스 토큰은
     * 응답 본문으로만 나가고 프론트는 메모리에만 들고 있는다(디스크에 닿지 않는다).
     */
    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
            HttpServletRequest request) {

        // 여기 담는 문구는 그대로 사용자 화면에 뜬다. 내부 용어를 쓰지 않는다.
        String rawToken = authCookies.readRefreshToken(request)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "로그인이 필요해요."));

        Instant now = Instant.now();
        ClientType clientType = ClientType.fromUserAgent(userAgent);

        RefreshTokenService.Rotation rotation;
        try {
            rotation = refreshTokens.rotate(rawToken, clientType, now);
        } catch (InvalidRefreshTokenException e) {
            // 유효하지 않은 쿠키는 남겨둘 이유가 없다. 지워서 재로그인시킨다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, authCookies.expiredRefreshCookie().toString())
                    .build();
        }

        User user = users.findById(rotation.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "계정을 찾을 수 없어요. 다시 로그인해 주세요."));

        JwtService.IssuedAccessToken accessToken = jwtService.issue(user, now);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        authCookies.refreshCookie(rotation.rawToken(), clientType).toString())
                .body(new AccessTokenResponse(accessToken.value(), accessToken.expiresInSeconds()));
    }

    /** 이 기기의 세션만 끊는다. 다른 기기의 로그인은 유지된다. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        Optional<String> rawToken = authCookies.readRefreshToken(request);
        rawToken.ifPresent(token -> refreshTokens.revoke(token, Instant.now()));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.expiredRefreshCookie().toString())
                .build();
    }

    record AccessTokenResponse(String accessToken, long expiresIn) {
    }
}
