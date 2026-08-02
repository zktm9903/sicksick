package kr.sicksick.be.auth.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * 소셜 로그인 제공자.
 *
 * <p>구글은 임베디드 웹뷰에서의 OAuth 를 정책적으로 차단하므로(disallowed_useragent),
 * 시스템 브라우저 + 딥링크 브릿지가 준비된 뒤에 추가한다.
 */
public enum Provider {

    KAKAO,
    NAVER;

    /** URL 경로(소문자)에서 변환한다. 알 수 없는 값이면 비어 있는 Optional. */
    public static Optional<Provider> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String pathName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
