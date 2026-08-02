package kr.sicksick.be.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 고정 인증번호 판정.
 *
 * <p>빈 문자열·공백을 "설정됨"으로 잘못 읽으면, 값을 비워 실제 발송으로 전환하려 할 때
 * 여전히 고정값이 쓰이는 것처럼 동작한다. 전환 시점에 조용히 어긋나는 부분이라 못박는다.
 */
class SignupPropertiesTest {

    @Test
    void 값이_있으면_고정_인증번호를_쓴다() {
        assertThat(new SignupProperties("123456").hasFixedCode()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void 비어_있으면_난수로_발급한다(String value) {
        assertThat(new SignupProperties(value).hasFixedCode()).isFalse();
    }
}
