package kr.sicksick.be.onboarding.domain;

/**
 * 가장 최근 증상이 나타난 시점.
 *
 * <p>정확한 날짜를 기억하지 못하는 경우가 많아 구간으로도 받는다. 화면 문구는 프론트
 * 상수({@code features/onboarding/constants.ts})에 있고 여기는 값만 정의한다.
 */
public enum RecentOnsetType {

    /** 정확한 날짜를 골랐다. 이때만 날짜 값이 함께 저장된다. */
    EXACT,

    /** 최근 7일 이내. */
    D7,

    /** 최근 8~30일. */
    D30,

    /** 최근 1~3개월. */
    M3,

    /** 3개월보다 오래전. */
    OLD,

    /** 잘 기억나지 않음. */
    UNKNOWN
}
