package kr.sicksick.be.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.config.OAuthProperties;

class KakaoOAuthClientTest {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private MockRestServiceServer server;
    private KakaoOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoOAuthClient(
                builder.build(),
                new OAuthProperties.Credentials("rest-api-key", "client-secret"),
                "http://localhost:5173/api/v1/auth/oauth/kakao/callback");
    }

    @Test
    void 숫자로_오는_id_를_문자열로_보관한다() {
        stubToken();
        server.expect(requestTo(USER_INFO_URL))
                .andRespond(withSuccess("""
                        {"id":3812345678,
                         "kakao_account":{"profile":{"nickname":"씩씩이"},
                                          "email":"tester@kakao.com"}}
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client.fetchUserInfo("code", "state");

        assertThat(info.provider()).isEqualTo(Provider.KAKAO);
        assertThat(info.providerUserId()).isEqualTo("3812345678");
        assertThat(info.nickname()).isEqualTo("씩씩이");
    }

    /**
     * 이메일 선택 동의를 거부하면 응답에 email 키 자체가 없다. 흔한 경로이므로
     * 예외가 아니라 null 로 통과해야 한다.
     */
    @Test
    void 이메일_미동의여도_가입_경로가_깨지지_않는다() {
        stubToken();
        server.expect(requestTo(USER_INFO_URL))
                .andRespond(withSuccess("""
                        {"id":123,
                         "kakao_account":{"profile":{"nickname":"익명"},
                                          "email_needs_agreement":true}}
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client.fetchUserInfo("code", "state");

        assertThat(info.email()).isNull();
        assertThat(info.providerUserId()).isEqualTo("123");
    }

    @Test
    void 토큰_응답에_access_token_이_없으면_실패로_다룬다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"error":"invalid_grant","error_description":"KOE320"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("used-code", "state"))
                .isInstanceOf(OAuthException.class)
                .hasMessageContaining("KOE320");
    }

    @Test
    void 인가_URL_에_scope_와_state_가_들어간다() {
        String uri = client.authorizeUri("state-value").toString();

        assertThat(uri).startsWith("https://kauth.kakao.com/oauth/authorize");
        assertThat(uri).contains("client_id=rest-api-key");
        assertThat(uri).contains("state=state-value");
        assertThat(uri).contains("profile_nickname");
    }

    /**
     * 콘솔에 설정되지 않은 동의항목을 scope 로 요청하면 카카오가 인가 요청을 거부한다.
     * 이메일은 "권한 없음"(비즈앱 전환 필요) 상태이므로 요청하면 안 된다.
     */
    @Test
    void 권한이_없는_이메일_동의항목은_요청하지_않는다() {
        String uri = client.authorizeUri("state-value").toString();

        assertThat(uri).doesNotContain("account_email");
    }

    private void stubToken() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"token_type":"bearer","access_token":"AAAA","expires_in":21599}
                        """, MediaType.APPLICATION_JSON));
    }
}
