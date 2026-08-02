package kr.sicksick.be.signup.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.service.NextStepResolver;
import kr.sicksick.be.auth.web.CurrentUser;
import kr.sicksick.be.config.SignupProperties;
import kr.sicksick.be.signup.service.PhoneVerificationService;
import kr.sicksick.be.signup.service.TermsService;

/**
 * 가입 절차 — 약관 동의와 본인인증.
 *
 * <p>각 단계는 인증된 상태(PENDING 유저)에서 호출된다. 응답에 다음 단계를 실어주므로
 * 프론트는 순서를 직접 알 필요가 없다.
 */
@RestController
@RequestMapping("/api/v1/signup")
class SignupController {

    private final TermsService termsService;
    private final PhoneVerificationService phoneVerifications;
    private final NextStepResolver nextStepResolver;
    private final boolean exposeCode;

    SignupController(TermsService termsService, PhoneVerificationService phoneVerifications,
                     NextStepResolver nextStepResolver, SignupProperties signupProperties,
                     @Value("${sicksick.oauth.stub-enabled:false}") boolean stubEnabled) {
        this.termsService = termsService;
        this.phoneVerifications = phoneVerifications;
        this.nextStepResolver = nextStepResolver;

        // 인증번호를 응답에 실어 보낼지.
        //
        // 고정값이면 감출 이유가 없다 — 모두에게 같은 값이라 비밀이 아니고, SMS 도
        // 나가지 않으므로 감추면 아무도 가입을 마칠 수 없다.
        // 난수로 발급하는 경우에는 개발 환경(스텁)에서만 노출한다.
        this.exposeCode = signupProperties.hasFixedCode() || stubEnabled;
    }

    @PostMapping("/terms")
    StepResponse agreeTerms(@CurrentUser User user, @Valid @RequestBody TermsRequest request) {
        termsService.agree(user.getId(), request.agreements(), Instant.now());
        return new StepResponse(nextStepResolver.resolve(user));
    }

    @PostMapping("/phone/code")
    PhoneCodeResponse requestPhoneCode(@CurrentUser User user, @Valid @RequestBody PhoneRequest request) {
        String code = phoneVerifications.requestCode(user.getId(), request.phone(), Instant.now());
        return new PhoneCodeResponse(exposeCode ? code : null);
    }

    @PostMapping("/phone/verify")
    StepResponse verifyPhone(@CurrentUser User user, @Valid @RequestBody PhoneVerifyRequest request) {
        phoneVerifications.verify(user, request.phone(), request.code(), Instant.now());
        return new StepResponse(nextStepResolver.resolve(user));
    }

    // 검증 메시지는 그대로 사용자 화면에 뜬다. 기본 문구("공백일 수 없습니다")는 어느
    // 항목인지 알려주지 않으므로 항목마다 직접 적는다.

    /** 약관 코드 → 동의 여부. 선택 약관의 false 도 그대로 기록한다. */
    record TermsRequest(
            @NotNull(message = "약관 동의 정보가 필요해요.") Map<String, Boolean> agreements) {
    }

    record PhoneRequest(
            @NotBlank(message = "휴대폰 번호를 입력해 주세요.") String phone) {
    }

    record PhoneVerifyRequest(
            @NotBlank(message = "휴대폰 번호를 입력해 주세요.") String phone,
            @NotBlank(message = "인증번호를 입력해 주세요.") String code) {
    }

    /**
     * 인증번호를 화면에 안내하기 위한 값.
     *
     * <p>SMS 발송이 붙기 전까지는 고정값이라 감출 것이 없어 그대로 내려간다.
     * 실제 발송이 붙고 난수 발급으로 돌아가면 개발 환경 외에는 null 이 된다.
     */
    record PhoneCodeResponse(String devCode) {
    }

    record StepResponse(String nextStep) {
    }
}
