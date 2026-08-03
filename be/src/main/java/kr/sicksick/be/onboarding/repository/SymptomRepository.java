package kr.sicksick.be.onboarding.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.sicksick.be.onboarding.domain.Symptom;

public interface SymptomRepository extends JpaRepository<Symptom, Long> {

    /**
     * 증상 검색.
     *
     * <p>이름(의료명칭·영어)뿐 아니라 동의어와 연관 검색어까지 훑는다. 사용자는 '열남'
     * 처럼 일상 표현으로 찾거나 '편두통' 처럼 하위유형으로 찾기 때문이다.
     *
     * <p>동의어·연관어 테이블은 검색 전용이라 엔티티로 매핑하지 않았고, 여기서 네이티브
     * 서브쿼리로만 참조한다. 127행 규모라 term 인덱스만으로 충분하다.
     *
     * <p>정렬 기준: 이름이 검색어로 시작하는 것 → 이름에 포함된 것 → 나머지. 사용자가
     * 입력한 그대로에 가까운 결과를 위로 올린다.
     *
     * <p>카테고리는 {@link #withCategories}로 따로 채운다. 네이티브 쿼리라 fetch join 을
     * 쓸 수 없는데, {@code open-in-view=false} 라 응답을 만들 때 지연 로딩이 터진다.
     */
    @Query(value = """
            SELECT s.* FROM symptoms s
             WHERE s.name LIKE CONCAT('%', :query, '%')
                OR s.name_en LIKE CONCAT('%', :query, '%')
                OR EXISTS (SELECT 1 FROM symptom_synonyms y
                            WHERE y.symptom_id = s.id AND y.term LIKE CONCAT('%', :query, '%'))
                OR EXISTS (SELECT 1 FROM symptom_related_terms r
                            WHERE r.symptom_id = s.id AND r.term LIKE CONCAT('%', :query, '%'))
             ORDER BY CASE
                        WHEN s.name = :query THEN 0
                        WHEN s.name LIKE CONCAT(:query, '%') THEN 1
                        WHEN s.name LIKE CONCAT('%', :query, '%') THEN 2
                        ELSE 3
                      END,
                      s.name
            """, nativeQuery = true)
    List<Symptom> search(@Param("query") String query, Limit limit);

    /**
     * 검색 결과에 카테고리를 붙여 다시 읽는다.
     *
     * <p>id 순서로 돌아오므로 호출부가 원래 정렬을 유지하려면 직접 맞춰야 한다.
     */
    @Query("""
            select s from Symptom s
              join fetch s.category
             where s.id in :ids
            """)
    List<Symptom> withCategories(@Param("ids") Collection<Long> ids);

    @Query("""
            select s from Symptom s
              join fetch s.category
             where s.id in :ids
            """)
    List<Symptom> findByIdIn(Collection<Long> ids);
}
