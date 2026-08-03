package kr.sicksick.be.onboarding.domain;

/**
 * 증상 도입 우선순위.
 *
 * <p>엑셀 마스터의 '우선순위' 컬럼에서 온 값이다. 지금은 전부 적재해 두고 구분만 갖고
 * 있으며, 나중에 노출 범위를 좁혀야 할 때 이 값으로 거른다.
 */
public enum SymptomPriority {

    /** 1차 — 이번 POC 범위(희귀질환·암·당뇨). */
    PRIMARY,

    /** 2차 — 심평원 다빈도 상병 기준의 일반적인 증상. */
    SECONDARY
}
