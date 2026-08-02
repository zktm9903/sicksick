package kr.sicksick.be.auth.service;

import org.springframework.stereotype.Service;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.domain.UserStatus;
import kr.sicksick.be.signup.repository.UserTermAgreementRepository;

/**
 * 이 유저가 지금 가야 할 화면을 정한다.
 *
 * <p>판단을 서버에 두는 이유: 프론트에 두면 앱 버전마다 가입 순서가 갈려서, 배포 시점이
 * 다른 웹과 앱이 서로 다른 흐름을 태우게 된다. 또한 저장된 상태만 보고 계산하므로
 * 중간에 이탈했다가 재로그인해도 그 자리에서 이어진다.
 *
 * <p>반환값은 프론트 라우트 경로이며 {@code fe/src/app/routes.ts} 와 일치해야 한다.
 */
@Service
public class NextStepResolver {

    public static final String SIGNUP_TERMS = "/signup/terms";
    public static final String SIGNUP_PHONE = "/signup/phone";
    public static final String ONBOARDING_NICKNAME = "/onboarding/nickname";
    public static final String HOME = "/home";

    private final UserTermAgreementRepository agreements;

    NextStepResolver(UserTermAgreementRepository agreements) {
        this.agreements = agreements;
    }

    public String resolve(User user) {
        if (!agreements.existsByKeyUserId(user.getId())) {
            return SIGNUP_TERMS;
        }
        if (!user.isPhoneVerified()) {
            return SIGNUP_PHONE;
        }
        if (user.getStatus() == UserStatus.PENDING) {
            // 온보딩은 아직 구현 전이라 첫 화면(플레이스홀더)으로 보낸다.
            return ONBOARDING_NICKNAME;
        }
        return HOME;
    }
}
