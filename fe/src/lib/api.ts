/** 서버가 오류 응답을 돌려준 경우. `message` 는 사용자에게 그대로 보여줄 수 있다. */
export class ApiError extends Error {
  readonly status: number
  readonly path: string
  /** 서버가 내려준 안내 문구. 없으면 undefined. */
  readonly detail?: string
  /**
   * 화면이 오류 종류에 따라 다르게 동작해야 할 때만 채워진다.
   *
   * 예: 로그인 실패가 `account_not_found` 면 alert 대신 회원가입 유도 배너를 띄운다.
   * 문구로 분기하면 서버 문구를 고치는 순간 조용히 깨지므로 코드를 본다.
   * 서버의 `kr.sicksick.be.config.ApiException` 과 짝을 이룬다.
   */
  readonly code?: string

  constructor(status: number, path: string, detail?: string, code?: string) {
    super(detail ?? `API ${status}: ${path}`)
    this.name = 'ApiError'
    this.status = status
    this.path = path
    this.detail = detail
    this.code = code
  }
}

/**
 * 서버에 닿지도 못한 경우 — 오프라인, 서버 다운, DNS 실패 등.
 *
 * `fetch` 는 이때 `TypeError` 로 reject 하는데, 그대로 두면 "요청 실패" 같은 뭉뚱그린
 * 문구로 처리돼 사용자가 원인을 짐작할 수 없다. 따로 구분해 연결 문제라고 알린다.
 */
export class NetworkError extends Error {
  constructor(cause?: unknown) {
    super('네트워크 요청에 실패했습니다')
    this.name = 'NetworkError'
    this.cause = cause
  }
}

const NETWORK_MESSAGE = '인터넷 연결이 불안정하거나 서버에 연결할 수 없어요.'

/**
 * 서버에 닿지 못했다는 뜻의 게이트웨이 오류.
 *
 * 앞단에 프록시(개발 중에는 Vite, 운영에서는 Cloudflare)가 있으면 백엔드가 죽어도
 * fetch 는 성공하고 502/503/504 가 돌아온다. 사용자 입장에서는 연결이 안 되는 것과
 * 같은 상황이므로 같은 안내를 준다.
 */
const UNREACHABLE_STATUSES = new Set([502, 503, 504])

/**
 * 오류에서 사용자에게 보여줄 문구를 뽑는다.
 *
 * 서버가 준 안내가 있으면 그게 가장 정확하므로 우선한다. 연결 자체가 실패했으면
 * 연결 안내를, 그 외에는 화면이 정한 기본 문구를 쓴다.
 */
export function toMessage(error: unknown, fallback: string): string {
  if (error instanceof NetworkError) {
    return NETWORK_MESSAGE
  }
  if (error instanceof ApiError) {
    if (error.detail) {
      return error.detail
    }
    return UNREACHABLE_STATUSES.has(error.status) ? NETWORK_MESSAGE : fallback
  }
  return fallback
}

/**
 * 액세스 토큰은 메모리에만 둔다.
 *
 * localStorage 에 넣으면 XSS 한 번에 그대로 털린다. 새로고침으로 사라져도 리프레시
 * 쿠키로 즉시 복구되므로 디스크에 남길 이유가 없다.
 */
let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

/** 세션 복구 결과. 처음 방문과 도중 만료를 구분해야 안내 문구가 달라진다. */
export type RestoreResult =
  /** 액세스 토큰을 받았다. */
  | 'restored'
  /** 로그인 상태였던 적이 없다 — 처음 방문이거나 로그아웃 상태. 안내할 것이 없다. */
  | 'no-session'
  /** 쓰던 도중 세션이 끊겼다 — 만료되었거나 서버가 폐기했다. 사용자에게 알려야 한다. */
  | 'expired'

