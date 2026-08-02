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
 * 네이버 로그인.
 *
 * <p><b>네이버는 실패해도 HTTP 200 을 돌려준다.</b> 본문의 {@code error} 필드와
 * 사용자 조회의 {@code resultcode} 를 직접 봐야 한다. 상태 코드만 보고 넘어가면
 * null 토큰을 들고 다음 단계로 진입해 엉뚱한 곳에서 터진다.
 *
 * <p>scope 파라미터가 없다. 제공 항목은 개발자센터에서 설정한 값이 그대로 적용된다.
 * PKCE·nonce 도 지원하지 않는다.
 */
class NaverOAuthClient implements OAuthClient {

    private static final String AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private static final String RESULT_CODE_SUCCESS = "00";

    private final RestClient restClient;
    private final OAuthProperties.Credentials credentials;
    private final String redirectUri;

    NaverOAuthClient(RestClient restClient, OAuthProperties.Credentials credentials, String redirectUri) {
        this.restClient = restClient;
        this.credentials = credentials;
        this.redirectUri = redirectUri;
    }

    @Override
    public Provider provider() {
        return Provider.NAVER;
    }

    @Override
    public URI authorizeUri(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", credentials.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String code, String state) {
        return readUserInfo(exchangeCodeForAccessToken(code, state));
    }

    private String exchangeCodeForAccessToken(String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", credentials.clientId());
        form.add("client_secret", credentials.clientSecret());
        form.add("code", code);
        form.add("state", state);

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthResponses.MAP_TYPE);
        } catch (RestClientException e) {
            throw new OAuthException("네이버 토큰 교환에 실패했습니다", e);
        }

        // HTTP 200 이어도 여기에 error 가 들어 있을 수 있다.
        String error = OAuthResponses.stringOrNull(response, "error");
        if (error != null) {
            throw new OAuthException("네이버 토큰 교환 오류: " + OAuthResponses.errorSummary(response));
        }

        String accessToken = OAuthResponses.stringOrNull(response, "access_token");
        if (accessToken == null) {
            throw new OAuthException("네이버 토큰 응답에 access_token 이 없습니다");
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
            throw new OAuthException("네이버 사용자 정보 조회에 실패했습니다", e);
        }

        String resultCode = OAuthResponses.stringOrNull(response, "resultcode");
        if (!RESULT_CODE_SUCCESS.equals(resultCode)) {
            throw new OAuthException("네이버 사용자 정보 조회 실패 (resultcode=" + resultCode + "): "
                    + OAuthResponses.stringOrNull(response, "message"));
        }

        Map<String, Object> profile = OAuthResponses.nestedOrEmpty(response, "response");
        String id = OAuthResponses.stringOrNull(profile, "id");
        if (id == null) {
            throw new OAuthException("네이버 사용자 정보에 id 가 없습니다");
        }

        // 닉네임 미동의 시 name 으로 대체한다(둘 다 없으면 null).
        String nickname = OAuthResponses.stringOrNull(profile, "nickname");
        if (nickname == null) {
            nickname = OAuthResponses.stringOrNull(profile, "name");
        }

        return new OAuthUserInfo(
                Provider.NAVER,
                id,
                OAuthResponses.stringOrNull(profile, "email"),
                nickname);
    }
}
