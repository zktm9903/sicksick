package kr.sicksick.be.signup.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.sicksick.be.signup.domain.Term;
import kr.sicksick.be.signup.service.TermsService;

/** 약관 목록. 가입 전에도 봐야 하므로 인증이 필요 없다. */
@RestController
@RequestMapping("/api/v1/terms")
class TermsController {

    private final TermsService termsService;

    TermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    @GetMapping
    List<TermResponse> terms() {
        return termsService.activeTerms().stream()
                .map(term -> new TermResponse(
                        term.getCode(), term.getTitle(), term.isRequired(), term.getDisplayOrder()))
                .toList();
    }

    record TermResponse(String code, String title, boolean required, int displayOrder) {
    }
}
