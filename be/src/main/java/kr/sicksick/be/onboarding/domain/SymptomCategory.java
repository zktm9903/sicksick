package kr.sicksick.be.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 증상의 위치(대분류). 배·가슴·머리 같은 신체 큰 부위 단위다. */
@Entity
@Table(name = "symptom_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SymptomCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int displayOrder;
}
