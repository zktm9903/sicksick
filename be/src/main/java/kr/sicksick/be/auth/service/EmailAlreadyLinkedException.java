package kr.sicksick.be.auth.service;

/**
 * 다른 소셜 계정이 이미 이 이메일로 가입돼 있다.
 *
 * <p>자동 병합하지 않고 막는 이유: 소셜이 주는 이메일은 검증됐다는 보장이 없다.
 * 공격자가 피해자의 이메일로 소셜 계정을 만든 뒤 로그인하면 계정이 통째로 넘어간다.
 * 기존 수단으로 로그인한 뒤 직접 연동하도록 유도해야 한다.
 */
public class EmailAlreadyLinkedException extends RuntimeException {

    public EmailAlreadyLinkedException(String email) {
        super("이미 가입된 이메일입니다: " + email);
    }
}
