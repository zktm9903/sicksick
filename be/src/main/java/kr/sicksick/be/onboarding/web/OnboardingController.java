package kr.sicksick.be.onboarding.web;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.service.NextStepResolver;
import kr.sicksick.be.auth.web.CurrentUser;
import kr.sicksick.be.onboarding.service.OnboardingCommand;
import kr.sicksick.be.onboarding.service.OnboardingService;

/** 온보딩 저장. 화면은 8단계지만 저장은 마지막에 한 번이다. */
@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController {

    private final OnboardingService onboardingService;
    private final NextStepResolver nextStepResolver;

    OnboardingController(OnboardingService onboardingService, NextStepResolver nextStepResolver) {
        this.onboardingService = onboardingService;
        this.nextStepResolver = nextStepResolver;
    }

    @PostMapping("/complete")
    StepResponse complete(@CurrentUser User user, @Valid @RequestBody OnboardingRequest request) {
        onboardingService.complete(user, toCommand(request), Instant.now());
        // 여기서 ACTIVE 가 되므로 nextStep 은 /home 이 된다.
        return new StepResponse(nextStepResolver.resolve(user));
    }

    private OnboardingCommand toCommand(OnboardingRequest request) {
        List<OnboardingCommand.ConditionInput> conditions = request.conditions().stream()
                .map(c -> new OnboardingCommand.ConditionInput(
                        c.conditionId(),
                        blankToNull(c.customName()),
                        blankToNull(c.customCode()),
                        blankToNull(c.customDescription()),
                        c.status(),
                        c.symptoms().stream()
                                .map(s -> new OnboardingCommand.SymptomInput(
                                        s.symptomId(), blankToNull(s.customName())))
                                .toList(),
                        c.recentOnsetType(),
                        c.recentOnsetDate()))
                .toList();

        return new OnboardingCommand(
                request.nickname().trim(),
                request.birthDate(),
                request.heightCm(),
                request.weightKg(),
                conditions);
    }

    /** 화면에서 빈 문자열이 올라오는 선택 입력들을 null 로 맞춘다. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record StepResponse(String nextStep) {
    }
}
