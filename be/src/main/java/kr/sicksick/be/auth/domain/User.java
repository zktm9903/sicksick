package kr.sicksick.be.auth.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 유저. 소셜 가입이든 자체 가입이든 하나의 테이블로 다룬다.
 *
 * <p>이메일은 식별키가 아니다. 카카오는 이메일이 선택 동의라 아예 없을 수 있고,
 * 유저가 소셜 계정 이메일을 바꿀 수도 있다. 식별은 {@link SocialAccount} 의
 * (provider, providerUserId) 로 한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소셜에서 못 받으면 null 이다. 온보딩에서 직접 입력받는 경로가 필요하다. */
    @Column(length = 255)
    private String email;

    /**
     * 자체 가입 계정의 비밀번호 해시. 소셜로만 가입한 계정은 null 이다.
     *
     * <p>알고리즘 접두사를 포함한다({@code {bcrypt}$2a$10$...}). 원문은 어디에도 남기지 않는다.
     */
    @Column(length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String nickname;

    /** 온보딩에서 입력받는다. */
    private LocalDate birthDate;

    /**
     * 키·몸무게는 선택 입력이라 null 일 수 있다.
     *
     * <p>현재값만 둔다. 체중 변화 추이가 필요해지면 별도 이력 테이블을 만든다.
     */
    private Short heightCm;

    private Short weightKg;

    @Column(length = 20)
    private String phone;

    private Instant phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deletedAt;

    private User(String email, String nickname, UserStatus status, Instant now) {
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 소셜 인증 직후 만들어지는, 아직 가입을 마치지 않은 유저. */
    public static User pending(String email, String nickname, Instant now) {
        return new User(email, nickname, UserStatus.PENDING, now);
    }

    /**
     * 이메일·비밀번호로 직접 가입한, 아직 가입을 마치지 않은 유저.
     *
     * <p>닉네임은 받지 않는다. 소셜 가입과 마찬가지로 온보딩에서 입력받는다.
     */
    public static User localPending(String email, String passwordHash, Instant now) {
        User user = new User(email, null, UserStatus.PENDING, now);
        user.passwordHash = passwordHash;
        return user;
    }

    /** 비밀번호 로그인이 가능한 계정인지. 소셜로만 가입했으면 false. */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** 본인인증 성공 시점에 번호를 확정한다. */
    public void verifyPhone(String phone, Instant now) {
        this.phone = phone;
        this.phoneVerifiedAt = now;
        this.updatedAt = now;
    }

    /** 온보딩까지 마쳐 정상 이용이 가능해졌을 때. */
    public void activate(Instant now) {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void changeNickname(String nickname, Instant now) {
        this.nickname = nickname;
        this.updatedAt = now;
    }

    /** 온보딩에서 받은 프로필. 키·몸무게는 건너뛸 수 있어 null 을 허용한다. */
    public void updateProfile(String nickname, LocalDate birthDate,
                              Short heightCm, Short weightKg, Instant now) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.updatedAt = now;
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }
}
