package kr.sicksick.be.auth.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.repository.UserRepository;

/**
 * {@link CurrentUser} 파라미터를 실제 엔티티로 바꾼다.
 *
 * <p>JWT 의 subject(=userId)로 매 요청 유저를 조회한다. 토큰에 담긴 status 는 발급
 * 시점의 값이라 최신이 아닐 수 있으므로, 상태에 따라 갈리는 판단은 여기서 읽은
 * 엔티티를 기준으로 한다.
 */
@Component
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository users;

    CurrentUserArgumentResolver(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요.");
        }

        long userId;
        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않아요. 다시 로그인해 주세요.");
        }

        return users.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정을 찾을 수 없어요. 다시 로그인해 주세요."));
    }
}
