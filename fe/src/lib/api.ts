export class ApiError extends Error {
  readonly status: number
  readonly path: string

  constructor(status: number, path: string) {
    super(`API ${status}: ${path}`)
    this.name = 'ApiError'
    this.status = status
    this.path = path
  }
}

/**
 * 현재 도메인 기준 상대경로로 GET 요청을 보낸다.
 *
 * fetch 는 4xx/5xx 에도 reject 하지 않기 때문에, 여기서 직접 throw 해야
 * TanStack Query 가 에러 상태로 인식한다.
 */
export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw new ApiError(response.status, path)
  }

  return (await response.json()) as T
}
