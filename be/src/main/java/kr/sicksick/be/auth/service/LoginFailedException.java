package kr.sicksick.be.auth.service;

import org.springframework.http.HttpStatus;

import kr.sicksick.be.config.ApiException;

/**
 * 이메일·비밀번호 로그인 실패.
 *
 * <p>실패 사유를 코드로 구분해 내려보낸다. 프로토타입(sicksick.html)의 로그인 화면이
 * 미가입 계정일 때 alert 대신 회원가입 유도 배너를 띄우기 때문이다.
 *
 * <p><b>이는 특정 이메일의 가입 여부가 외부에 드러난다는 뜻이다(계정 열거).</b> 그럼에도
 * 이렇게 두는 이유는, 가입 화면이 "이미 가입된 이메일이에요"를 이미 알려주고 있어
 * 여기서 감춰도 실질적으로 새로 감춰지는 정보가 없기 때문이다. 응답 시간으로 새는 것은
 * 별개 문제라 {@link LocalAccountService} 가 미가입 이메일에도 해시를 한 번 돌려 막는다.
 */
public class LoginFailedException extends ApiException {

    public enum Reason {

        /** 그 이메일로 가입된 계정이 없다. */
        ACCOUNT_NOT_FOUND("account_not_found", "가입된 계정이 없어요."),

        /** 계정은 있지만 비밀번호가 다르다. */
        BAD_PASSWORD("bad_password", "비밀번호가 올바르지 않아요."),

        /** 소셜로만 가입한 계정이라 비밀번호가 없다. */
        SOCIAL_ONLY("social_only", "소셜 계정으로 가입하셨어요. 간편 로그인을 이용해 주세요.");

        private final String code;
        private final String message;

        Reason(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public LoginFailedException(Reason reason) {
        super(HttpStatus.UNAUTHORIZED, reason.code, reason.message);
    }
}
