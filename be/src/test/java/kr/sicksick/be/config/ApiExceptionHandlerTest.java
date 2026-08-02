package kr.sicksick.be.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 오류 응답 변환 규칙.
 *
 * <p>여기서 정하는 건 "무엇을 사용자에게 보여주고 무엇을 숨기는가"다. 4xx 는 우리가
 * 의도해서 쓴 안내이므로 그대로 내보내고, 5xx·예상 못한 예외는 내부 사정이 새어나가지
 * 않도록 고정 문구로 덮는다.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void 사용자용_4xx_문구는_그대로_내보낸다() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 합니다: PRIVACY"),
                request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("필수 약관에 동의해야 합니다: PRIVACY");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    /** 5xx 의 reason 에는 내부 사정이 담기기 쉽다. 사용자에게 전달하지 않는다. */
    @Test
    void 서버_오류는_원인을_감추고_기본_문구로_덮는다() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DB 커넥션 풀 고갈"),
                request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo(ApiErrorResponse.FALLBACK_MESSAGE)
                .doesNotContain("DB");
    }

    /** Spring 내부에서 나는 예외는 reason 이 비어 있을 수 있다. */
    @Test
    void reason_이_없으면_기본_문구를_쓴다() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST), request());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(ApiErrorResponse.FALLBACK_MESSAGE);
    }

    @Test
    void 본문을_읽지_못하면_형식_안내를_준다() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnreadableBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("요청 형식이 올바르지 않아요.");
    }

    @Test
    void 예상하지_못한_예외는_내부_메시지를_노출하지_않는다() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("NullPointer at UserRepository.line42"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo(ApiErrorResponse.FALLBACK_MESSAGE)
                .doesNotContain("UserRepository");
    }

    @Test
    void 빈_문구는_기본_문구로_대체된다() {
        assertThat(ApiErrorResponse.of("  ", 400).message())
                .isEqualTo(ApiErrorResponse.FALLBACK_MESSAGE);
    }

    private HttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/api/v1/signup/terms");
    }
}
