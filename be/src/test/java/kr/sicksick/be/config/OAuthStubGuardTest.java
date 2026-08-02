package kr.sicksick.be.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 개발용 스텁이 운영에 켜진 채 나가는 것을 막는 안전장치.
 *
 * <p>스텁은 아무 인증 없이 임의 계정으로 로그인시켜 주므로, 운영에서 켜지면 인증이
 * 통째로 무력화된다. 조용히 무시하는 대신 기동을 실패시킨다.
 *
 * <p>이 장치는 {@code prod} 프로파일에서만 등록되므로 배포 구성이
 * {@code SPRING_PROFILES_ACTIVE=prod} 를 넘겨야 의미가 있다(compose.prod.yaml).
 */
class OAuthStubGuardTest {

    @Test
    void 운영에서_스텁이_켜져_있으면_기동을_실패시킨다() {
        OAuthStubGuard guard = new OAuthStubGuard(propertiesWithStub(true));

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH_STUB=false");
    }

    @Test
    void 스텁이_꺼져_있으면_정상_기동한다() {
        OAuthStubGuard guard = new OAuthStubGuard(propertiesWithStub(false));

        guard.afterPropertiesSet();
    }

    /**
     * 기본값이 켜짐이라는 사실을 못박는다. 이 때문에 위 안전장치가 필요하다 —
     * 환경변수를 빠뜨린 배포가 곧 스텁이 켜진 배포가 된다.
     */
    @Test
    void 설정을_빠뜨리면_스텁이_켜진다는_전제를_고정한다() {
        assertThat(propertiesWithStub(true).stubEnabled()).isTrue();
    }

    private OAuthProperties propertiesWithStub(boolean stubEnabled) {
        return new OAuthProperties(
                "https://sicksick.kr",
                stubEnabled,
                new OAuthProperties.Credentials("kakao-id", "kakao-secret"),
                new OAuthProperties.Credentials("naver-id", "naver-secret"));
    }
}
