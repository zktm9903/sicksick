package kr.sicksick.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 가입 절차 설정. {@code sicksick.signup.*}
 *
 * @param fixedVerificationCode 고정 인증번호. 비어 있으면 매번 난수로 발급한다.
 *                              <p>실제 SMS 발송을 붙이기 전까지 쓰는 임시값이다.
 *                              <b>설정돼 있으면 본인인증이 보안 기능으로 동작하지 않는다</b> —
 *                              누구나 남의 번호를 입력하고 이 값으로 통과할 수 있다.
 *                              SMS 연동 후에는 반드시 비워야 하며, 잊지 않도록
 *                              {@link FixedVerificationCodeNotice} 가 기동할 때마다
 *                              경고를 남긴다.
 */
@ConfigurationProperties(prefix = "sicksick.signup")
public record SignupProperties(String fixedVerificationCode) {

    public boolean hasFixedCode() {
        return fixedVerificationCode != null && !fixedVerificationCode.isBlank();
    }
}
