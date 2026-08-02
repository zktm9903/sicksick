package kr.sicksick.be.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.signup.repository.UserTermAgreementRepository;

/**
 * 가입 단계 판단.
 *
 * <p>중간에 이탈했다가 재로그인해도 저장된 상태만으로 같은 지점을 다시 계산해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class NextStepResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Mock
    UserTermAgreementRepository agreements;

    @InjectMocks
    NextStepResolver resolver;

    @Test
    void 약관_동의_이력이_없으면_약관_화면으로() {
        User user = pendingUser();
        when(agreements.existsByKeyUserId(any())).thenReturn(false);

        assertThat(resolver.resolve(user)).isEqualTo(NextStepResolver.SIGNUP_TERMS);
    }

    @Test
    void 약관은_동의했지만_본인인증_전이면_휴대폰_화면으로() {
        User user = pendingUser();
        when(agreements.existsByKeyUserId(any())).thenReturn(true);

        assertThat(resolver.resolve(user)).isEqualTo(NextStepResolver.SIGNUP_PHONE);
    }

    @Test
    void 본인인증까지_마쳤지만_아직_PENDING_이면_온보딩으로() {
        User user = pendingUser();
        user.verifyPhone("01012345678", NOW);
        when(agreements.existsByKeyUserId(any())).thenReturn(true);

        assertThat(resolver.resolve(user)).isEqualTo(NextStepResolver.ONBOARDING_NICKNAME);
    }

    @Test
    void 가입을_모두_마친_ACTIVE_유저는_홈으로() {
        User user = pendingUser();
        user.verifyPhone("01012345678", NOW);
        user.activate(NOW);
        when(agreements.existsByKeyUserId(any())).thenReturn(true);

        assertThat(resolver.resolve(user)).isEqualTo(NextStepResolver.HOME);
    }

    /** id 는 DB 가 채우므로 테스트에서는 직접 심어준다. */
    private User pendingUser() {
        User user = User.pending("tester@example.com", "tester", NOW);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
