import { QueryClient } from '@tanstack/react-query'

/**
 * 앱 전역에서 하나만 존재하는 QueryClient.
 * 컴포넌트 안에서 만들면 리렌더마다 캐시가 초기화되므로 모듈 스코프에 둔다.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 기본값(3회)은 연결 실패를 너무 늦게 드러낸다.
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})
