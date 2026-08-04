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

/**
 * 웹뷰 안에서 열어야 하는 호스트.
 *
 * 소셜 로그인은 카카오·네이버 도메인을 거쳐 우리 콜백으로 돌아오는데, 중간에
 * 외부 브라우저로 빠지면 콜백이 심는 쿠키가 웹뷰 저장소에 남지 않아 로그인이
 * 완료되지 않는다. 그래서 인증 도메인은 반드시 웹뷰 안에서 처리한다.
 */
const INTERNAL_HOST_SUFFIXES = [
  'sicksick.kr',
  // 카카오 로그인
  'kakao.com',
  'kakaocdn.net',
  // 네이버 로그인
  'naver.com',
  'naver.net',
]

/** 로컬 개발 서버(사설 IP)도 웹뷰 안에서 연다. */
function isPrivateHost(hostname: string): boolean {
  return (
    hostname === 'localhost' ||
    /^10\./.test(hostname) ||
    /^192\.168\./.test(hostname) ||
    /^172\.(1[6-9]|2\d|3[01])\./.test(hostname)
  )
}

export function isInternalUrl(url: string): boolean {
  try {
    const { hostname } = new URL(url)
    return (
      isPrivateHost(hostname) ||
      INTERNAL_HOST_SUFFIXES.some(
        (suffix) => hostname === suffix || hostname.endsWith(`.${suffix}`),
      )
    )
  } catch {
    // about:blank, data: 같은 특수 스킴. 웹뷰가 알아서 처리하게 둔다.
    return true
  }
}
