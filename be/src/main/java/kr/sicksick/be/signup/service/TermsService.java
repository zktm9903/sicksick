package kr.sicksick.be.signup.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import kr.sicksick.be.signup.domain.Term;
import kr.sicksick.be.signup.domain.UserTermAgreement;
import kr.sicksick.be.signup.repository.TermRepository;
import kr.sicksick.be.signup.repository.UserTermAgreementRepository;

/** 약관 조회와 동의 저장. */
@Service
public class TermsService {

    private final TermRepository terms;
    private final UserTermAgreementRepository agreements;

    TermsService(TermRepository terms, UserTermAgreementRepository agreements) {
        this.terms = terms;
        this.agreements = agreements;
    }

    @Transactional(readOnly = true)
    public List<Term> activeTerms() {
        return terms.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * 동의 결과를 저장한다.
     *
     * <p>선택 약관의 거부도 기록으로 남긴다(마케팅 수신 거부를 증명해야 할 수 있다).
     * 필수 약관이 하나라도 빠지면 400 이다.
     */
    @Transactional
    public void agree(Long userId, Map<String, Boolean> agreedByCode, Instant now) {
        List<Term> active = terms.findByActiveTrueOrderByDisplayOrderAsc();

        List<String> missingRequired = active.stream()
                .filter(Term::isRequired)
                .filter(term -> !Boolean.TRUE.equals(agreedByCode.get(term.getCode())))
                .map(Term::getCode)
                .toList();

        if (!missingRequired.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "필수 약관에 동의해야 합니다: " + String.join(", ", missingRequired));
        }

        // 재동의(뒤로 갔다가 다시 제출)면 기존 행을 갱신한다.
        Map<Long, UserTermAgreement> existing = agreements.findByKeyUserId(userId).stream()
                .collect(Collectors.toMap(UserTermAgreement::termId, Function.identity()));

        List<UserTermAgreement> toSave = new ArrayList<>();
        for (Term term : active) {
            boolean agreed = Boolean.TRUE.equals(agreedByCode.get(term.getCode()));
            UserTermAgreement current = existing.get(term.getId());
            if (current == null) {
                toSave.add(UserTermAgreement.of(userId, term.getId(), agreed, now));
            } else {
                current.update(agreed, now);
            }
        }
        agreements.saveAll(toSave);
    }
}
