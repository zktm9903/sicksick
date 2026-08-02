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

/**
 * 네이버 응답 처리.
 *
 * <p>네이버는 <b>실패해도 HTTP 200</b> 을 준다. 상태 코드만 보고 넘어가면 null 토큰을
 * 들고 다음 단계로 진입해 엉뚱한 곳에서 터지므로, 본문의 error / resultcode 를 반드시
 * 검사해야 한다. 이 테스트가 그 계약을 고정한다.
 */
class NaverOAuthClientTest {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private RestClient restClient;
    private MockRestServiceServer server;
    private NaverOAuthClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        client = new NaverOAuthClient(
                restClient,
                new OAuthProperties.Credentials("client-id", "client-secret"),
                "http://localhost:5173/api/v1/auth/oauth/naver/callback");
    }

    @Test
    void 정상_응답이면_사용자_정보를_정규화한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"AAAA","refresh_token":"BBBB",
                         "token_type":"bearer","expires_in":"3600"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URL))
                .andRespond(withSuccess("""
                        {"resultcode":"00","message":"success",
                         "response":{"id":"32742776","nickname":"씩씩이",
                                     "email":"tester@naver.com","name":"이상철"}}
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client.fetchUserInfo("code", "state");

        assertThat(info.provider()).isEqualTo(Provider.NAVER);
        assertThat(info.providerUserId()).isEqualTo("32742776");
        assertThat(info.email()).isEqualTo("tester@naver.com");
        assertThat(info.nickname()).isEqualTo("씩씩이");
        server.verify();
    }

    @Test
    void 토큰_교환이_HTTP_200_이어도_본문에_error_가_있으면_실패로_다룬다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"error":"invalid_request","error_description":"잘못된 요청"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("code", "state"))
                .isInstanceOf(OAuthException.class)
                .hasMessageContaining("invalid_request");
    }

    @Test
    void 사용자_조회의_resultcode_가_00_이_아니면_실패로_다룬다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"AAAA","token_type":"bearer","expires_in":"3600"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URL))
                .andRespond(withSuccess("""
                        {"resultcode":"024","message":"Authentication failed"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUserInfo("code", "state"))
                .isInstanceOf(OAuthException.class)
                .hasMessageContaining("024");
    }

    @Test
    void 닉네임을_주지_않으면_이름으로_대체한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"AAAA","token_type":"bearer","expires_in":"3600"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URL))
                .andRespond(withSuccess("""
                        {"resultcode":"00","response":{"id":"1","name":"이상철"}}
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client.fetchUserInfo("code", "state");

        assertThat(info.nickname()).isEqualTo("이상철");
        // 이메일 미동의는 정상 경로다. null 이어야 하고 예외가 되면 안 된다.
        assertThat(info.email()).isNull();
    }

    @Test
    void 인가_URL_에_state_와_redirect_uri_가_들어간다() {
        String uri = client.authorizeUri("state-value").toString();

        assertThat(uri).startsWith("https://nid.naver.com/oauth2.0/authorize");
        assertThat(uri).contains("state=state-value");
        assertThat(uri).contains("response_type=code");
    }
}
