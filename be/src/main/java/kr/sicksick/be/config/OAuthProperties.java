package kr.sicksick.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import kr.sicksick.be.auth.domain.Provider;

/**
 * 소셜 로그인 설정. {@code sicksick.oauth.*}
 *
 * @param baseUrl     브라우저가 보는 우리 서비스 주소. redirect_uri 를 이 값으로 조립하므로
 *                    각 사 콘솔에 등록한 값과 문자 단위로 같아야 한다.
 *                    개발은 Vite 개발 서버(5173), 운영은 실제 도메인.
 * @param stubEnabled 앱 키 없이 로그인 전 구간을 돌려보기 위한 개발용 스텁.
 * @param kakao       카카오 자격증명. client-id 는 REST API 키다(JavaScript 키가 아니다).
 * @param naver       네이버 자격증명.
 */
@ConfigurationProperties(prefix = "sicksick.oauth")
public record OAuthProperties(
        String baseUrl,
        boolean stubEnabled,
        Credentials kakao,
        Credentials naver
) {

    public record Credentials(String clientId, String clientSecret) {

        /** 키가 비어 있으면 해당 프로바이더를 등록하지 않는다(누르면 안내 화면으로 보낸다). */
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank();
        }
    }

    public Credentials credentialsFor(Provider provider) {
        return switch (provider) {
            case KAKAO -> kakao;
            case NAVER -> naver;
        };
    }

    /** 각 사 콘솔에 등록해야 하는 콜백 주소. */
    public String redirectUri(Provider provider) {
        return baseUrl + "/api/v1/auth/oauth/" + provider.pathName() + "/callback";
    }
}
