package kr.sicksick.be.onboarding.web;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.sicksick.be.onboarding.domain.Symptom;
import kr.sicksick.be.onboarding.repository.SymptomRepository;

/** 증상 마스터 검색. 온보딩에서 직접 입력한 질환의 증상을 고를 때 쓴다. */
@RestController
@RequestMapping("/api/v1/symptoms")
class SymptomController {

    /** 화면이 목록으로 보여줄 수 있는 정도. 더 필요하면 검색어를 좁히는 게 낫다. */
    private static final int MAX_RESULTS = 20;

    private final SymptomRepository symptoms;

    SymptomController(SymptomRepository symptoms) {
        this.symptoms = symptoms;
    }

    @GetMapping
    List<SymptomResponse> search(@RequestParam(required = false) String query) {
        // 빈 검색어로 127건을 통째로 내려보내지 않는다. 화면도 검색 전에는 목록을 감춘다.
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 1) 네이티브 쿼리로 순위대로 id 를 뽑고
        List<Long> ranked = symptoms.search(query.trim(), Limit.of(MAX_RESULTS)).stream()
                .map(Symptom::getId)
                .toList();
        if (ranked.isEmpty()) {
            return List.of();
        }

        // 2) 카테고리를 함께 읽어온 뒤 1)의 순서로 되돌린다.
        //    네이티브 쿼리에는 fetch join 을 쓸 수 없고, open-in-view=false 라
        //    응답을 만드는 시점에는 지연 로딩이 불가능하다.
        Map<Long, Symptom> loaded = symptoms.withCategories(ranked).stream()
                .collect(Collectors.toMap(Symptom::getId, Function.identity()));

        return ranked.stream()
                .map(loaded::get)
                .filter(Objects::nonNull)
                .map(SymptomResponse::from)
                .toList();
    }

    record SymptomResponse(Long id, String name, String nameKo, String description,
                           String category, String detailLocation) {

        static SymptomResponse from(Symptom symptom) {
            return new SymptomResponse(
                    symptom.getId(),
                    symptom.getName(),
                    symptom.getNameKo(),
                    symptom.getDescription(),
                    symptom.getCategory().getName(),
                    symptom.getDetailLocation());
        }
    }
}
