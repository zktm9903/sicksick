package kr.sicksick.be.config;

import org.springframework.http.HttpStatus;

/**
 * 프론트가 <b>분기해야 하는</b> 오류.
 *
 * <p>대부분의 오류는 안내 문구만 띄우면 되고, 그건 {@code ResponseStatusException} 의
 * reason 으로 충분하다. 하지만 화면이 오류 종류에 따라 다른 UI 를 그려야 하는 경우가 있다
 * — 예를 들어 로그인 실패가 "가입된 계정이 없음"이면 alert 대신 회원가입 유도 배너를
 * 띄운다. 문구를 문자열 비교하는 것은 문구를 고치는 순간 깨지므로, 기계가 읽을 코드를
 * 따로 내려준다.
 *
 * <p>{@link ApiExceptionHandler} 가 상태·문구·코드를 그대로 응답에 실어 준다. 반대로
 * 말하면 <b>여기 담기는 메시지는 사용자에게 그대로 보인다.</b>
 *
 * <p>인터페이스가 아니라 클래스인 이유: {@code @ExceptionHandler} 는
 * {@code Class<? extends Throwable>} 만 받으므로 Throwable 을 상속해야 잡을 수 있다.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * @param userMessage 화면에 그대로 띄워도 되는 한국어 안내. 예외 메시지로도 쓰인다
     * @param code        프론트 분기용 식별자. {@code fe/src/features/auth/api.ts} 와 짝을 맞춘다
     */
    protected ApiException(HttpStatus status, String code, String userMessage) {
        super(userMessage);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
