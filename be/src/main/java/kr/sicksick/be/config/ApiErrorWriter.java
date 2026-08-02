package kr.sicksick.be.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증·인가 실패 응답을 API 오류 형태로 직접 쓴다.
 *
 * <p>{@link ApiExceptionHandler} 는 컨트롤러에 도달한 요청만 다룬다. Security 필터에서
 * 끊기는 401/403 은 거기까지 가지 못하므로 <b>본문이 비어 있는 응답</b>이 나간다.
 * 프론트가 파싱할 게 없어 화면에 띄울 문구를 못 만든다.
 *
 * <p>401 문구에 "존재하지 않는 경로"를 함께 적은 이유: {@code /api/**} 가 전부 인증
 * 대상이라 오타난 주소도 404 가 아니라 401 로 떨어진다. 어떤 엔드포인트가 있는지는
 * 여전히 알려주지 않으면서, 개발 중 원인을 좁힐 단서만 준다.
 */
@Component
class ApiErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String UNAUTHORIZED_MESSAGE =
            "로그인이 필요하거나 존재하지 않는 경로예요.";

    private static final String FORBIDDEN_MESSAGE =
            "이 기능을 사용할 권한이 없어요.";

    private final ObjectMapper objectMapper;

    ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 인증되지 않은 요청. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    /** 인증은 됐지만 권한이 없는 요청. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(message, status.value()));
    }
}
