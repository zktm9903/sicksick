package kr.sicksick.be.auth.token;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.sicksick.be.config.AuthProperties;

/**
 * 리프레시 토큰 쿠키.
 *
 * <p>왜 쿠키이고 왜 이 속성들인지:
 * <ul>
 *   <li><b>HttpOnly</b> — JS 가 못 읽으므로 XSS 한 방에 계정이 넘어가지 않는다.
 *       덤으로 Safari ITP 의 7일 만료 캡을 피한다(그 캡은 document.cookie 로 심은
 *       쿠키에만 걸린다). 웹뷰 앱에서 로그인이 오래 유지되는 데 결정적이다.</li>
 *   <li><b>SameSite=Lax</b> — 크로스사이트 POST 에 쿠키가 실리지 않으므로
 *       {@code POST /auth/refresh} 에 대한 CSRF 가 성립하지 않는다.</li>
 *   <li><b>Path</b> — 인증 엔드포인트에만 실린다. 나머지 모든 요청에서 불필요하게
 *       왕복하지 않는다.</li>
 * </ul>
 */
@Component
public class AuthCookies {

    public static final String REFRESH_COOKIE = "sicksick_refresh";

    /** 이 경로 아래에서만 쿠키가 전송된다. */
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthProperties properties;

    AuthCookies(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie refreshCookie(String rawToken, ClientType clientType) {
        Duration maxAge = clientType == ClientType.APP
                ? properties.refreshTokenTtlApp()
                : properties.refreshTokenTtlWeb();

        return base(rawToken)
                .maxAge(maxAge)
                .build();
    }

    /** 로그아웃 시 즉시 삭제. */
    public ResponseCookie expiredRefreshCookie() {
        return base("")
                .maxAge(0)
                .build();
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                // 로컬은 http 라 false 여야 한다. Safari 는 http://localhost 에서
                // Secure 쿠키를 아예 거부한다.
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH);
    }
}
