package kr.sicksick.be.auth.oauth;

import kr.sicksick.be.auth.domain.Provider;

/**
 * 각 사 응답을 하나로 맞춘 형태.
 *
 * <p>카카오·네이버·구글은 응답 구조가 전부 다르다(중첩 위치, 필드명, id 타입까지).
 * 이 레코드로 정규화해 두면 가입·로그인 로직은 프로바이더가 몇 개든 한 벌로 끝난다.
 *
 * @param providerUserId 각 사의 고유 식별자. 카카오 id(Long), 네이버 response.id,
 *                       구글 sub — 전부 문자열로 통일한다.
 * @param email          동의하지 않았으면 null 이다. 식별키로 쓰지 않는다.
 * @param nickname       없을 수 있다.
 */
public record OAuthUserInfo(
        Provider provider,
        String providerUserId,
        String email,
        String nickname
) {

    public OAuthUserInfo {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException(provider + " 응답에 사용자 식별자가 없습니다");
        }
    }
}
