package kr.sicksick.be.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰. 실질적으로 "수명이 긴 세션"이다.
 *
 * <p>액세스 토큰(JWT)은 무상태라 취소가 불가능하므로, 즉시 차단이 필요한 경우
 * (로그아웃·탈퇴·탈취)를 위해 리프레시만 서버에 상태로 남긴다.
 *
 * <p>원문은 저장하지 않고 SHA-256 해시만 남긴다. 비밀번호를 해시로 두는 것과 같은 이유다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    /**
     * 회전해도 물려받는 최초 발급 시각.
     *
     * <p>슬라이딩 만료만 있으면 토큰 체인이 무한히 이어져서, 탈취당한 기기가 영구
     * 접근권을 갖는다. 이 값을 기준으로 절대 만료를 건다.
     */
    @Column(nullable = false)
    private Instant chainStartedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private RefreshToken(Long userId, String tokenHash, Instant chainStartedAt,
                         Instant expiresAt, Instant now) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.chainStartedAt = chainStartedAt;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    /** 로그인 시점의 첫 발급. 체인이 여기서 시작한다. */
    public static RefreshToken issue(Long userId, String tokenHash, Instant expiresAt, Instant now) {
        return new RefreshToken(userId, tokenHash, now, expiresAt, now);
    }

    /** 회전 발급. 체인 시작 시각을 그대로 물려받아 절대 만료가 초기화되지 않게 한다. */
    public RefreshToken rotateTo(String nextTokenHash, Instant nextExpiresAt, Instant now) {
        return new RefreshToken(userId, nextTokenHash, chainStartedAt, nextExpiresAt, now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
