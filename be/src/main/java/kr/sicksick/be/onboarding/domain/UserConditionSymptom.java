package kr.sicksick.be.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 사용자가 질환별로 선택한 경험 증상.
 *
 * <p>질환과 마찬가지로 마스터 참조({@code symptom})와 직접 입력({@code customName})을 함께
 * 다룬다. 마스터 증상은 이름을 복사하지 않고 id 로 참조해, 나중에 명칭이 바뀌어도 과거
 * 기록과 연결이 유지되게 한다.
 */
@Entity
@Table(name = "user_condition_symptoms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConditionSymptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_condition_id", nullable = false)
    private UserCondition userCondition;

    /** null 이면 사용자가 직접 입력한 증상이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symptom_id")
    private Symptom symptom;

    @Column(length = 100)
    private String customName;

    private UserConditionSymptom(Symptom symptom, String customName) {
        this.symptom = symptom;
        this.customName = customName;
    }

    public static UserConditionSymptom of(Symptom symptom, String customName) {
        if (symptom == null && (customName == null || customName.isBlank())) {
            throw new IllegalArgumentException("마스터 증상이 아니면 직접 입력한 이름이 있어야 합니다");
        }
        return new UserConditionSymptom(symptom, customName);
    }

    void attachTo(UserCondition userCondition) {
        this.userCondition = userCondition;
    }

    public String displayName() {
        return symptom != null ? symptom.getName() : customName;
    }
}
