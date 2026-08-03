package kr.sicksick.be.onboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.sicksick.be.onboarding.domain.Condition;

public interface ConditionRepository extends JpaRepository<Condition, Long> {

    /**
     * 질환 검색.
     *
     * <p>주요 증상을 함께 읽는다. 검색 결과에서 바로 증상을 보여주고, 선택 후 증상 화면의
     * 후보 목록으로도 쓰기 때문이다. {@code open-in-view=false} 라 지연 로딩으로 두면
     * 컨트롤러에서 터진다.
     *
     * <p>{@code distinct} 는 조인으로 질환이 증상 수만큼 중복되는 것을 막는다.
     */
    @Query("""
            select distinct c from Condition c
              left join fetch c.symptoms
             where c.name like concat('%', :query, '%')
                or c.code like concat('%', :query, '%')
             order by c.name
            """)
    List<Condition> searchWithSymptoms(@Param("query") String query, Limit limit);

    @Query("""
            select c from Condition c
              left join fetch c.symptoms
             where c.id = :id
            """)
    Optional<Condition> findWithSymptomsById(@Param("id") Long id);
}
