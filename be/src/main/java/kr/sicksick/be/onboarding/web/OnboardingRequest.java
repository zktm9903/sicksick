package kr.sicksick.be.onboarding.web;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import kr.sicksick.be.onboarding.domain.ConditionStatus;
import kr.sicksick.be.onboarding.domain.RecentOnsetType;

/**
 * 온보딩 입력 전체.
 *
 * <p>화면은 8단계지만 저장은 마지막에 한 번이라 요청도 하나다. 검증 메시지는 그대로
 * 사용자에게 노출되므로 한국어로 적는다.
 *
 * @param conditions 질환. "나중에 등록할게요" 를 고르면 빈 배열이다
 */
record OnboardingRequest(
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 12, message = "닉네임은 12자까지 쓸 수 있어요.")
        String nickname,

        @NotNull(message = "생년월일을 입력해 주세요.")
        @Past(message = "생년월일이 올바르지 않아요.")
        LocalDate birthDate,

        @Min(value = 50, message = "키를 다시 확인해 주세요.")
        @Max(value = 250, message = "키를 다시 확인해 주세요.")
        Short heightCm,

        @Min(value = 10, message = "몸무게를 다시 확인해 주세요.")
        @Max(value = 300, message = "몸무게를 다시 확인해 주세요.")
        Short weightKg,

        @NotNull(message = "질환 정보가 필요해요.")
        @Valid
        List<ConditionEntry> conditions
) {

    /**
     * 등록할 질환 하나.
     *
     * @param conditionId 마스터 질환 id. null 이면 직접 입력이며 {@code customName} 이 필요하다
     * @param recentOnsetType "경험한 증상이 없어요" 를 고른 질환은 시점을 묻지 않아 null 이다
     */
    record ConditionEntry(
            Long conditionId,

            @Size(max = 100, message = "질환명은 100자까지 쓸 수 있어요.")
            String customName,

            @Size(max = 20, message = "분류 코드는 20자까지 쓸 수 있어요.")
            String customCode,

            @Size(max = 500, message = "설명은 500자까지 쓸 수 있어요.")
            String customDescription,

            @NotNull(message = "진단 상태를 골라 주세요.")
            ConditionStatus status,

            @NotNull(message = "증상 정보가 필요해요.")
            @Valid
            List<SymptomEntry> symptoms,

            RecentOnsetType recentOnsetType,
            LocalDate recentOnsetDate
    ) {
    }

    /**
     * 선택한 증상 하나.
     *
     * @param symptomId 마스터 증상 id. null 이면 직접 입력이며 {@code customName} 이 필요하다
     */
    record SymptomEntry(
            Long symptomId,

            @Size(max = 100, message = "증상명은 100자까지 쓸 수 있어요.")
            String customName
    ) {
    }
}
