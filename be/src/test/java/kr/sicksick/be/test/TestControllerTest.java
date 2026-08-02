package kr.sicksick.be.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import kr.sicksick.be.support.IntegrationTest;

/**
 * 슬라이스(@WebMvcTest) 대신 전체 컨텍스트를 쓴다.
 *
 * <p>Security 필터 체인과 인자 리졸버가 붙은 뒤로는 슬라이스에서 그 의존을 일일이
 * 흉내 내야 하는데, 그렇게 만든 테스트는 정작 실제 요청이 통과하는지를 보장하지 못한다.
 */
class TestControllerTest extends IntegrationTest {

    @Test
    void 테스트_엔드포인트는_인증_없이_200과_ok_상태를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
