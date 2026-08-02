package kr.sicksick.be.auth.oauth;

import java.net.URI;

import kr.sicksick.be.auth.domain.Provider;

/**
 * 소셜 로그인 제공자 한 곳과의 통신.
 *
 * <p>구현체는 인가 URL 조립과 (토큰 교환 + 유저 조회)만 책임진다. state 생성·검증,
 * 쿠키, 우리 토큰 발급은 호출하는 쪽의 일이다.
 */
public interface OAuthClient {

    Provider provider();

    /** 사용자를 보낼 인가 화면 주소. */
    URI authorizeUri(String state);

    /**
     * 인가 코드를 유저 정보로 바꾼다. 내부적으로 토큰 교환과 유저 조회를 함께 수행한다.
     *
     * @throws OAuthException 교환·조회 중 실패
     */
    OAuthUserInfo fetchUserInfo(String code, String state);
}
