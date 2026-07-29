import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'

import { apiGet } from '@/lib/api'

type TestResponse = {
  status: string
}

/**
 * 서버 연결 확인용 프로브. 결과는 화면에 그리지 않고 콘솔로만 알린다.
 * 화면에 아무것도 렌더링하지 않는다는 점을 시그니처로 못박기 위해 아무 값도 반환하지 않는다.
 */
export function useTestProbe(): void {
  const { data, error } = useQuery({
    queryKey: ['test'],
    queryFn: () => apiGet<TestResponse>('/api/v1/test'),
  })

  useEffect(() => {
    if (data) {
      console.log('[test api] 연결 성공', data)
    }
  }, [data])

  useEffect(() => {
    if (error) {
      console.error('[test api] 연결 실패', error)
    }
  }, [error])
}
