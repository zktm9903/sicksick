package kr.sicksick.be.config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * React 빌드 산출물(classpath:/static/)을 서빙한다.
 *
 * <p>경로 분간:
 * <pre>
 *   /api/**  → @RestController (아래 fallback 이 절대 가로채지 않는다)
 *   그 외     → 정적 파일, 없으면 index.html (SPA 클라이언트 라우팅)
 * </pre>
 */
@Configuration
class WebConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/static/";
    private static final String INDEX_HTML = "static/index.html";

    private final HandlerMethodArgumentResolver currentUserArgumentResolver;

    WebConfig(HandlerMethodArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /** {@code @CurrentUser User} 파라미터를 실제 엔티티로 바꿔주는 리졸버를 등록한다. */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite 가 파일명에 콘텐츠 해시를 붙이므로(index-C0p3TcVp.js) 장기 캐시가 안전하다.
        // 커스텀 리졸버를 붙이지 않으므로 없는 파일은 정상적으로 404 가 된다.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(STATIC_LOCATION + "assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).immutable());

        // index.html 은 캐시하지 않는다. 캐시되면 배포 후에도 브라우저가 옛 번들을 가리킨다.
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATION)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * 실제 파일이 없으면 index.html 로 대체해 클라이언트 라우팅을 살린다.
     */
    private static final class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // API 경로는 절대 index.html 로 대체하지 않는다.
            // 대체해 버리면 오타난 엔드포인트가 404 대신 200 + HTML 을 반환하고,
            // fe 의 apiGet 은 res.ok 로 판단하므로 에러를 놓친 채 HTML 을 JSON 으로
            // 파싱하려다 엉뚱한 곳에서 터진다.
            if (resourcePath.startsWith("api/")) {
                return null;
            }

            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            // 확장자가 있으면 파일 요청이다. 없는 이미지·폰트가 index.html 을 받지 않도록 404.
            if (resourcePath.contains(".")) {
                return null;
            }

            // /login, /signup/terms 같은 클라이언트 라우트.
            return new ClassPathResource(INDEX_HTML);
        }
    }
}
