import type { ReactNode } from 'react'
import { Navigate } from 'react-router'

import { ROUTES } from '@/app/routes'

import { LOGIN_ERROR_PARAM, SESSION_EXPIRED_CODE } from './loginErrors'
import { useSession } from './session'

type OnboardingGuardProps = {
  children: ReactNode
}

/** 가입 절차가 남아 있으면 서버가 이 접두어로 시작하는 경로를 알려준다. */
const SIGNUP_PREFIX = '/signup/'

/**
 * 온보딩 화면들의 진입 가드.
 *
 * <p>`AuthGuard` 를 쓸 수 없다. 그쪽은 서버가 알려준 `nextStep` 과 현재 경로가 정확히
 * 일치할 때만 통과시키는데, 서버는 온보딩이 진행 중인 내내 `/onboarding/nickname` 만
 * 알려준다(입력을 마지막에 한 번에 저장하므로 중간 진행도를 모른다). 그대로 쓰면
 * 두 번째 화면부터 전부 튕겨나간다.
 *
 * 그렇다고 `allowAnyStep` 으로 단계 검사를 통째로 끄면 약관도 마치지 않은 유저가
 * URL 로 온보딩에 들어올 수 있다. 그래서 "가입은 끝났는가"만 확인한다.
 */
export function OnboardingGuard({ children }: OnboardingGuardProps) {
  const { data: state, isPending } = useSession()

  // 세션 복구 중에는 아무것도 그리지 않는다. 로그인 화면을 잠깐 보여주면 새로고침할
  // 때마다 화면이 깜빡인다.
  if (isPending || !state) {
    return null
  }

  if (state.status !== 'authenticated') {
    const to =
      state.status === 'expired'
        ? `${ROUTES.login}?${LOGIN_ERROR_PARAM}=${SESSION_EXPIRED_CODE}`
        : ROUTES.login
    return <Navigate to={to} replace />
  }

  // 약관·본인인증이 남아 있으면 그쪽을 먼저 마쳐야 한다.
  if (state.session.nextStep.startsWith(SIGNUP_PREFIX)) {
    return <Navigate to={state.session.nextStep} replace />
  }

  return <>{children}</>
}
