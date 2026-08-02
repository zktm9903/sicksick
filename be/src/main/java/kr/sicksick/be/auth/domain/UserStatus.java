package kr.sicksick.be.auth.domain;

/**
 * 계정 상태.
 *
 * <p>소셜 인증에 성공하면 곧바로 {@link #PENDING} 유저를 만들고 우리 토큰을 발급한다.
 * 그래야 약관·본인인증·온보딩 도중 이탈해도 재로그인 시 그 자리에서 이어갈 수 있다.
 * 온보딩까지 마치면 {@link #ACTIVE} 가 된다.
 */
public enum UserStatus {

    /** 가입 절차를 아직 마치지 않았다. 온보딩 관련 API 외에는 접근을 막는다. */
    PENDING,

    /** 정상 이용 가능. */
    ACTIVE,

    /** 운영 정책에 의해 정지됨. */
    SUSPENDED
}
