import { apiPost } from '@/lib/api'

/**
 * 서버가 정한 다음 화면.
 *
 * 가입이든 로그인이든 갈 곳은 서버가 정한다. 프론트가 순서를 판단하면 배포 시점이 다른
 * 웹과 앱이 서로 다른 흐름을 태우게 된다.
 */
type StepResponse = { nextStep: string }

/**
 * 로그인 실패 사유. 서버 `LoginFailedException.Reason` 과 짝을 이룬다.
 *
 * 미가입 계정만 회원가입 유도 배너를 띄우고 나머지는 alert 로 안내한다.
 */
export const ACCOUNT_NOT_FOUND = 'account_not_found'

/**
 * 이메일·비밀번호로 계정을 만든다.
 *
 * 성공하면 서버가 리프레시 쿠키를 심는다 — 이 시점부터 로그인 상태다. 액세스 토큰은
 * 응답에 실려 오지 않고, 세션 조회(`useSession`)가 `/auth/refresh` 로 받아간다.
 */
export function signUpWithPassword(email: string, password: string) {
  return apiPost<StepResponse>('/api/v1/auth/signup', { email, password })
}

export function loginWithPassword(email: string, password: string) {
  return apiPost<StepResponse>('/api/v1/auth/login', { email, password })
}
