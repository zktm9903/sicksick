package kr.sicksick.be.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 오류 응답을 사용자가 읽을 수 있는 형태로 통일한다.
 *
 * <p>이게 없으면 Spring 기본 오류 본문이 나가는데, 거기엔 우리가 쓴 안내 문구가 없다.
 * {@code server.error.include-message} 기본값이 {@code never} 라 잘려 나가기 때문이다.
 * 그러면 프론트에 남는 건 {@code "Bad Request"} 같은 프레임워크 문자열뿐이고, 그게
 * 그대로 사용자 화면에 노출된다.
 *
 * <p><b>{@code include-message=always} 로 여는 대신 여기서 예외별로 다루는 이유</b>:
 * 그 설정은 의도하지 않은 예외의 메시지까지 내보낸다. SQL 파편이나 클래스명이 사용자에게
 * 전달될 수 있다. 여기서는 우리가 직접 만든 4xx 의 문구만 통과시키고 나머지는 덮는다.
 *
 * <p>주의: 이 어드바이스는 <b>컨트롤러에 도달한 요청만</b> 다룬다. 인증 실패처럼 필터
 * 단계에서 끊기는 응답은 {@link SecurityConfig} 의 진입점이 따로 처리한다.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 우리가 직접 던진 오류. {@code reason} 에 사용자용 문구가 들어 있다.
     *
     * <p>Spring 내부에서 발생한 것은 reason 이 비어 있을 수 있고, 5xx 는 서버 사정이므로
     * 두 경우 모두 기본 문구로 덮는다.
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException e,
                                                          HttpServletRequest request) {
        HttpStatusCode status = e.getStatusCode();

        if (status.is5xxServerError()) {
            log.error("서버 오류 — {} {}", request.getMethod(), request.getRequestURI(), e);
            return build(ApiErrorResponse.FALLBACK_MESSAGE, status);
        }

        return build(e.getReason(), status);
    }

    /**
     * 화면이 종류에 따라 다르게 동작해야 하는 오류. 문구와 함께 분기용 코드를 실어 준다.
     *
     * <p>스택은 남기지 않는다. 잘못된 비밀번호처럼 <b>정상적으로 자주 일어나는</b> 흐름이라
     * 스택을 찍으면 로그가 의미 없이 불어난다.
     */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiErrorResponse.of(e.getMessage(), e.getStatus().value(), e.getCode()));
    }

    /** {@code @Valid} 검증 실패. 화면에 한 줄로 뜨므로 첫 번째 위반만 보여준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::describe)
                .findFirst()
                .orElse("입력값을 확인해 주세요.");

        return build(message, HttpStatus.BAD_REQUEST);
    }

    /** 본문이 JSON 으로 읽히지 않는 경우. 파싱 오류 원문은 노출하지 않는다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
        return build("요청 형식이 올바르지 않아요.", HttpStatus.BAD_REQUEST);
    }

    /**
     * 예상하지 못한 오류.
     *
     * <p>원인은 로그에만 남기고 사용자에게는 고정 문구를 준다. 예외 메시지가 그대로
     * 화면에 뜨면 내부 구조가 노출된다.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외 — {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(ApiErrorResponse.FALLBACK_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String describe(FieldError error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank()
                ? error.getField() + " 값을 확인해 주세요."
                : message;
    }

    private static ResponseEntity<ApiErrorResponse> build(String message, HttpStatusCode status) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(message, status.value()));
    }
}
