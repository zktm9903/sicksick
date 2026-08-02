package kr.sicksick.be.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 고정 인증번호가 켜져 있으면 기동할 때마다 경고를 남긴다.
 *
 * <p>실제 SMS 발송을 붙이기 전까지 쓰는 임시 설정인데, 조용히 동작하면 그대로 잊힌 채
 * 서비스가 열릴 수 있다. 그 상태에서는 본인인증이 아무것도 검증하지 못한다 — 누구나
 * 남의 번호를 입력하고 통과할 수 있다.
 *
 * <p>기동을 막지 않는 이유는 지금이 의도된 상태이기 때문이다. 다만 배포 로그를 볼 때마다
 * 눈에 걸리도록 남겨서, SMS 연동 시점에 반드시 걷어내게 한다.
 */
@Component
class FixedVerificationCodeNotice implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(FixedVerificationCodeNotice.class);

    private final SignupProperties properties;

    FixedVerificationCodeNotice(SignupProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.hasFixedCode()) {
            log.warn("""
                    ┌────────────────────────────────────────────────────────────┐
                    │ 휴대폰 인증번호가 고정값으로 발급됩니다.                   │
                    │ 실제 SMS 는 발송되지 않으며, 본인인증이 검증하는 것이      │
                    │ 없습니다 — 누구나 임의의 번호로 통과할 수 있습니다.        │
                    │                                                            │
                    │ SMS 발송을 붙인 뒤 SIGNUP_FIXED_CODE 를 비우세요.          │
                    └────────────────────────────────────────────────────────────┘""");
        }
    }
}
