package kr.sicksick.be.auth.oauth;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.config.OAuthProperties;

/**
 * 프로바이더별 클라이언트를 모아둔 곳.
 *
 * <p>스텁이 켜져 있으면 모든 프로바이더를 스텁으로 대체한다. 그렇지 않으면 키가 설정된
 * 프로바이더만 등록하고, 나머지는 조회 시 비어 있는 Optional 을 돌려준다(호출부가
 * 안내 화면으로 돌려보낸다). 키 없이 실제 요청을 보내 카카오에서 오류를 받는 것보다
 * 원인이 명확하다.
 */
@Component
public class OAuthClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(OAuthClientRegistry.class);

    private final Map<Provider, OAuthClient> clients;

    OAuthClientRegistry(RestClient oauthRestClient, OAuthProperties properties) {
        this.clients = build(oauthRestClient, properties);
    }

    private static Map<Provider, OAuthClient> build(RestClient restClient, OAuthProperties properties) {
        Map<Provider, OAuthClient> result = new EnumMap<>(Provider.class);

        if (properties.stubEnabled()) {
            for (Provider provider : Provider.values()) {
                result.put(provider, new StubOAuthClient(provider, properties.baseUrl()));
            }
            log.warn("OAuth 스텁이 켜져 있습니다. 실제 소셜 인증 없이 로그인됩니다 (개발 전용).");
            return result;
        }

        for (Provider provider : Provider.values()) {
            OAuthProperties.Credentials credentials = properties.credentialsFor(provider);
            if (credentials == null || !credentials.isConfigured()) {
                log.warn("{} 자격증명이 없어 등록하지 않습니다.", provider);
                continue;
            }
            String redirectUri = properties.redirectUri(provider);
            result.put(provider, switch (provider) {
                case KAKAO -> new KakaoOAuthClient(restClient, credentials, redirectUri);
                case NAVER -> new NaverOAuthClient(restClient, credentials, redirectUri);
            });
            log.info("{} 로그인 등록 완료 (redirect_uri={})", provider, redirectUri);
        }
        return result;
    }

    public Optional<OAuthClient> find(Provider provider) {
        return Optional.ofNullable(clients.get(provider));
    }
}
