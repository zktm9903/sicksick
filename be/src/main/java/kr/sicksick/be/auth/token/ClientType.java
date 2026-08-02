package kr.sicksick.be.auth.token;

/**
 * 요청을 보낸 클라이언트 종류. 리프레시 토큰 수명을 가른다.
 *
 * <p>앱은 "한 번 로그인하면 계속 유지"가 기대치라 훨씬 길게 잡는다.
 */
public enum ClientType {

    WEB,
    APP;

    /** 웹뷰 앱이 UserAgent 에 붙이는 식별자. 네이티브 래퍼에서 설정한다. */
    private static final String APP_USER_AGENT_MARKER = "SicksickApp";

    public static ClientType fromUserAgent(String userAgent) {
        return userAgent != null && userAgent.contains(APP_USER_AGENT_MARKER) ? APP : WEB;
    }
}
