package kr.sicksick.be.onboarding.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 등록한 질환.
 *
 * <p>마스터에 있는 질환({@code condition})과 사용자가 직접 입력한 질환({@code customName})을
 * 한 테이블에서 다룬다. 둘 중 하나는 반드시 있어야 하며 {@link #of} 에서 검증한다.
 */
@Entity
@Table(name = "user_conditions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** null 이면 사용자가 직접 입력한 질환이다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private Condition condition;

    @Column(length = 100)
    private String customName;

    @Column(length = 20)
    private String customCode;

    @Column(length = 500)
    private String customDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionStatus status;

    /** "경험한 증상이 없어요" 를 고른 질환은 시점을 묻지 않으므로 null 이다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecentOnsetType recentOnsetType;

    /** {@link RecentOnsetType#EXACT} 일 때만 채워진다. */
    private LocalDate recentOnsetDate;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "userCondition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserConditionSymptom> symptoms = new ArrayList<>();

    private UserCondition(Long userId, Condition condition, String customName, String customCode,
                          String customDescription, ConditionStatus status,
                          RecentOnsetType recentOnsetType, LocalDate recentOnsetDate, Instant now) {
        this.userId = userId;
        this.condition = condition;
        this.customName = customName;
        this.customCode = customCode;
        this.customDescription = customDescription;
        this.status = status;
        this.recentOnsetType = recentOnsetType;
        this.recentOnsetDate = recentOnsetDate;
        this.createdAt = now;
    }

    public static UserCondition of(Long userId, Condition condition, String customName,
                                   String customCode, String customDescription,
                                   ConditionStatus status, RecentOnsetType recentOnsetType,
                                   LocalDate recentOnsetDate, Instant now) {
        if (condition == null && (customName == null || customName.isBlank())) {
            throw new IllegalArgumentException("마스터 질환이 아니면 직접 입력한 이름이 있어야 합니다");
        }
        // EXACT 가 아닌데 날짜가 오면 저장하지 않는다. 화면에서 구간을 다시 고르면
        // 이전에 입력한 날짜가 남아 있을 수 있다.
        LocalDate date = recentOnsetType == RecentOnsetType.EXACT ? recentOnsetDate : null;
        return new UserCondition(userId, condition, customName, customCode, customDescription,
                status, recentOnsetType, date, now);
    }

    public void addSymptom(UserConditionSymptom symptom) {
        symptoms.add(symptom);
        symptom.attachTo(this);
    }

    /** 화면에 보여줄 이름 — 마스터 질환이면 그 이름, 아니면 직접 입력한 이름. */
    public String displayName() {
        return condition != null ? condition.getName() : customName;
    }
}
