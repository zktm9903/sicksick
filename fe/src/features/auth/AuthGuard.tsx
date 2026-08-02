import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'

import { ROUTES } from '@/app/routes'

import { LOGIN_ERROR_PARAM, SESSION_EXPIRED_CODE } from './loginErrors'
import { useSession } from './session'

type AuthGuardProps = {
  children: ReactNode
  /**
   * 서버가 지정한 다음 단계와 현재 경로가 달라도 그대로 둔다.
   *
   * 완료 화면처럼 "이미 지나온 단계"를 의도적으로 보여주는 경우에 쓴다.
   * 이게 없으면 /signup/done 에 도착하자마자 온보딩으로 튕겨나간다.
   */
  allowAnyStep?: boolean
}

/**
 * 로그인이 필요한 화면을 감싼다.
 *
 * 미로그인이면 로그인 화면으로, 로그인했지만 아직 지나야 할 단계가 남았으면 서버가
 * 알려준 단계로 보낸다. 판단 기준이 서버에 있으므로 화면을 추가해도 이 컴포넌트는
 * 손대지 않는다.
 */
export function AuthGuard({ children, allowAnyStep = false }: AuthGuardProps) {
  const { data: state, isPending } = useSession()
  const { pathname } = useLocation()

  // 세션 복구 중에는 아무것도 그리지 않는다. 여기서 로그인 화면을 잠깐 보여주면
  // 새로고침할 때마다 화면이 깜빡인다.
  if (isPending || !state) {
    return null
  }

  if (state.status !== 'authenticated') {
    // 쓰던 도중 끊긴 경우에만 만료를 알린다. 처음 방문한 사람에게
    // "로그인이 만료됐어요"라고 하면 틀린 안내가 된다.
    const to =
      state.status === 'expired'
        ? `${ROUTES.login}?${LOGIN_ERROR_PARAM}=${SESSION_EXPIRED_CODE}`
        : ROUTES.login
    return <Navigate to={to} replace />
  }

  if (!allowAnyStep && state.session.nextStep !== pathname) {
    return <Navigate to={state.session.nextStep} replace />
  }

  return <>{children}</>
}
