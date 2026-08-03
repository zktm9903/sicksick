package kr.sicksick.be.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import kr.sicksick.be.auth.domain.UserStatus;
import kr.sicksick.be.auth.oauth.OAuthTxCookie;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.auth.token.AuthCookies;
import kr.sicksick.be.onboarding.domain.RecentOnsetType;
import kr.sicksick.be.onboarding.repository.ConditionRepository;
import kr.sicksick.be.onboarding.repository.UserConditionRepository;
import kr.sicksick.be.support.IntegrationTest;

/** 회원가입을 마친 유저가 온보딩까지 끝내고 ACTIVE 가 되는 경로. */
class OnboardingFlowIntegrationTest extends IntegrationTest {

    private static final Pattern STATE = Pattern.compile("state=([^&]+)");

    @Autowired
    UserRepository users;

    @Autowired
    UserConditionRepository userConditions;

    @Autowired
    ConditionRepository conditions;

    @Test
    void 마스터_질환과_직접_입력_질환을_함께_등록한다() throws Exception {
        String token = signedUpUser("kakao", "onboarding-master");

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "씩씩이", "birthDate": "1990-03-15",
                                  "heightCm": 170, "weightKg": 62,
                                  "conditions": [
                                    { "conditionId": %d, "status": "DIAGNOSED",
                                      "symptoms": [{ "customName": "밤에 심해지는 복통" }],
                                      "recentOnsetType": "D7" },
                                    { "conditionId": null, "customName": "희귀질환 A",
                                      "customCode": "Z99", "customDescription": "직접 등록",
                                      "status": "OBSERVING",
                                      "symptoms": [{ "customName": "원인 불명 어지럼" }],
                                      "recentOnsetType": "EXACT", "recentOnsetDate": "2026-07-20" }
                                  ]
                                }
                                """.formatted(crohnId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/home"));

        var user = users.findByEmailAndDeletedAtIsNull("onboarding-master@kakao.stub.local")
                .orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getNickname()).isEqualTo("씩씩이");
        assertThat(user.getHeightCm()).isEqualTo((short) 170);

        var saved = userConditions.findAll().stream()
                .filter(c -> c.getUserId().equals(user.getId()))
                .toList();
        assertThat(saved).hasSize(2);

        // 마스터 질환은 id 로만 참조한다(이름을 읽으면 트랜잭션 밖에서 지연 로딩이 터진다).
        assertThat(saved).filteredOn(c -> c.getCondition() != null)
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.getCondition().getId()).isEqualTo(crohnId());
                    assertThat(c.getRecentOnsetType()).isEqualTo(RecentOnsetType.D7);
                    // EXACT 가 아니면 날짜를 저장하지 않는다.
                    assertThat(c.getRecentOnsetDate()).isNull();
                });

        assertThat(saved).filteredOn(c -> c.getCondition() == null)
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.getCustomName()).isEqualTo("희귀질환 A");
                    assertThat(c.getCustomCode()).isEqualTo("Z99");
                    assertThat(c.getRecentOnsetType()).isEqualTo(RecentOnsetType.EXACT);
                    assertThat(c.getRecentOnsetDate()).isNotNull();
                });
    }

    @Test
    void 질환을_건너뛰어도_등록을_마칠_수_있다() throws Exception {
        String token = signedUpUser("naver", "onboarding-skip");

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"건너뛴이","birthDate":"1995-01-01","conditions":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("/home"));

        var user = users.findByEmailAndDeletedAtIsNull("onboarding-skip@naver.stub.local")
                .orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        // 키·몸무게를 건너뛰면 비어 있어야 한다.
        assertThat(user.getHeightCm()).isNull();
        assertThat(userConditions.existsByUserId(user.getId())).isFalse();
    }

    /** 완료 화면에서 뒤로 갔다가 다시 누르면 질환이 두 번 등록될 수 있다. */
    @Test
    void 이미_마친_유저의_재호출은_거부한다() throws Exception {
        String token = signedUpUser("kakao", "onboarding-twice");
        String body = """
                {"nickname":"두번","birthDate":"1990-01-01","conditions":[]}
                """;

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록을 마쳤어요."));
    }

    @Test
    void 없는_질환_id_는_거부한다() throws Exception {
        String token = signedUpUser("naver", "onboarding-badid");

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"엉뚱","birthDate":"1990-01-01","conditions":[
                                  {"conditionId":999999,"status":"DIAGNOSED","symptoms":[]}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("등록할 수 없는 질환이에요."));
    }

    @Test
    void 닉네임이_없으면_안내_문구를_준다() throws Exception {
        String token = signedUpUser("kakao", "onboarding-noname");

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"","birthDate":"1990-01-01","conditions":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("닉네임")));
    }

    @Test
    void 검색_API_는_인증을_요구한다() throws Exception {
        mockMvc.perform(get("/api/v1/symptoms").param("query", "두통"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/conditions").param("query", "크론"))
                .andExpect(status().isUnauthorized());
    }

    // ── 헬퍼 ──

    /** 시드로 들어간 질환의 id. AUTO_INCREMENT 값을 가정하지 않는다. */
    private long crohnId() {
        return conditions.searchWithSymptoms("크론", Limit.of(1)).getFirst().getId();
    }

    /** 스텁 로그인 → 약관 → 본인인증까지 마친 유저의 액세스 토큰. */
    private String signedUpUser(String provider, String identifier) throws Exception {
        MvcResult authorize = mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/authorize"))
                .andExpect(status().isFound()).andReturn();

        Matcher matcher = STATE.matcher(authorize.getResponse().getHeader(HttpHeaders.LOCATION));
        assertThat(matcher.find()).isTrue();

        MvcResult callback = mockMvc.perform(get("/api/v1/auth/oauth/" + provider + "/callback")
                        .param("code", identifier)
                        .param("state", matcher.group(1))
                        .cookie(cookie(authorize.getResponse(), OAuthTxCookie.COOKIE_NAME)))
                .andExpect(status().isFound()).andReturn();

        String token = accessToken(cookie(callback.getResponse(), AuthCookies.REFRESH_COOKIE));

        mockMvc.perform(post("/api/v1/signup/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agreements":{"SERVICE":true,"PRIVACY":true,"PHONE_AUTH":true,
                                               "MARKETING":false,"AGE_14":true}}
                                """))
                .andExpect(status().isOk());

        String phone = "010" + String.format("%08d", Math.abs(identifier.hashCode()) % 100000000);
        String code = mockMvc.perform(post("/api/v1/signup/phone/code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"devCode\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/signup/phone/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        return token;
    }

    private String accessToken(Cookie refreshCookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private static Cookie cookie(MockHttpServletResponse response, String name) {
        Cookie found = response.getCookie(name);
        assertThat(found).as(name + " 쿠키").isNotNull();
        return found;
    }
}
