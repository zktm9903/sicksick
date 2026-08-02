package kr.sicksick.be.auth.oauth;

/** 소셜 제공자와의 통신이 실패했다. 사용자에게는 재시도 안내로 바꿔 보여준다. */
public class OAuthException extends RuntimeException {

    public OAuthException(String message) {
        super(message);
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
