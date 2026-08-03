package kr.sicksick.be.onboarding.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.domain.UserStatus;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.onboarding.domain.Condition;
import kr.sicksick.be.onboarding.domain.Symptom;
import kr.sicksick.be.onboarding.domain.UserCondition;
import kr.sicksick.be.onboarding.domain.UserConditionSymptom;
import kr.sicksick.be.onboarding.repository.ConditionRepository;
import kr.sicksick.be.onboarding.repository.SymptomRepository;
import kr.sicksick.be.onboarding.repository.UserConditionRepository;

/**
 * 온보딩 완료 — 프로필과 질환·증상을 한 번에 저장하고 계정을 활성화한다.
 *
 * <p>화면은 8단계지만 저장은 여기 한 곳뿐이다. 중간 단계를 저장하지 않으므로 전부
 * 성공하거나 전부 실패한다.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final UserRepository users;
    private final ConditionRepository conditions;
    private final SymptomRepository symptoms;
    private final UserConditionRepository userConditions;

    OnboardingService(UserRepository users, ConditionRepository conditions,
                      SymptomRepository symptoms, UserConditionRepository userConditions) {
        this.users = users;
        this.conditions = conditions;
        this.symptoms = symptoms;
        this.userConditions = userConditions;
    }

    @Transactional
    public void complete(User user, OnboardingCommand command, Instant now) {
        // 이미 마친 유저가 다시 호출하면 질환이 중복 등록된다. 새로고침이나 뒤로 가기로
        // 완료 화면에 다시 들어오는 경우가 있어 서버에서 막는다.
        if (user.getStatus() == UserStatus.ACTIVE || userConditions.existsByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 등록을 마쳤어요.");
        }

        user.updateProfile(command.nickname(), command.birthDate(),
                command.heightCm(), command.weightKg(), now);

        for (OnboardingCommand.ConditionInput input : command.conditions()) {
            userConditions.save(toEntity(user.getId(), input, now));
        }

        user.activate(now);
        users.save(user);

        log.info("온보딩 완료 — userId={} 질환 {}건", user.getId(), command.conditions().size());
    }

    private UserCondition toEntity(Long userId, OnboardingCommand.ConditionInput input, Instant now) {
        Condition master = resolveCondition(input);

        UserCondition userCondition = UserCondition.of(
                userId, master, input.customName(), input.customCode(), input.customDescription(),
                input.status(), input.recentOnsetType(), input.recentOnsetDate(), now);

        resolveSymptoms(input).forEach(userCondition::addSymptom);
        return userCondition;
    }

    private Condition resolveCondition(OnboardingCommand.ConditionInput input) {
        if (input.conditionId() == null) {
            // 직접 입력. 이름이 없으면 UserCondition.of 가 막는다.
            return null;
        }
        return conditions.findById(input.conditionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "등록할 수 없는 질환이에요."));
    }

    /**
     * 마스터 증상은 id 를 한 번에 조회해 붙이고, 직접 입력한 증상은 이름만 담는다.
     *
     * <p>증상마다 findById 를 부르면 질환 수 × 증상 수만큼 쿼리가 나간다. 미리 모아서 한 번에 읽는다.
     */
    private List<UserConditionSymptom> resolveSymptoms(OnboardingCommand.ConditionInput input) {
        List<Long> ids = input.symptoms().stream()
                .map(OnboardingCommand.SymptomInput::symptomId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, Symptom> found = ids.isEmpty()
                ? Map.of()
                : symptoms.findByIdIn(ids).stream()
                        .collect(Collectors.toMap(Symptom::getId, Function.identity()));

        return input.symptoms().stream()
                .map(s -> {
                    if (s.symptomId() == null) {
                        return UserConditionSymptom.of(null, s.customName());
                    }
                    Symptom master = found.get(s.symptomId());
                    if (master == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "등록할 수 없는 증상이에요.");
                    }
                    return UserConditionSymptom.of(master, null);
                })
                .toList();
    }
}
