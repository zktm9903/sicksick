package kr.sicksick.be.onboarding.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질환 마스터.
 *
 * <p>목록에 없는 질환은 사용자가 직접 입력하므로, 이 테이블에 모든 질환이 있을 필요는 없다.
 * {@code UserCondition.conditionId} 가 null 인 경우가 그 경로다.
 */
@Entity
@Table(name = "conditions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** 질병 분류 코드(KCD-8 등). 없을 수 있다. */
    @Column(length = 20)
    private String code;

    @Column(length = 500)
    private String description;

    /**
     * 이 질환의 주요 증상. 증상 선택 화면의 후보 목록으로 쓴다.
     *
     * <p>질환당 8개 안팎이라 지연 로딩으로 충분하다.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "condition_symptoms",
            joinColumns = @JoinColumn(name = "condition_id"),
            inverseJoinColumns = @JoinColumn(name = "symptom_id"))
    private Set<Symptom> symptoms = new LinkedHashSet<>();
}
