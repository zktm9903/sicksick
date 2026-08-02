package kr.sicksick.be.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.sicksick.be.auth.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 탈퇴한 계정의 이메일은 선점 판단에서 제외한다. */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