/**
 * 이번 페이지 수명 동안 한 번이라도 로그인 상태였는지.
 *
 * 리프레시 쿠키는 HttpOnly 라 JS 로 존재 여부를 읽을 수 없고(`Set-Cookie` 는 금지된
 * 응답 헤더라 fetch 로도 못 본다), 서버는 "쿠키 없음"과 "쿠키 무효"에 똑같이 401 을
 * 준다. 그래서 클라이언트가 아는 사실로 판단한다 — 쓰던 도중 끊긴 것인지, 애초에
 * 로그인한 적이 없는 것인지.
 *
 * 처음 방문한 사람에게 "로그인이 만료됐어요"라고 하면 틀린 안내가 된다.
 */
let hadSession = false

type RefreshResponse = { accessToken: string; expiresIn: number }

/**
 * 진행 중인 재발급 요청. 동시에 여러 요청이 401 을 받아도 refresh 는 한 번만 나간다.
 *
 * 이걸 빼먹으면 화면 진입 시 동시 요청들이 각자 refresh 를 호출하고, 서버가 토큰을
 * 회전시키기 때문에 뒤늦은 요청들이 "이미 폐기된 토큰"으로 판정된다. 그러면 서버의
 * 재사용 탐지가 발동해 세션 전체가 끊긴다.
 */
let refreshing: Promise<RestoreResult> | null = null

async function refreshAccessToken(): Promise<RestoreResult> {
  let response: Response
  try {
    response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
  } catch {
    accessToken = null
    // 연결이 안 된 것은 세션 만료가 아니다. 만료 안내를 띄우면 오해를 부른다.
    return 'no-session'
  }

  if (!response.ok) {
    accessToken = null
    const result: RestoreResult = hadSession ? 'expired' : 'no-session'
    hadSession = false
    return result
  }

  const body = (await response.json()) as RefreshResponse
  accessToken = body.accessToken
  hadSession = true
  return 'restored'
}

function ensureRefreshed(): Promise<RestoreResult> {
  refreshing ??= refreshAccessToken().finally(() => {
    refreshing = null
  })
  return refreshing
}

/** 세션 복구. 앱 진입 시 한 번 호출한다. */
export function restoreSession(): Promise<RestoreResult> {
  return ensureRefreshed()
}

export async function logout(): Promise<void> {
  accessToken = null
  // 스스로 로그아웃한 것이므로 다음 진입에서 "만료" 안내가 뜨면 안 된다.
  hadSession = false
  try {
    await fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'include' })
  } catch {
    // 로그아웃은 실패해도 클라이언트 상태는 이미 비웠다. 사용자를 막을 이유가 없다.
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  /** 401 을 받아도 재발급을 시도하지 않는다(재발급 경로 자신의 무한 재귀 방지). */
  skipRefresh?: boolean
}

async function send(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  try {
    return await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      // 리프레시 쿠키를 실어 보낸다. 없으면 재발급이 동작하지 않는다.
      credentials: 'include',
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch (cause) {
    throw new NetworkError(cause)
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let response = await send(path, options)

  // 액세스 토큰이 만료됐을 뿐일 수 있다. 한 번만 재발급 후 재시도한다.
  if (response.status === 401 && !options.skipRefresh) {
    if ((await ensureRefreshed()) === 'restored') {
      response = await send(path, options)
    }
  }

  if (!response.ok) {
    const { message, code } = await readError(response)
    throw new ApiError(response.status, path, message, code)
  }

  // 204 No Content 는 본문이 없다.
  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/**
 * 서버가 내려준 안내 문구와 분기 코드를 꺼낸다.
 *
 * `message` 만 본다. 스프링 기본 오류 본문의 `error` 필드에는 `"Bad Request"` 같은
 * 프레임워크 문자열이 들어 있어서, 그걸 fallback 으로 쓰면 그대로 사용자에게 노출된다.
 *
 * `code` 는 서버가 필요할 때만 붙인다. 대부분의 오류에는 없다.
 */
async function readError(response: Response): Promise<{ message?: string; code?: string }> {
  try {
    const body = (await response.json()) as { message?: string; code?: string }
    return { message: body.message?.trim() || undefined, code: body.code || undefined }
  } catch {
    return {}
  }
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body })
}
