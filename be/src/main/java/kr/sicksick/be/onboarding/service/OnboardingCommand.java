package kr.sicksick.be.onboarding.service;

import java.time.LocalDate;
import java.util.List;

import kr.sicksick.be.onboarding.domain.ConditionStatus;
import kr.sicksick.be.onboarding.domain.RecentOnsetType;

/**
 * 온보딩 저장 입력.
 *
 * <p>웹 요청 DTO(`OnboardingRequest`)와 형태가 같지만 따로 둔다. 서비스가 검증 애너테이션과
 * HTTP 계층에 묶이지 않게 하기 위해서다.
 */
public record OnboardingCommand(
        String nickname,
        LocalDate birthDate,
        Short heightCm,
        Short weightKg,
        List<ConditionInput> conditions
) {

    public record ConditionInput(
            Long conditionId,
            String customName,
            String customCode,
            String customDescription,
            ConditionStatus status,
            List<SymptomInput> symptoms,
            RecentOnsetType recentOnsetType,
            LocalDate recentOnsetDate
    ) {
    }

    public record SymptomInput(Long symptomId, String customName) {
    }
}
