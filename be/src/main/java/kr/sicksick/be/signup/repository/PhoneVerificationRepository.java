package kr.sicksick.be.signup.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.sicksick.be.signup.domain.PhoneVerification;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /** 재요청하면 새 행이 쌓이므로, 검증은 항상 가장 최근 것 하나만 본다. */
    Optional<PhoneVerification> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
