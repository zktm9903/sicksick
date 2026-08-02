package kr.sicksick.be.auth.token;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.sicksick.be.auth.domain.RefreshToken;
import kr.sicksick.be.auth.repository.RefreshTokenRepository;
import kr.sicksick.be.config.AuthProperties;

/**
 * 리프레시 토큰 발급·회전.
 *
 * <p>세 가지 정책이 맞물려 있다.
 * <ul>
 *   <li><b>회전</b> — 갱신할 때마다 기존 토큰을 폐기하고 새로 발급한다.</li>
 *   <li><b>슬라이딩 만료</b> — 회전하면서 만료 시각도 민다. 계속 쓰는 한 로그인이 유지된다.</li>
 *   <li><b>재사용 탐지</b> — 이미 폐기된 토큰이 다시 오면 탈취로 보고 그 유저의 토큰을 전부 끊는다.
 *       리프레시 토큰 유출을 알아챌 수 있는 사실상 유일한 실용적 수단이다.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 쿠키에 실리는 원문 길이. 불투명 난수이므로 의미를 담지 않는다. */
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokens;
    private final RefreshTokenRevoker revoker;
    private final AuthProperties properties;

    RefreshTokenService(RefreshTokenRepository refreshTokens, RefreshTokenRevoker revoker,
                        AuthProperties properties) {
        this.refreshTokens = refreshTokens;
        this.revoker = revoker;
        this.properties = properties;
    }

    /** 로그인 시점의 최초 발급. */
    @Transactional
    public String issue(Long userId, ClientType clientType, Instant now) {
        String raw = Secrets.randomUrlSafe(TOKEN_BYTES);
        refreshTokens.save(RefreshToken.issue(
                userId, Secrets.sha256Hex(raw), now.plus(ttlFor(clientType)), now));
        return raw;
    }

    /**
     * 제시된 토큰을 검증하고 새 토큰으로 회전한다.
     *
     * @throws InvalidRefreshTokenException 알 수 없거나 만료됐거나 재사용된 토큰
     */
    @Transactional
    public Rotation rotate(String rawToken, ClientType clientType, Instant now) {
        RefreshToken stored = refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("알 수 없는 리프레시 토큰"));

        if (stored.isRevoked()) {
            // 정상 클라이언트는 폐기된 토큰을 다시 보내지 않는다. 유출로 간주하고 체인을 끊는다.
            // 아래 예외로 이 트랜잭션은 롤백되므로, 폐기는 반드시 별도 트랜잭션에서 커밋해야 한다.
            int revoked = revoker.revokeAllByUserId(stored.getUserId(), now);
            log.warn("리프레시 토큰 재사용 감지 — userId={} 활성 토큰 {}건 폐기", stored.getUserId(), revoked);
            throw new InvalidRefreshTokenException("재사용된 리프레시 토큰");
        }

        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException("만료된 리프레시 토큰");
        }

        // 슬라이딩이 무한히 이어지지 않도록 최초 발급 시점 기준 상한을 둔다.
        Instant absoluteDeadline = stored.getChainStartedAt().plus(properties.refreshTokenAbsoluteTtl());
        if (!now.isBefore(absoluteDeadline)) {
            stored.revoke(now);
            throw new InvalidRefreshTokenException("절대 만료를 초과한 리프레시 토큰");
        }

        stored.revoke(now);

        String nextRaw = Secrets.randomUrlSafe(TOKEN_BYTES);
        Instant nextExpiresAt = nextExpiresAt(stored, clientType, now, absoluteDeadline);
        refreshTokens.save(stored.rotateTo(Secrets.sha256Hex(nextRaw), nextExpiresAt, now));

        return new Rotation(stored.getUserId(), nextRaw);
    }

    /** 로그아웃. 해당 토큰만 끊는다(다른 기기의 세션은 유지). */
    @Transactional
    public void revoke(String rawToken, Instant now) {
        refreshTokens.findByTokenHash(Secrets.sha256Hex(rawToken))
                .ifPresent(token -> token.revoke(now));
    }

    /**
     * 새 만료 시각.
     *
     * <p>매번 새로 미는 대신 남은 수명이 절반 아래로 떨어졌을 때만 연장한다.
     * 앱을 열 때마다 만료를 갱신하면 쓰기가 불필요하게 잦아진다.
     */
    private Instant nextExpiresAt(RefreshToken stored, ClientType clientType,
                                  Instant now, Instant absoluteDeadline) {
        Duration ttl = ttlFor(clientType);
        Duration remaining = Duration.between(now, stored.getExpiresAt());

        Instant extended = remaining.compareTo(ttl.dividedBy(2)) < 0
                ? now.plus(ttl)
                : stored.getExpiresAt();

        // 절대 만료를 넘겨서 연장하지 않는다.
        return extended.isAfter(absoluteDeadline) ? absoluteDeadline : extended;
    }

    private Duration ttlFor(ClientType clientType) {
        return clientType == ClientType.APP
                ? properties.refreshTokenTtlApp()
                : properties.refreshTokenTtlWeb();
    }

    /** 회전 결과 — 누구의 토큰이었고, 새 원문은 무엇인지. */
    public record Rotation(Long userId, String rawToken) {
    }
}
