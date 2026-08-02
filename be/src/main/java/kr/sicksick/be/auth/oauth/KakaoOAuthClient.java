package kr.sicksick.be.auth.oauth;

import java.net.URI;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.config.OAuthProperties;

/**
 * 카카오 로그인.
 *
 * <p>주의할 점:
 * <ul>
 *   <li>client_id 는 <b>REST API 키</b>다. JavaScript 키를 넣으면 KOE101 이 난다.</li>
 *   <li>{@code redirect_uri} 는 인가 요청과 토큰 교환에서 문자 단위로 같아야 한다(KOE006).</li>
 *   <li>이메일은 선택 동의라 미동의 시 응답에 키 자체가 없다. null 을 정상으로 다룬다.</li>
 * </ul>
 */
class KakaoOAuthClient implements OAuthClient {

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    /**
     * 요청할 동의항목. 쉼표 구분이다(네이버·구글과 구분자가 다르다).
     *
     * <p><b>이메일({@code account_email})은 뺐다.</b> 개발자 콘솔에서 "권한 없음" 상태이며
     * 비즈니스 앱 전환이나 검수를 거쳐야 열린다. 콘솔에 설정되지 않은 항목을 scope 로
     * 요청하면 카카오가 인가 요청 자체를 거부한다.
     *
     * <p>따라서 카카오로 가입한 유저는 이메일이 항상 null 이다. 이미 그 경로를 정상으로
     * 다루고 있으므로(users.email nullable) 문제는 없고, 필요하면 온보딩에서 직접 받는다.
     * 비즈앱 전환 후에는 여기에 {@code ,account_email} 을 다시 붙이면 된다.
     */
    private static final String SCOPE = "profile_nickname";

    private final RestClient restClient;
    private final OAuthProperties.Credentials credentials;
    private final String redirectUri;

    KakaoOAuthClient(RestClient restClient, OAuthProperties.Credentials credentials, String redirectUri) {
        this.restClient = restClient;
        this.credentials = credentials;
        this.redirectUri = redirectUri;
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public URI authorizeUri(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", credentials.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String state) {
        return readUserInfo(exchangeCodeForAccessToken(code));
    }

    private String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", credentials.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (credentials.clientSecret() != null && !credentials.clientSecret().isBlank()) {
            // 콘솔에서 'client_secret 사용'을 켰다면 필수다. 껐다면 보내도 무시된다.
            form.add("client_secret", credentials.clientSecret());
        }

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthResponses.MAP_TYPE);
        } catch (RestClientException e) {
            throw new OAuthException("카카오 토큰 교환에 실패했습니다", e);
        }

        String accessToken = OAuthResponses.stringOrNull(response, "access_token");
        if (accessToken == null) {
            throw new OAuthException("카카오 토큰 응답에 access_token 이 없습니다: "
                    + OAuthResponses.errorSummary(response));
        }
        return accessToken;
    }

    private OAuthUserInfo readUserInfo(String accessToken) {
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(OAuthResponses.MAP_TYPE);
        } catch (RestClientException e) {
            throw new OAuthException("카카오 사용자 정보 조회에 실패했습니다", e);
        }

        if (response == null) {
            throw new OAuthException("카카오 사용자 정보 응답이 비어 있습니다");
        }

        // id 는 숫자로 오지만 우리는 문자열로 보관한다.
        Object id = response.get("id");
        if (id == null) {
            throw new OAuthException("카카오 사용자 정보에 id 가 없습니다");
        }

        Map<String, Object> account = OAuthResponses.nestedOrEmpty(response, "kakao_account");
        Map<String, Object> profile = OAuthResponses.nestedOrEmpty(account, "profile");

        return new OAuthUserInfo(
                Provider.KAKAO,
                String.valueOf(id),
                // 미동의면 키 자체가 없다.
                OAuthResponses.stringOrNull(account, "email"),
                OAuthResponses.stringOrNull(profile, "nickname"));
    }
}
