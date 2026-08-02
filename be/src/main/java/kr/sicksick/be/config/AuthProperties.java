package kr.sicksick.be.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 관련 설정. {@code sicksick.auth.*}
 *
 * @param jwtSecret            HS256 서명 키. 이 값이 바뀌면 발급된 액세스 토큰이 전부 무효가 된다.
 * @param accessTokenTtl       액세스 토큰 수명. 짧을수록 "취소 불가 구간"이 짧아진다.
 * @param refreshTokenTtlWeb   브라우저용 리프레시 수명.
 * @param refreshTokenTtlApp   웹뷰 앱용 리프레시 수명. 앱은 로그인 유지 기대치가 훨씬 길다.
 * @param refreshTokenAbsoluteTtl 회전을 아무리 반복해도 넘을 수 없는 상한.
 * @param cookieSecure         쿠키에 Secure 를 걸지. 로컬(http)에서는 false 여야 한다.
 */
@ConfigurationProperties(prefix = "sicksick.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtlWeb,
        Duration refreshTokenTtlApp,
        Duration refreshTokenAbsoluteTtl,
        boolean cookieSecure
) {

    /** HS256 은 키가 최소 256비트여야 한다. 짧으면 서명 시점이 아니라 기동 시점에 잡는다. */
    private static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "sicksick.auth.jwt-secret 은 최소 " + MIN_SECRET_BYTES + "바이트여야 합니다. "
                            + "운영에서는 JWT_SECRET 환경변수로 주입하세요.");
        }
    }

    public byte[] jwtSecretBytes() {
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}
