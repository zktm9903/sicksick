package kr.sicksick.be.signup.service;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.auth.token.Secrets;
import kr.sicksick.be.config.SignupProperties;
import kr.sicksick.be.signup.domain.PhoneVerification;
import kr.sicksick.be.signup.repository.PhoneVerificationRepository;

/**
 * 휴대폰 본인인증.
 *
 * <p><b>현재는 SMS 발송이 목업이다.</b> 인증번호를 만들어 저장하고 로그로만 흘린다.
 * 발급·만료·시도 제한·검증 흐름과 DB 구조는 실제와 동일하므로, 나중에 SMS 사업자를
 * 붙일 때 {@link #dispatch} 한 곳만 갈아끼우면 된다.
 *
 * <p>소셜 로그인으로는 전화번호를 받을 수 없어서(구글은 아예 미제공, 카카오는 비즈앱
 * 심사 필요) 본인인증은 어차피 별도 단계로 존재해야 한다.
 */
@Service
public class PhoneVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);

    /** 프로토타입 OtpStep 의 카운트다운(180초)과 맞춘다. */
    private static final Duration CODE_TTL = Duration.ofMinutes(3);

    private static final int CODE_LENGTH = 6;

    /** 숫자 11자리. 화면에서도 숫자만 입력받는다. */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01\\d{9}$");

    private final PhoneVerificationRepository verifications;
    private final UserRepository users;
    private final SignupProperties properties;

    PhoneVerificationService(PhoneVerificationRepository verifications, UserRepository users,
                             SignupProperties properties) {
        this.verifications = verifications;
        this.users = users;
        this.properties = properties;
    }

    /**
     * 인증번호를 발급한다.
     *
     * @return 발급된 인증번호 원문. 개발 환경에서만 응답에 실어 보낸다.
     */
    @Transactional
    public String requestCode(Long userId, String phone, Instant now) {
        String normalized = normalize(phone);

        String code = issueCode();
        verifications.save(PhoneVerification.issue(
                userId, normalized, Secrets.sha256Hex(code), now.plus(CODE_TTL), now));

        dispatch(normalized, code);
        return code;
    }

    /**
     * 실제 SMS 발송을 붙이기 전까지는 고정 인증번호를 쓸 수 있게 해 둔다.
     *
     * <p>발급 이후의 흐름(해시 저장·만료·시도 제한·검증)은 난수일 때와 완전히 같으므로,
     * 나중에 고정값을 끄기만 하면 실제 동작이 된다.
     */
    private String issueCode() {
        return properties.hasFixedCode()
                ? properties.fixedVerificationCode()
                : Secrets.randomDigits(CODE_LENGTH);
    }

    /**
     * 인증번호를 검증하고 통과하면 유저의 번호를 확정한다.
     *
     * <p>실패 사유를 세분화해 알려주지 않는다(만료인지 오답인지). 공격자에게 정보를
     * 덜 주면서 사용자에게는 "다시 시도" 안내로 충분하다.
     */
    @Transactional
    public void verify(User user, String phone, String code, Instant now) {
        String normalized = normalize(phone);

        PhoneVerification verification = verifications
                .findFirstByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow(() -> badRequest("인증번호를 먼저 요청해 주세요."));

        if (verification.isVerified()) {
            throw badRequest("이미 사용된 인증번호입니다. 다시 요청해 주세요.");
        }
        if (verification.isExpired(now)) {
            throw badRequest("인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (verification.isAttemptsExceeded()) {
            throw badRequest("인증 시도 횟수를 초과했습니다. 다시 요청해 주세요.");
        }
        if (!verification.getPhone().equals(normalized)) {
            throw badRequest("인증번호를 요청한 번호와 다릅니다.");
        }

        // 실패해도 시도 횟수는 올라간다. 무차별 대입을 막는 핵심이다.
        verification.recordAttempt();

        if (!Secrets.constantTimeEquals(verification.getCodeHash(), Secrets.sha256Hex(code))) {
            throw badRequest("인증번호가 올바르지 않습니다.");
        }

        verification.markVerified(now);
        user.verifyPhone(normalized, now);
        users.save(user);
    }

    /**
     * 실제 SMS 발송 자리.
     *
     * <p>목업 단계에서는 서버 로그로만 남긴다. 응답 본문으로 코드를 내보내는 것은
     * 컨트롤러가 개발 프로파일에서만 판단한다.
     */
    private void dispatch(String phone, String code) {
        log.info("[본인인증 목업] {} 로 인증번호 {} 발송 (실제 SMS 는 발송되지 않음)", masked(phone), code);
    }

    private String normalize(String phone) {
        if (phone == null) {
            throw badRequest("휴대폰 번호를 입력해 주세요.");
        }
        String digits = phone.replaceAll("\\D", "");
        if (!PHONE_PATTERN.matcher(digits).matches()) {
            throw badRequest("올바른 휴대폰 번호가 아닙니다.");
        }
        return digits;
    }

    /** 로그에 전체 번호를 남기지 않는다. */
    private String masked(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
