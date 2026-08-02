package kr.sicksick.be.auth.token;

/** 리프레시 토큰이 유효하지 않다. 클라이언트는 재로그인해야 한다. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
