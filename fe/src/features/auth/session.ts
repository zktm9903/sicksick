import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { apiGet, logout as logoutRequest, restoreSession } from '@/lib/api'

export type UserStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED'

export type Session = {
  userId: number
  email: string | null
  nickname: string | null
  status: UserStatus
  phoneVerified: boolean
  /** 서버가 정한 다음 화면 경로. 프론트는 순서를 직접 판단하지 않는다. */
  nextStep: string
}

export const SESSION_QUERY_KEY = ['session'] as const

/**
 * 세션 조회 결과.
 *
 * 로그인하지 않은 경우에도 "처음부터 없었는지" / "쓰던 도중 끊겼는지"를 구분해서
 * 돌려준다. 후자만 사용자에게 만료 안내를 띄워야 한다.
 */
export type SessionState =
  | { status: 'authenticated'; session: Session }
  | { status: 'anonymous' }
  | { status: 'expired' }

/**
 * 현재 로그인 상태.
 *
 * 액세스 토큰은 메모리에만 있으므로 새로고침하면 사라진다. 조회 전에 리프레시 쿠키로
 * 세션을 복구한 뒤 /users/me 를 부른다.
 */
export function useSession() {
  return useQuery<SessionState>({
    queryKey: SESSION_QUERY_KEY,
    queryFn: async () => {
      const restored = await restoreSession()
      if (restored !== 'restored') {
        return { status: restored === 'expired' ? 'expired' : 'anonymous' }
      }
      return { status: 'authenticated', session: await apiGet<Session>('/api/v1/users/me') }
    },
    // 가입 단계가 진행되면 nextStep 이 바뀌므로 캐시를 오래 들고 있지 않는다.
    staleTime: 0,
    retry: false,
  })
}

export function useLogout() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: logoutRequest,
    onSettled: () => {
      queryClient.clear()
      navigate(ROUTES.login, { replace: true })
    },
  })
}
