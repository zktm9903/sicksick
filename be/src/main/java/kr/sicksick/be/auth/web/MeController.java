package kr.sicksick.be.auth.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.domain.UserStatus;
import kr.sicksick.be.auth.service.NextStepResolver;

/**
 * 현재 로그인한 유저.
 *
 * <p>프론트는 진입 시 이걸 한 번 불러 {@code nextStep} 대로 라우팅한다. 어느 화면까지
 * 진행했는지 판단하는 책임을 서버에 두기 위한 엔드포인트다.
 */
@RestController
@RequestMapping("/api/v1/users")
class MeController {

    private final NextStepResolver nextStepResolver;

    MeController(NextStepResolver nextStepResolver) {
        this.nextStepResolver = nextStepResolver;
    }

    @GetMapping("/me")
    MeResponse me(@CurrentUser User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.isPhoneVerified(),
                nextStepResolver.resolve(user));
    }

    record MeResponse(
            Long userId,
            String email,
            String nickname,
            UserStatus status,
            boolean phoneVerified,
            String nextStep
    ) {
    }
}
