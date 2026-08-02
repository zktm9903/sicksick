package kr.sicksick.be.signup.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.sicksick.be.signup.domain.Term;

public interface TermRepository extends JpaRepository<Term, Long> {

    /** 현재 노출 중인 약관을 화면 순서대로. */
    List<Term> findByActiveTrueOrderByDisplayOrderAsc();
}
