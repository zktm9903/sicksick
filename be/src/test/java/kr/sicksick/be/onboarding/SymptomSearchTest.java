package kr.sicksick.be.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

import kr.sicksick.be.onboarding.domain.Symptom;
import kr.sicksick.be.onboarding.domain.SymptomPriority;
import kr.sicksick.be.onboarding.repository.ConditionRepository;
import kr.sicksick.be.onboarding.repository.SymptomRepository;
import kr.sicksick.be.support.IntegrationTest;

/**
 * 증상 마스터 시드와 검색.
 *
 * <p>시드는 엑셀에서 스크립트로 생성한다. 건수가 어긋나면 생성이 잘못됐거나 엑셀이
 * 바뀐 것이므로 여기서 잡는다.
 */
class SymptomSearchTest extends IntegrationTest {

    @Autowired
    SymptomRepository symptoms;

    @Autowired
    ConditionRepository conditions;

    @Test
    void 엑셀_시드가_그대로_적재된다() {
        List<Symptom> all = symptoms.findAll();

        assertThat(all).hasSize(127);
        // 엑셀 '안내' 시트가 밝힌 1차(POC) 범위 건수.
        assertThat(all.stream().filter(s -> s.getPriority() == SymptomPriority.PRIMARY)).hasSize(36);
        // HPO 매핑을 확정하지 못해 의도적으로 비운 항목.
        assertThat(all.stream().filter(s -> s.getHpoCode() == null)).hasSize(7);
    }

    /**
     * 사용자는 임상 용어를 모른다. 일상 표현·영어·하위유형 어느 쪽으로 검색해도
     * 같은 증상에 도달해야 한다.
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @CsvSource({
            "발열,   발열",   // 의료명칭
            "Fever, 발열",   // 영어
            "열남,   발열",   // 일상 표현(동의어)
            "미열,   발열",   // 연관 검색어
            "두통,   두통",
            "편두통, 두통",   // 하위유형(연관 검색어)
            "머리 아픔, 두통",
    })
    void 이름_동의어_연관어_어느_쪽으로도_찾을_수_있다(String query, String expected) {
        List<Symptom> found = symptoms.search(query, Limit.of(20));

        assertThat(found).extracting(Symptom::getName).contains(expected);
    }

    @Test
    void 정확히_일치하는_이름이_가장_위에_온다() {
        List<Symptom> found = symptoms.search("두통", Limit.of(20));

        assertThat(found).isNotEmpty();
        assertThat(found.getFirst().getName()).isEqualTo("두통");
    }

    @Test
    void 일치하는_증상이_없으면_빈_결과다() {
        assertThat(symptoms.search("존재하지않는증상xyz", Limit.of(20))).isEmpty();
    }

    @Test
    void 질환은_주요_증상과_함께_조회된다() {
        var found = conditions.searchWithSymptoms("크론", Limit.of(20));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getCode()).isEqualTo("K50");
        assertThat(found.getFirst().getSymptoms())
                .extracting(Symptom::getName)
                .contains("복통", "설사", "체중 감소");
    }
}
