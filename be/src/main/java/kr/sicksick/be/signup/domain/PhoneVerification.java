package kr.sicksick.be.signup.domain;

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
 * 휴대폰 본인인증 시도 한 건.
 *
 * <p>현재는 SMS 발송만 목업이고 발급·만료·시도횟수·검증 흐름은 실제와 동일하다.
 * 실제 SMS 사업자를 붙일 때 발송 부분만 갈아끼우면 된다.
 */
@Entity
@Table(name = "phone_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    /** 무차별 대입을 막는 시도 상한. 초과하면 폐기하고 재발송을 요구한다. */
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String phone;

    /** 목업이어도 평문으로 두지 않는다. */
    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant verifiedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private PhoneVerification(Long userId, String phone, String codeHash, Instant expiresAt, Instant now) {
        this.userId = userId;
        this.phone = phone;
        this.codeHash = codeHash;
        this.attempts = 0;
        this.expiresAt = expiresAt;
        this.createdAt = now;
    }

    public static PhoneVerification issue(Long userId, String phone, String codeHash,
                                          Instant expiresAt, Instant now) {
        return new PhoneVerification(userId, phone, codeHash, expiresAt, now);
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void markVerified(Instant now) {
        this.verifiedAt = now;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isAttemptsExceeded() {
        return attempts >= MAX_ATTEMPTS;
    }
}
