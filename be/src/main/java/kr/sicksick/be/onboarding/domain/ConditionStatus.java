package kr.sicksick.be.onboarding.domain;

/** 사용자가 등록한 질환의 진단 상태. */
public enum ConditionStatus {

    /** 진단 완료. */
    DIAGNOSED,

    /** 진단 전 — 의심되거나 관찰 중. */
    OBSERVING
}
