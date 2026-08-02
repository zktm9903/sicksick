package kr.sicksick.be.auth.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 유저를 컨트롤러 파라미터로 받는다.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * MeResponse me(@CurrentUser User user) { ... }
 * }</pre>
 *
 * <p>토큰은 유효한데 계정이 사라진 경우(탈퇴 등)에는 401 로 끊는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
