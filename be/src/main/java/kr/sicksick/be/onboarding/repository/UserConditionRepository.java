package kr.sicksick.be.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.sicksick.be.onboarding.domain.UserCondition;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    boolean existsByUserId(Long userId);
}
