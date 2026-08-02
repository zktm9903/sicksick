package kr.sicksick.be.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import kr.sicksick.be.auth.oauth.OAuthTxCookie;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.support.IntegrationTest;

/**
 * 소셜 로그인부터 본인인증까지 관통.
 *
 * <p>스텁 프로바이더를 쓰므로 카카오·네이버로 실제 요청이 나가지 않는다. state 검증,
 * 쿠키, 토큰 발급, 단계 계산 등 나머지 경로는 실제와 동일하게 지나간다.
 */
class SignupFlowIntegrationTest extends IntegrationTest {

    private static final Pattern STATE = Pattern.compile("state=([^&]+)");

    @Test
    void 소셜_로그인부터_본인인증까지_이어진다() throws Exception {
        Session session = login("kakao", "flow-tester");

        // 1) 신규 계정은 약관 화면에서 시작한다.
        assertThat(session.landing()).isEqualTo("/signup/terms");

        String accessToken = refresh(session);
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.phoneVerified").value(false))
                .andExpect(jsonPath("$.nextStep").value("/signup/terms"));

        // 2) 약관 동의 → 본인인증 단계로.
        mockMvc.perform(post("/api/v1/signup/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreements":{"SERVICE":true,"PRIVACY":true,"PHONE_AUTH":true,
                                               "MARKETING":false,"AGE_14":true}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/signup/phone"));

        // 3) 인증번호 발급 — 개발 환경이라 응답에 코드가 실려 온다.
        String code = mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"devCode\":\"([^\"]+)\".*", "$1");

        // 4) 틀린 코드는 거부된다.
        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());

        // 5) 올바른 코드 → 온보딩으로.
        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/onboarding/nickname"));
    }

