import type { ReactNode } from 'react'
import { Navigate } from 'react-router'

import { ROUTES } from '@/app/routes'

import { useSession } from './session'

type SessionRedirectProps = {
  /**
   * 로그인하지 않은 사용자에게 보여줄 화면.
   *
   * 없으면 로그인 화면으로 보낸다(진입점 용도). 로그인 화면 자신을 감쌀 때는
   * 여기에 화면을 넘긴다 — 안 그러면 자기 자신으로 리다이렉트가 반복된다.
   */
  children?: ReactNode
}

/**
 * 세션을 확인해 갈 곳을 정한다.
 *
 * 앱(웹뷰)은 항상 루트로 열리고 브라우저 북마크도 대개 루트다. 거기서 무조건
 * 로그인 화면으로 보내면 리프레시 쿠키가 살아 있는 사용자도 매번 로그인 화면을
 * 보게 된다. 서버가 주는 `nextStep` 으로 보내면 로그인한 사람은 홈으로, 가입이
 * 덜 끝난 사람은 중단한 단계로 이어진다.
 *
 * 로그인 화면에도 걸어 둔다. 이미 로그인한 사람이 뒤로 가기로 돌아왔을 때 다시
 * 로그인하라고 요구할 이유가 없다.
 */
export function SessionRedirect({ children }: SessionRedirectProps) {
  const { data: state, isPending } = useSession()

  // 세션 복구는 리프레시 쿠키 왕복이라 한 박자 걸린다. 그동안 로그인 화면을
  // 잠깐 보여주면 앱을 열 때마다 화면이 깜빡인다.
  if (isPending || !state) {
    return null
  }

  if (state.status === 'authenticated') {
    // nextStep 은 /signup/… · /onboarding/… · /home 중 하나다.
    // /login 이 되는 경우가 없으므로 되돌아오는 순환이 생기지 않는다.
    return <Navigate to={state.session.nextStep} replace />
  }

  return children ? <>{children}</> : <Navigate to={ROUTES.login} replace />
}
