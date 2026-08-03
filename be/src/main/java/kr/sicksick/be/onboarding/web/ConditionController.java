package kr.sicksick.be.onboarding.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.sicksick.be.onboarding.domain.Condition;
import kr.sicksick.be.onboarding.domain.Symptom;
import kr.sicksick.be.onboarding.repository.ConditionRepository;

/**
 * 질환 마스터 검색.
 *
 * <p>마스터에 없는 질환은 사용자가 직접 입력하므로, 결과가 비어도 정상이다. 화면은 항상
 * "'…' 직접 입력하기" 항목을 함께 보여준다.
 */
@RestController
@RequestMapping("/api/v1/conditions")
class ConditionController {

    private static final int MAX_RESULTS = 20;

    private final ConditionRepository conditions;

    ConditionController(ConditionRepository conditions) {
        this.conditions = conditions;
    }

    @GetMapping
    List<ConditionResponse> search(@RequestParam(required = false) String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return conditions.searchWithSymptoms(query.trim(), Limit.of(MAX_RESULTS)).stream()
                .map(ConditionResponse::from)
                .toList();
    }

    record ConditionResponse(Long id, String name, String code, String description,
                             List<SymptomSummary> symptoms) {

        static ConditionResponse from(Condition condition) {
            List<SymptomSummary> symptoms = condition.getSymptoms().stream()
                    // Set 이라 순서가 보장되지 않는다. 화면에 매번 같은 순서로 보이게 정렬한다.
                    .sorted(Comparator.comparing(Symptom::getId))
                    .map(s -> new SymptomSummary(s.getId(), s.getName()))
                    .toList();

            return new ConditionResponse(
                    condition.getId(),
                    condition.getName(),
                    condition.getCode(),
                    condition.getDescription(),
                    symptoms);
        }
    }

    record SymptomSummary(Long id, String name) {
    }
}
