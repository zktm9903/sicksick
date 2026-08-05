package kr.sicksick.be.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.oauth.OAuthTxCookie;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.support.IntegrationTest;

/**
 * 이메일·비밀번호 가입과 로그인.
 *
 * <p>소셜 가입({@link SignupFlowIntegrationTest})과 같은 자리에 도착해야 한다 — 가입
 * 직후 약관 화면, 이후 단계는 {@code NextStepResolver} 가 DB 상태만 보고 계산한다.
 */
class LocalAuthFlowIntegrationTest extends IntegrationTest {

    private static final Pattern STATE = Pattern.compile("state=([^&]+)");

    private static final String PASSWORD = "sicksick123";

    @Autowired
    private UserRepository users;

    @Test
    void 가입부터_본인인증까지_소셜과_같은_경로로_이어진다() throws Exception {
        Session session = signUp("flow@sicksick.test", PASSWORD);

        // 1) 신규 계정은 소셜과 똑같이 약관 화면에서 시작한다.
        assertThat(session.nextStep()).isEqualTo("/signup/terms");

        String accessToken = refresh(session);
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@sicksick.test"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.phoneVerified").value(false))
                .andExpect(jsonPath("$.nextStep").value("/signup/terms"));

        // 2) 약관 동의 → 본인인증.
        agreeAllTerms(accessToken);

        // 3) 본인인증 → 온보딩.
        mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01055556666\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01055556666\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/onboarding/nickname"));
    }

    /**
     * 비밀번호가 원문으로 남지 않는지.
     *
     * <p>이 테스트가 깨지는 상황은 곧 DB 유출이 계정 유출이 되는 상황이다.
     */
    @Test
    void 비밀번호는_해시로_저장된다() throws Exception {
        signUp("hash@sicksick.test", PASSWORD);

        User user = users.findByEmailAndDeletedAtIsNull("hash@sicksick.test").orElseThrow();

        assertThat(user.getPasswordHash())
                .as("알고리즘을 나중에 바꿀 수 있도록 접두사가 붙어야 한다")
                .startsWith("{bcrypt}")
                .doesNotContain(PASSWORD);
    }

    @Test
    void 이미_가입된_이메일이면_막는다() throws Exception {
        signUp("dup@sicksick.test", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("dup@sicksick.test", "another-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));
    }

    /**
     * 대소문자만 다른 이메일도 같은 계정이다.
     *
     * <p>MySQL 콜레이션이 대소문자를 구분하지 않아 지금은 DB 가 막아 주지만, 거기 기대면
     * 콜레이션이 바뀌는 순간 같은 이메일로 계정이 둘 생긴다. 애플리케이션 정규화를 검증한다.
     */
    @Test
    void 대소문자만_다른_이메일은_같은_계정으로_본다() throws Exception {
        signUp("case@sicksick.test", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("CASE@Sicksick.TEST", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));

        // 로그인도 대소문자에 관계없이 통해야 한다.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("  Case@SICKSICK.test  ", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void 중간에_이탈해도_재로그인하면_같은_지점에서_이어진다() throws Exception {
        Session first = signUp("resume@sicksick.test", PASSWORD);
        assertThat(first.nextStep()).isEqualTo("/signup/terms");

        agreeAllTerms(refresh(first));

        // 브라우저를 닫았다가 다시 로그인한 상황.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("resume@sicksick.test", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/signup/phone"));
    }

    /**
     * 실패 사유가 코드로 구분돼야 한다.
     *
     * <p>로그인 화면이 미가입일 때만 회원가입 유도 배너를 띄운다. 문구를 문자열 비교하면
     * 문구를 고치는 순간 조용히 깨지므로 코드로 내려보낸다.
     */
    @Test
    void 가입되지_않은_이메일은_회원가입을_안내한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("nobody@sicksick.test", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("account_not_found"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 비밀번호가_틀리면_거부한다() throws Exception {
        signUp("wrongpw@sicksick.test", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("wrongpw@sicksick.test", "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("bad_password"));
    }

    /** 소셜로만 가입한 계정에는 비밀번호가 없다. 무엇을 해야 하는지 알려줘야 한다. */
    @Test
    void 소셜_전용_계정은_간편_로그인을_안내한다() throws Exception {
        socialLogin("kakao", "social-only-tester");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("social-only-tester@kakao.stub.local", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("social_only"));
    }

    /** 소셜 가입이 자체 가입 이메일을 덮어쓰지 않는지. 반대 방향도 막혀야 한다. */
    @Test
    void 자체_가입한_이메일로는_소셜_가입이_막힌다() throws Exception {
        signUp("taken@naver.stub.local", PASSWORD);

        MvcResult callback = socialCallback("naver", "taken");

        assertThat(callback.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("/login?error=email_taken");
    }

    @Test
    void 짧은_비밀번호는_기준을_알려준다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("short@sicksick.test", "1234567")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 입력해 주세요."));
    }

    @Test
    void 이메일_형식이_아니면_알려준다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않아요."));
    }

    /**
     * BCrypt 는 72바이트를 넘기면 예외를 던진다 — 막지 않으면 500 이 된다.
     *
     * <p>한글은 UTF-8 로 3바이트라 25자면 이미 75바이트다. 문자 수만 세는 {@code @Size}
     * 로는 잡히지 않는 경계다.
     */
    @Test
    void 너무_긴_비밀번호는_500_이_아니라_안내를_준다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("long@sicksick.test", "씩".repeat(25))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("너무 길어요")));
    }

    /** 가입·로그인은 토큰이 없는 상태에서 부른다. 필터가 막으면 아무도 가입할 수 없다. */
    @Test
    void 가입과_로그인은_인증_없이_호출할_수_있다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("anyone@sicksick.test", PASSWORD)))
                .andExpect(status().isUnauthorized())
                // 필터가 막았다면 본문에 우리 코드가 없다.
                .andExpect(jsonPath("$.code").value("account_not_found"));
    }

    // ── 헬퍼 ──

    /** 가입하고 착지 경로와 리프레시 쿠키를 돌려준다. */
    private Session signUp(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isCreated())
                .andReturn();

        return new Session(
                result.getResponse().getContentAsString()
                        .replaceAll(".*\"nextStep\":\"([^\"]+)\".*", "$1"),
                cookie(result.getResponse(), AuthCookies.REFRESH_COOKIE));
    }

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

    private void socialLogin(String provider, String identifier) throws Exception {
        socialCallback(provider, identifier);
    }

    /** 스텁 프로바이더로 authorize → callback 까지 진행한다. */
    private MvcResult socialCallback(String provider, String identifier) throws Exception {
        MvcResult authorize = mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/authorize"))
                .andExpect(status().isFound())
                .andReturn();

        Matcher matcher = STATE.matcher(authorize.getResponse().getHeader(HttpHeaders.LOCATION));
        assertThat(matcher.find()).as("인가 URL 에 state 가 있어야 한다").isTrue();

        return mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/callback")
                        .param("code", identifier)
                        .param("state", matcher.group(1))
                        .cookie(cookie(authorize.getResponse(), OAuthTxCookie.COOKIE_NAME)))
                .andExpect(status().isFound())
                .andReturn();
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static Cookie cookie(MockHttpServletResponse response, String name) {
        Cookie found = response.getCookie(name);
        assertThat(found).as(name + " 쿠키가 있어야 한다").isNotNull();
        return found;
    }

    private record Session(String nextStep, Cookie refreshCookie) {
    }
}
