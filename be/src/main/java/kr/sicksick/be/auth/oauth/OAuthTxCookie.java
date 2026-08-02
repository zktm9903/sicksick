package kr.sicksick.be.auth.oauth;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.config.AuthProperties;

/**
 * 인가 플로우 한 건의 상태를 담아두는 단기 쿠키.
 *
 * <p>서버 세션 대신 서명 쿠키를 쓴다. 인스턴스가 늘어도 세션 클러스터링이 필요 없고
 * Redis 같은 별도 저장소도 필요 없다. 담기는 값은 비밀이 아니라 위조만 막으면 되므로
 * 암호화 없이 서명으로 충분하다.
 *
 * <p><b>SameSite=Lax 가 필수다.</b> Strict 로 두면 카카오·네이버 도메인에서 콜백으로
 * 돌아오는 순간 브라우저가 쿠키를 보내지 않아 state 검증이 항상 실패한다. Lax 는
 * top-level GET 네비게이션에는 쿠키를 실어주므로 콜백에서 정상 동작한다.
 */
@Component
public class OAuthTxCookie {

    private static final Logger log = LoggerFactory.getLogger(OAuthTxCookie.class);

    public static final String COOKIE_NAME = "sicksick_oauth_tx";

    /** 인가 플로우는 이 안에 끝난다. 길게 두면 재생 공격 여지만 늘어난다. */
    private static final Duration TTL = Duration.ofMinutes(5);

    private static final String COOKIE_PATH = "/api/v1/auth/oauth";

    private static final String CLAIM_STATE = "state";
    private static final String CLAIM_PROVIDER = "provider";
    private static final String CLAIM_RETURN_TO = "returnTo";

    private final AuthProperties properties;
    private final MACSigner signer;
    private final MACVerifier verifier;

    OAuthTxCookie(AuthProperties properties) {
        this.properties = properties;
        try {
            SecretKeySpec key = new SecretKeySpec(properties.jwtSecretBytes(), "HmacSHA256");
            this.signer = new MACSigner(key);
            this.verifier = new MACVerifier(key);
        } catch (JOSEException e) {
            throw new IllegalStateException("OAuth 트랜잭션 쿠키 서명기를 만들 수 없습니다", e);
        }
    }

    public ResponseCookie create(Transaction transaction, Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim(CLAIM_STATE, transaction.state())
                .claim(CLAIM_PROVIDER, transaction.provider().name())
                .claim(CLAIM_RETURN_TO, transaction.returnTo())
                .expirationTime(Date.from(now.plus(TTL)))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("OAuth 트랜잭션 쿠키 서명에 실패했습니다", e);
        }

        return base(jwt.serialize()).maxAge(TTL).build();
    }

    /** 콜백 처리 직후 반드시 지운다. 1회용이다. */
    public ResponseCookie expired() {
        return base("").maxAge(0).build();
    }

    /** 서명·만료가 유효할 때만 값을 돌려준다. 위조·만료는 빈 Optional 이다. */
    public Optional<Transaction> read(HttpServletRequest request, Instant now) {
        return rawValue(request).flatMap(value -> parse(value, now));
    }

    private Optional<Transaction> parse(String value, Instant now) {
        try {
            SignedJWT jwt = SignedJWT.parse(value);
            if (!jwt.verify(verifier)) {
                log.warn("OAuth 트랜잭션 쿠키 서명이 유효하지 않습니다");
                return Optional.empty();
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || !now.isBefore(expiration.toInstant())) {
                return Optional.empty();
            }

            // 람다 안에서는 검사 예외를 던질 수 없으므로 클레임을 먼저 꺼낸다.
            String state = claims.getStringClaim(CLAIM_STATE);
            String returnTo = claims.getStringClaim(CLAIM_RETURN_TO);
            return Provider.from(claims.getStringClaim(CLAIM_PROVIDER))
                    .map(provider -> new Transaction(state, provider, returnTo));
        } catch (ParseException | JOSEException e) {
            log.warn("OAuth 트랜잭션 쿠키를 해석할 수 없습니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> rawValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                // Strict 면 프로바이더에서 돌아오는 콜백에 쿠키가 실리지 않는다.
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }

    /**
     * @param state    콜백에서 대조할 CSRF 토큰
     * @param provider 어느 프로바이더로 시작한 플로우인지(경로 뒤바꿔치기 방지)
     * @param returnTo 로그인 후 돌아갈 내부 경로
     */
    public record Transaction(String state, Provider provider, String returnTo) {
    }
}
