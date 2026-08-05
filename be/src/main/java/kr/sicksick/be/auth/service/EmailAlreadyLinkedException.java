package kr.sicksick.be.auth.service;

import org.springframework.http.HttpStatus;

import kr.sicksick.be.config.ApiException;

/**
 * 이 이메일로 이미 가입된 계정이 있다.
 *
 * <p>자동 병합하지 않고 막는 이유: 소셜이 주는 이메일은 검증됐다는 보장이 없다.
 * 공격자가 피해자의 이메일로 소셜 계정을 만든 뒤 로그인하면 계정이 통째로 넘어간다.
 * 기존 수단으로 로그인한 뒤 직접 연동하도록 유도해야 한다.
 *
 * <p>소셜 흐름에서는 {@code OAuthController} 가 직접 잡아 {@code /login?error=email_taken}
 * 으로 리다이렉트한다(브라우저를 이동시키는 경로라 JSON 을 줄 수 없다). 자체 가입에서는
 * 잡지 않고 그대로 올려 보내면 {@code ApiExceptionHandler} 가 409 로 만들어 준다.
 */
public class EmailAlreadyLinkedException extends ApiException {

    /** 어느 방법으로 가입돼 있는지는 알려주지 않는다. 사용자가 할 일은 어느 쪽이든 같다. */
    private static final String MESSAGE = "이미 가입된 이메일이에요. 기존에 사용하던 방법으로 로그인해 주세요.";

    private final String email;

    public EmailAlreadyLinkedException(String email) {
        super(HttpStatus.CONFLICT, "email_taken", MESSAGE);
        this.email = email;
    }

    /** 로그용. 응답 본문에는 나가지 않는다. */
    public String getEmail() {
        return email;
    }
}
