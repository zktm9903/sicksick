package kr.sicksick.be.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 운영에서 개발용 스텁 프로바이더가 켜져 있으면 기동을 실패시킨다.
 *
 * <p>스텁은 아무 인증 없이 임의의 계정으로 로그인시켜 주므로, 운영에 켜진 채 나가면
 * 인증이 통째로 무력화된다. 조용히 무시하지 않고 기동을 막는 이유다.
 */
@Component
@Profile("prod")
class OAuthStubGuard implements InitializingBean {

    private final OAuthProperties properties;

    OAuthStubGuard(OAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.stubEnabled()) {
            throw new IllegalStateException(
                    "운영 프로파일에서 sicksick.oauth.stub-enabled 가 켜져 있습니다. "
                            + "OAUTH_STUB=false 로 설정하세요.");
        }
    }
}
