package kr.sicksick.be.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.repository.UserRepository;

/**
 * 이메일·비밀번호 자체 가입과 로그인.
 *
 * <p>{@link AuthService}(소셜)와 나란히 서는 또 하나의 진입 수단이다. 어느 쪽이든 결과는
 * 같다 — {@code PENDING} 유저가 만들어지고, 이후 단계는 {@code NextStepResolver} 가 DB
 * 상태만 보고 계산한다. 그래서 가입 도중 이탈해도 다시 로그인하면 그 자리에서 이어진다.
 */
@Service
public class LocalAccountService {

    private static final Logger log = LoggerFactory.getLogger(LocalAccountService.class);

    /**
     * BCrypt 는 72바이트를 넘는 비밀번호를 거부한다(넘기면 예외가 나 500 이 된다).
     *
     * <p>검증 애너테이션의 {@code @Size} 는 <b>문자 수</b>를 세므로 이걸 못 잡는다.
     * 한글은 UTF-8 로 3바이트라 24자만 넘어도 한계에 걸린다.
     */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    /**
     * 존재하지 않는 계정에 대해서도 한 번 돌려볼 해시.
     *
     * <p>없는 이메일이라고 즉시 반환하면 응답 시간만으로 가입 여부를 알아낼 수 있다.
     * 화면에서는 가입 여부를 구분해 보여주기로 했지만(그건 우리가 선택해 노출하는 것),
     * 타이밍으로 새는 것은 통제 밖이라 별개로 막는다.
     */
    private final String dummyHash;

    LocalAccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        // 아무도 알 수 없는 값이어야 한다. 기동 시 한 번만 계산한다.
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * 새 계정을 만든다. 아직 약관·본인인증 전이므로 {@code PENDING} 이다.
     *
     * @throws EmailAlreadyLinkedException 이미 그 이메일로 가입된 계정이 있는 경우
     */
    @Transactional
    public User signUp(String email, String rawPassword, Instant now) {
        String normalized = normalizeEmail(email);
        checkPasswordLength(rawPassword);

        if (users.existsByEmailAndDeletedAtIsNull(normalized)) {
            throw new EmailAlreadyLinkedException(normalized);
        }

        try {
            User user = users.saveAndFlush(
                    User.localPending(normalized, passwordEncoder.encode(rawPassword), now));
            log.info("자체 가입 — userId={}", user.getId());
            return user;
        } catch (DataIntegrityViolationException e) {
            // 같은 이메일로 두 번 제출된 경합. 유니크 제약이 막아줬으니 안내로 바꿔 준다.
            // (AuthService.signUp 과 달리 기존 계정을 그냥 쓸 수는 없다 — 비밀번호가 다를 수 있다)
            log.info("자체 가입 경합 감지 — 이미 선점된 이메일");
            throw new EmailAlreadyLinkedException(normalized);
        }
    }

    /**
     * 이메일·비밀번호를 확인하고 유저를 돌려준다.
     *
     * @throws LoginFailedException 계정이 없거나, 소셜 전용이거나, 비밀번호가 다른 경우
     */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        User user = users.findByEmailAndDeletedAtIsNull(normalizeEmail(email)).orElse(null);

        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new LoginFailedException(LoginFailedException.Reason.ACCOUNT_NOT_FOUND);
        }
        if (!user.hasPassword()) {
            throw new LoginFailedException(LoginFailedException.Reason.SOCIAL_ONLY);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new LoginFailedException(LoginFailedException.Reason.BAD_PASSWORD);
        }

        return user;
    }

    /**
     * 이메일 정규화.
     *
     * <p>MySQL 기본 콜레이션(utf8mb4_0900_ai_ci)이 대소문자를 구분하지 않아 조회는 알아서
     * 맞지만, 거기 기대면 콜레이션이 바뀌는 순간 같은 이메일로 계정이 둘 생긴다.
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 입력해 주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void checkPasswordLength(String rawPassword) {
        if (rawPassword == null
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "비밀번호가 너무 길어요. 영문·숫자 기준 72자 이내로 입력해 주세요.");
        }
    }
}
