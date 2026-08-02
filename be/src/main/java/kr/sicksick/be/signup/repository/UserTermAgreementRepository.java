package kr.sicksick.be.signup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.sicksick.be.signup.domain.UserTermAgreement;

public interface UserTermAgreementRepository
        extends JpaRepository<UserTermAgreement, UserTermAgreement.Key> {

    List<UserTermAgreement> findByKeyUserId(Long userId);

    /** 약관 동의를 마쳤는지 판단할 때 쓴다(다음 단계 결정). */
    boolean existsByKeyUserId(Long userId);
}
