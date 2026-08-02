package kr.sicksick.be.config;

/**
 * API 오류 응답 본문.
 *
 * <p>프론트는 {@code message} 만 읽어 사용자에게 그대로 보여준다. 따라서 여기 담기는
 * 문구는 <b>사용자가 읽을 수 있는 한국어</b>여야 하고, 내부 사정(SQL·클래스명·스택)이
 * 섞이면 안 된다.
 *
 * @param message 화면에 그대로 띄워도 되는 안내 문구
 * @param status  HTTP 상태 코드. 디버깅 편의를 위해 본문에도 넣는다
 */
public record ApiErrorResponse(String message, int status) {

    /** 원인을 노출하면 안 되는 경우에 쓰는 기본 문구. */
    public static final String FALLBACK_MESSAGE = "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.";

    public static ApiErrorResponse of(String message, int status) {
        return new ApiErrorResponse(
                message == null || message.isBlank() ? FALLBACK_MESSAGE : message,
                status);
    }
}
