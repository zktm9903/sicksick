package kr.sicksick.be.auth.token;

import java.time.Instant;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.config.AuthProperties;

/**
 * 액세스 토큰 발급.
 *
 * <p>검증은 여기서 하지 않는다. Spring Security 의 리소스 서버 필터가
 * {@code SecurityConfig} 에 등록된 디코더로 처리한다.
 *
 * <p>토큰에 status 를 넣는 이유: 가입을 마치지 않은(PENDING) 유저를 걸러내는 판단을
 * 매 요청 DB 조회 없이 하기 위해서다. 대신 상태가 바뀌어도 최대 액세스 토큰 수명만큼
 * 옛 값이 남으므로, 상태 전환 직후에는 클라이언트가 토큰을 새로 받아야 한다.
 */
@Service
public class JwtService {

    public static final String CLAIM_STATUS = "status";

    private final AuthProperties properties;
    private final MACSigner signer;

    JwtService(AuthProperties properties) {
        this.properties = properties;
        try {
            this.signer = new MACSigner(new SecretKeySpec(properties.jwtSecretBytes(), "HmacSHA256"));
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT 서명기를 만들 수 없습니다", e);
        }
    }

    public IssuedAccessToken issue(User user, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_STATUS, user.getStatus().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT 서명에 실패했습니다", e);
        }

        return new IssuedAccessToken(jwt.serialize(), expiresAt,
                properties.accessTokenTtl().toSeconds());
    }

    public record IssuedAccessToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
