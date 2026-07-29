/**
 * 앱의 모든 경로를 한 곳에서 정의한다.
 * 화면 이동은 문자열 리터럴 대신 항상 이 상수를 통해서만 한다.
 */
export const ROUTES = {
  login: '/login',

  /** 회원가입 — 계정 생성부터 본인인증까지 */
  signup: {
    account: '/signup/account',
    terms: '/signup/terms',
    phone: '/signup/phone',
    otp: '/signup/otp',
    done: '/signup/done',
  },

  /** 온보딩 — 프로필/질환/증상 등록 */
  onboarding: {
    nickname: '/onboarding/nickname',
    conditionGate: '/onboarding/condition-gate',
    condition: '/onboarding/condition',
    conditionConfirm: '/onboarding/condition-confirm',
    symptoms: '/onboarding/symptoms',
    recent: '/onboarding/recent',
    basic: '/onboarding/basic',
    done: '/onboarding/done',
  },

  home: '/home',
} as const