    /**
     * 기존 계정 재로그인.
     *
     * <p>신규 가입은 방금 만든 엔티티를 그대로 쓰지만 재로그인은 DB 에서 읽은 프록시를
     * 다룬다. 지연 로딩이 트랜잭션 밖으로 새면 이 경로만 500 이 된다.
     */
    @Test
    void 중간에_이탈해도_재로그인하면_같은_지점에서_이어진다() throws Exception {
        Session first = login("naver", "resume-tester");
        assertThat(first.landing()).isEqualTo("/signup/terms");

        mockMvc.perform(post("/api/v1/signup/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreements":{"SERVICE":true,"PRIVACY":true,"PHONE_AUTH":true,
                                               "MARKETING":true,"AGE_14":true}}
                                """))
                .andExpect(status().isOk());

        // 브라우저를 닫았다가 다시 로그인한 상황.
        Session again = login("naver", "resume-tester");
        assertThat(again.landing()).isEqualTo("/signup/phone");
    }

    @Test
    void state_가_다르면_콜백을_거부한다() throws Exception {
        MvcResult authorize = mockMvc.perform(get("/api/v1/auth/oauth/kakao/authorize"))
                .andExpect(status().isFound())
                .andReturn();

        mockMvc.perform(get("/api/v1/auth/oauth/kakao/callback")
                        .param("code", "attacker")
                        .param("state", "forged-state")
                        .cookie(cookie(authorize.getResponse(), OAuthTxCookie.COOKIE_NAME)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=invalid_state"));
    }

    @Test
    void 트랜잭션_쿠키가_없으면_만료로_처리한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/kakao/callback")
                        .param("code", "anything")
                        .param("state", "anything"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=expired"));
    }

    @Test
    void 사용자가_동의를_취소하면_토큰_교환을_시도하지_않는다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/kakao/callback").param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=cancelled"));
    }

    @Test
    void 알_수_없는_프로바이더는_안내_화면으로_보낸다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/apple/authorize"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=provider_not_configured"));
    }

    @Test
    void 인증_없이_보호된_API_를_부르면_401_이다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    /**
     * 필터 단계에서 끊기는 응답에도 본문이 있어야 한다.
     *
     * <p>{@code @RestControllerAdvice} 는 컨트롤러에 도달한 요청만 다루므로, 인증 실패는
     * 별도 진입점이 없으면 <b>빈 본문</b>이 나간다. 그러면 프론트가 파싱할 게 없어
     * 사용자에게 띄울 안내를 만들지 못한다.
     */
    @Test
    void 인증_실패_응답에도_안내_문구가_담긴다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * 회귀 방지: 서버가 의도한 안내가 응답에 실제로 담기는지.
     *
     * <p>Spring 기본값({@code server.error.include-message=never})으로는 reason 이 잘려
     * 나가고 프론트에 {@code "Bad Request"} 만 남는다. 그게 그대로 화면에 노출됐었다.
     */
    @Test
    void 필수_약관_누락_시_어느_항목인지_알려준다() throws Exception {
        Session session = login("kakao", "terms-error-tester");
        String accessToken = refresh(session);

        mockMvc.perform(post("/api/v1/signup/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agreements\":{\"SERVICE\":true}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("PRIVACY")))
                .andExpect(jsonPath("$.message").value(not(containsString("Bad Request"))));
    }

    /**
     * SMS 발송을 붙이기 전까지 인증번호는 고정값이다.
     *
     * <p>고정값인 동안에는 본인인증이 아무것도 검증하지 못한다는 사실을 테스트로도
     * 드러내 둔다. 실제 발송을 붙일 때 이 테스트가 깨지면서 함께 정리하게 된다.
     */
    @Test
    void 인증번호는_고정값으로_발급된다() throws Exception {
        Session session = login("kakao", "fixed-code-tester");
        String accessToken = refresh(session);
        // 약관을 먼저 마쳐야 본인인증 다음 단계가 온보딩으로 넘어간다.
        agreeAllTerms(accessToken);

        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devCode").value("123456"));

        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01011112222\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/onboarding/nickname"));
    }

    /** 고정값이어도 틀린 코드는 여전히 막아야 한다(검증 로직 자체는 살아 있다). */
    @Test
    void 고정값이어도_다른_번호를_입력하면_실패한다() throws Exception {
        Session session = login("naver", "wrong-code-tester");
        String accessToken = refresh(session);

        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01033334444\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01033334444\",\"code\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증번호가 올바르지 않습니다."));
    }

    @Test
    void 잘못된_휴대폰_번호는_이유를_알려준다() throws Exception {
        Session session = login("naver", "phone-error-tester");
        String accessToken = refresh(session);

        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("올바른 휴대폰 번호가 아닙니다."));
    }

    /** 검증 기본 문구("공백일 수 없습니다")는 어느 항목인지 알려주지 않는다. */
    @Test
    void 검증_실패는_항목을_알아볼_수_있는_문구를_준다() throws Exception {
        Session session = login("kakao", "valid-error-tester");
        String accessToken = refresh(session);

        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("휴대폰 번호를 입력해 주세요."));
    }

    @Test
    void 깨진_JSON_본문은_형식_안내를_준다() throws Exception {
        Session session = login("naver", "json-error-tester");
        String accessToken = refresh(session);

        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않아요."));
    }

    @Test
    void 약관_목록은_인증_없이_볼_수_있다() throws Exception {
        mockMvc.perform(get("/api/v1/terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].code").value("SERVICE"));
    }

    @Test
    void 리프레시_쿠키가_없으면_재발급은_401_이다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    // ── 헬퍼 ──

    /** authorize → 스텁 콜백까지 진행하고 착지 경로와 리프레시 쿠키를 돌려준다. */
    private Session login(String provider, String identifier) throws Exception {
        MvcResult authorize = mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/authorize"))
                .andExpect(status().isFound())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        String state = extractState(authorize.getResponse().getHeader(HttpHeaders.LOCATION));

        MvcResult callback = mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/callback")
                        .param("code", identifier)
                        .param("state", state)
                        .cookie(cookie(authorize.getResponse(), OAuthTxCookie.COOKIE_NAME)))
                .andExpect(status().isFound())
                .andReturn();

        return new Session(
                callback.getResponse().getHeader(HttpHeaders.LOCATION),
                cookie(callback.getResponse(), AuthCookies.REFRESH_COOKIE));
    }

    /** 필수·선택 약관에 모두 동의시킨다. */
    private void agreeAllTerms(String accessToken) throws Exception {
        mockMvc.perform(post("/api/v1/signup/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreements":{"SERVICE":true,"PRIVACY":true,"PHONE_AUTH":true,
                                               "MARKETING":true,"AGE_14":true}}
                                """))
                .andExpect(status().isOk());
    }

    private String refresh(Session session) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private static String extractState(String location) {
        Matcher matcher = STATE.matcher(location);
        assertThat(matcher.find()).as("인가 URL 에 state 가 있어야 한다").isTrue();
        return matcher.group(1);
    }

    private static Cookie cookie(MockHttpServletResponse response, String name) {
        Cookie found = response.getCookie(name);
        assertThat(found).as(name + " 쿠키가 있어야 한다").isNotNull();
        return found;
    }

    private record Session(String landing, Cookie refreshCookie) {
    }
}
