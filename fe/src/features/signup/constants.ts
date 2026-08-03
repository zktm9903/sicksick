/**
 * 회원가입 진행바 단계.
 *
 * `auth` 는 이메일·비밀번호 자체 가입 화면 자리다. 소셜 로그인으로 들어오면 건너뛰지만
 * 진행바에는 남겨 전체 길이가 흔들리지 않게 한다.
 */
export const SIGNUP_STEPS = ['auth', 'terms', 'phone', 'otp'] as const

export type SignupStep = (typeof SIGNUP_STEPS)[number]
