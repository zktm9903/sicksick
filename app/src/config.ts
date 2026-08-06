import Constants from 'expo-constants'

/**
 * 웹뷰가 띄울 주소.
 *
 * `app.config.ts` 가 `EXPO_PUBLIC_WEB_URL` 을 읽어 `extra.webUrl` 에 넣어둔다.
 * 값이 없으면 운영을 본다.
 */
export const WEB_URL: string =
  (Constants.expoConfig?.extra?.webUrl as string | undefined) ?? 'https://sicksick.kr'

/**
 * UserAgent 에 덧붙일 앱 식별자.
 *
 * ★ 백엔드가 이 문자열로 앱을 판별한다(`ClientType.APP_USER_AGENT_MARKER`).
 * 앱으로 인식되면 리프레시 토큰 수명이 90일이 되고, 웹으로 인식되면 14일이다.
 * 즉 이 값이 빠지면 앱에서도 2주마다 로그아웃된다.
 *
 * `userAgent` prop 으로 UA 를 통째로 덮어쓰면 안 된다. 기본 UA 에 담긴 브라우저·OS
 * 정보가 사라져 카카오·네이버 로그인 화면이 클라이언트를 판별하지 못한다.
 * `applicationNameForUserAgent` 는 기존 UA 뒤에 덧붙이므로 둘 다 만족한다.
 */
export const APP_USER_AGENT = 'SicksickApp/1.0'

/** 우리 웹의 로그인 화면. 인증 페이지에서 빠져나올 때 돌아갈 곳이다. */
export const LOGIN_PATH = '/login'

/** 우리가 서비스하는 도메인. */
const OUR_HOST_SUFFIXES = ['sicksick.kr']

/**
 * 소셜 로그인 도메인.
 *
 * 우리 것은 아니지만 웹뷰 안에서 열어야 한다. 외부 브라우저로 빠지면 콜백이 심는
 * 쿠키가 웹뷰 저장소에 남지 않아 로그인이 완료되지 않는다.
 */
const AUTH_PROVIDER_HOST_SUFFIXES = [
  // 카카오 로그인
  'kakao.com',
  'kakaocdn.net',
  // 네이버 로그인
  'naver.com',
  'naver.net',
]

/** 웹뷰 안에서 열어야 하는 호스트 = 우리 것 + 인증 프로바이더. */
const INTERNAL_HOST_SUFFIXES = [...OUR_HOST_SUFFIXES, ...AUTH_PROVIDER_HOST_SUFFIXES]

/** 로컬 개발 서버(사설 IP)도 웹뷰 안에서 연다. */
function isPrivateHost(hostname: string): boolean {
  return (
    hostname === 'localhost' ||
    /^10\./.test(hostname) ||
    /^192\.168\./.test(hostname) ||
    /^172\.(1[6-9]|2\d|3[01])\./.test(hostname)
  )
}

function matchesAny(hostname: string, suffixes: string[]): boolean {
  return suffixes.some((suffix) => hostname === suffix || hostname.endsWith(`.${suffix}`))
}

const WEB_PROTOCOLS = ['http:', 'https:']

/**
 * 웹뷰가 스스로 처리하는 스킴.
 *
 * <p>여기 없는 비(非) HTTP 스킴은 시스템에 넘긴다. {@code kakaotalk://} 이 대표적인데,
 * "카카오톡으로 로그인"이 이 스킴으로 앱을 띄운다 — 웹뷰가 붙들면 아무 일도 안 일어난다.
 */
const WEBVIEW_SCHEMES = ['about:', 'data:', 'blob:', 'javascript:']

/**
 * URL 을 (프로토콜, 호스트)로 쪼갠다. 파싱 자체가 실패하면 null.
 *
 * <p>{@code new URL('about:blank')} 은 <b>예외를 던지지 않는다</b>(호스트가 빈 문자열인
 * 정상 URL 이다). 그래서 특수 스킴을 try/catch 로 거르려 하면 걸러지지 않고 "아무
 * 호스트에도 해당하지 않는 주소"로 취급돼 외부 브라우저로 새어 나간다.
 */
function parse(url: string): { protocol: string; hostname: string } | null {
  try {
    const { protocol, hostname } = new URL(url)
    return { protocol, hostname }
  } catch {
    return null
  }
}

export function isInternalUrl(url: string): boolean {
  const parsed = parse(url)
  if (parsed === null) {
    return true // 해석할 수 없는 주소는 웹뷰에 맡긴다.
  }
  if (!WEB_PROTOCOLS.includes(parsed.protocol)) {
    return WEBVIEW_SCHEMES.includes(parsed.protocol)
  }
  return isPrivateHost(parsed.hostname) || matchesAny(parsed.hostname, INTERNAL_HOST_SUFFIXES)
}

/**
 * 우리 웹 화면인가.
 *
 * <p>{@link isInternalUrl} 과 달리 카카오·네이버는 제외한다. 둘 다 웹뷰 안에서 열지만,
 * 프로바이더 페이지에서는 앱이 뒤로가기 수단을 대신 마련해 줘야 하기 때문이다.
 * 그 페이지는 우리가 만들지 않아서 나가는 길이 없다.
 */
export function isOurSite(url: string): boolean {
  const parsed = parse(url)
  if (parsed === null || !WEB_PROTOCOLS.includes(parsed.protocol)) {
    // about:blank 같은 중간 상태. 화면으로 보이는 페이지는 항상 http(s) 이므로
    // 우리 것으로 봐서 뒤로가기 바가 한 프레임 깜빡이는 것을 막는다.
    return true
  }
  return isPrivateHost(parsed.hostname) || matchesAny(parsed.hostname, OUR_HOST_SUFFIXES)
}
