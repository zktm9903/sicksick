package kr.sicksick.be.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증 필터 구성.
 *
 * <p>필터를 직접 짜지 않고 Spring Security 를 쓰는 이유는 <b>deny-by-default</b> 때문이다.
 * 손으로 짠 필터는 새 엔드포인트를 화이트리스트에 빠뜨리면 그대로 무인증으로 열리는데,
 * 증상·질환 같은 건강 데이터를 다루는 서비스에서 그 실패 방식은 감당하기 어렵다.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private final AuthProperties authProperties;
    private final ApiErrorWriter apiErrorWriter;

    SecurityConfig(AuthProperties authProperties, ApiErrorWriter apiErrorWriter) {
        this.authProperties = authProperties;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 우리가 직접 발급한 HS256 토큰을 검증한다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                        // 리소스 서버는 자체 진입점을 갖고 있어서, 아래 exceptionHandling 만
                        // 설정하면 Bearer 토큰이 붙은 요청의 실패 응답이 여기서 먼저 처리된다.
                        .authenticationEntryPoint(apiErrorWriter))

                // 필터 단계에서 끊기는 401/403 에도 본문을 실어 준다.
                // 이게 없으면 프론트가 파싱할 게 없어 안내 문구를 만들지 못한다.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(apiErrorWriter)
                        .accessDeniedHandler(apiErrorWriter))

                // 서버 세션을 만들지 않는다. 상태는 토큰과 리프레시 테이블에만 있다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF 를 끄는 근거:
                //   1) 상태를 바꾸는 API 는 Authorization 헤더를 요구한다. 헤더는 크로스사이트
                //      요청에 자동으로 실리지 않으므로 CSRF 가 성립하지 않는다.
                //   2) 쿠키만으로 동작하는 POST /auth/refresh 는 리프레시 쿠키가
                //      SameSite=Lax 라 크로스사이트 POST 에 아예 실리지 않는다.
                //   3) 응답은 CORS 로 막혀 공격자가 읽을 수 없다.
                .csrf(csrf -> csrf.disable())

                // 브라우저 기본 인증창·폼 로그인은 쓰지 않는다.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                .authorizeHttpRequests(auth -> auth
                        // 인증 이전에 호출되는 것들.
                        .requestMatchers("/api/v1/auth/oauth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                        // 약관 본문은 가입 전에도 봐야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/terms").permitAll()
                        .requestMatchers("/api/v1/test/**").permitAll()

                        // 나머지 API 는 전부 인증 필요.
                        .requestMatchers("/api/**").authenticated()

                        // SPA 정적 파일과 클라이언트 라우트. WebConfig 가 index.html 로 넘긴다.
                        .anyRequest().permitAll());

        return http.build();
    }

    /**
     * 자체 발급 토큰이라 JWKS 를 가져올 곳이 없다. 공유 비밀키로 직접 검증한다.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(authProperties.jwtSecretBytes(), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
