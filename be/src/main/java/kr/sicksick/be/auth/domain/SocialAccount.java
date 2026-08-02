package kr.sicksick.be.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저에 연결된 소셜 계정. 유저 1명이 여러 소셜을 붙일 수 있다.
 *
 * <p>{@code (provider, providerUserId)} 가 로그인 식별키다.
 */
@Entity
@Table(name = "social_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    /** 카카오는 Long, 네이버는 문자열로 주지만 우리는 전부 문자열로 보관한다. */
    @Column(nullable = false, length = 255)
    private String providerUserId;

    /** 연동 당시 스냅샷. 참고용이며 인증 판단에는 쓰지 않는다. */
    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    private Instant linkedAt;

    private SocialAccount(User user, Provider provider, String providerUserId, String email, Instant linkedAt) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.linkedAt = linkedAt;
    }

    public static SocialAccount link(User user, Provider provider, String providerUserId,
                                     String email, Instant now) {
        return new SocialAccount(user, provider, providerUserId, email, now);
    }
}
