package kr.sicksick.be.auth.oauth;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import kr.sicksick.be.auth.domain.Provider;

/**
 * 앱 키 없이 로그인 전 구간을 돌려보기 위한 개발용 대역.
 *
 * <p>실제 카카오·네이버 대신 우리 서버의 가짜 동의 화면으로 보낸다. 거기서 입력한
 * 식별자를 그대로 인가 코드로 삼아 콜백으로 돌아온다. state 검증·쿠키·토큰 발급 등
 * 나머지 경로는 실제와 완전히 동일하게 지나가므로, 키가 나오면 설정만 바꾸면 된다.
 *
 * <p>운영에서 켜지면 인증이 통째로 무력화되므로 {@code OAuthStubGuard} 가 기동을 막는다.
 */
class StubOAuthClient implements OAuthClient {

    private final Provider provider;
    private final String consentUrl;

    StubOAuthClient(Provider provider, String baseUrl) {
        this.provider = provider;
        this.consentUrl = baseUrl + "/api/v1/auth/oauth/stub/consent";
    }

    @Override
    public Provider provider() {
        return provider;
    }

    @Override
    public URI authorizeUri(String state) {
        return UriComponentsBuilder.fromUriString(consentUrl)
                .queryParam("provider", provider.pathName())
                .queryParam("state", state)
                .build()
                .toUri();
    }

    /**
     * 스텁에서는 인가 코드가 곧 테스트용 식별자다.
     *
     * <p>같은 값으로 다시 로그인하면 같은 계정으로 붙으므로 "재로그인 시 이어하기"
     * 동작까지 확인할 수 있다.
     */
    @Override
    public OAuthUserInfo fetchUserInfo(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new OAuthException("스텁 로그인에 식별자가 없습니다");
        }
        String identifier = code.trim();
        return new OAuthUserInfo(
                provider,
                provider.pathName() + "-" + identifier,
                identifier + "@" + provider.pathName() + ".stub.local",
                identifier);
    }
}
