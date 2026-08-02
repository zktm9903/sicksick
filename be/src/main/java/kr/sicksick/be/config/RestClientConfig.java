package kr.sicksick.be.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 소셜 제공자 호출용 RestClient.
 *
 * <p>타임아웃을 반드시 건다. 지정하지 않으면 카카오·네이버가 응답하지 않을 때 톰캣
 * 워커 스레드가 그대로 물려서, 소셜 로그인 장애가 서비스 전체 마비로 번진다.
 *
 * <p>Boot 의 RestClient 자동설정 모듈(spring-boot-restclient)은 클래스패스에 없으므로
 * 빌더 빈에 기대지 않고 직접 조립한다.
 */
@Configuration(proxyBeanMethods = false)
class RestClientConfig {

    /** 연결 수립까지 허용하는 시간. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /** 응답 본문을 받기까지 허용하는 시간. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestClient oauthRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                // 리다이렉트를 따라가지 않는다. 토큰·유저정보 엔드포인트는 직접 응답해야 한다.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
