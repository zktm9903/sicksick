package kr.sicksick.be.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증상 마스터.
 *
 * <p>동의어({@code symptom_synonyms})와 연관 검색어({@code symptom_related_terms})는 검색
 * 전용이라 엔티티로 매핑하지 않는다. 조회 쿼리에서만 조인해 쓴다.
 */
@Entity
@Table(name = "symptoms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 임상에서 쓰는 전문 용어. 화면 표시의 기준값이다. */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String nameEn;

    /** 사용자가 검색할 법한 일상 표현. */
    @Column(length = 200)
    private String nameKo;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SymptomCategory category;

    /** 통증·발진처럼 부위가 세분화되는 증상에만 있다. */
    @Column(length = 200)
    private String detailLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SymptomPriority priority;

    /** Human Phenotype Ontology 코드. 매핑을 확정하지 못한 항목은 null 이다. */
    @Column(length = 20)
    private String hpoCode;
}
