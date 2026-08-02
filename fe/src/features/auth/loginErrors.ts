/**
 * 로그인 실패 안내 문구.
 *
 * 서버가 `/login?error=<코드>` 로 되돌려 보낼 때 쓰는 코드와 짝을 이룬다.
 * 코드를 추가·변경할 때는 `be/.../auth/web/OAuthController.java` 의 `loginError(...)`
 * 호출부와 함께 봐야 한다.
 */
const LOGIN_ERROR_MESSAGES: Record<string, string> = {
  /** 사용자가 동의 화면에서 취소했다. */
  cancelled: '로그인을 취소했어요. 다시 시도해 주세요.',

  /** 인가 트랜잭션 쿠키(5분)가 만료됐다. */
  expired: '시간이 지나 로그인이 취소됐어요. 다시 시도해 주세요.',

  /** state 불일치 — CSRF 방어에 걸렸다. */
  invalid_state: '안전하지 않은 요청이 감지됐어요. 처음부터 다시 로그인해 주세요.',

  /**
   * 같은 이메일로 이미 다른 방법으로 가입돼 있다.
   *
   * 자동 병합하지 않는 이유는 계정 탈취 방지다. 안내가 없으면 사용자가 영원히
   * 진행하지 못하므로 무엇을 해야 하는지 분명히 알려준다.
   */
  email_taken: '이미 가입된 이메일이에요. 기존에 사용하던 방법으로 로그인해 주세요.',

  /** 해당 프로바이더의 앱 키가 서버에 설정돼 있지 않다. */
  provider_not_configured: '지금은 이 방법으로 로그인할 수 없어요. 다른 방법을 이용해 주세요.',

  /** 토큰 교환·사용자 조회 실패 등 나머지. */
  failed: '로그인에 실패했어요. 잠시 후 다시 시도해 주세요.',

  /** 프론트가 붙이는 코드 — 쓰던 도중 세션이 끊겼다. */
  session_expired: '로그인이 만료됐어요. 다시 로그인해 주세요.',
}

const DEFAULT_MESSAGE = LOGIN_ERROR_MESSAGES.failed

/** 알 수 없는 코드가 와도 빈 화면이 되지 않도록 기본 문구를 돌려준다. */
export function loginErrorMessage(code: string): string {
  return LOGIN_ERROR_MESSAGES[code] ?? DEFAULT_MESSAGE
}

/** `?error=` 쿼리 이름. 서버·프론트가 같은 값을 쓴다. */
export const LOGIN_ERROR_PARAM = 'error'

/** 세션이 끊겨 로그인 화면으로 보낼 때 붙이는 코드. */
export const SESSION_EXPIRED_CODE = 'session_expired'
