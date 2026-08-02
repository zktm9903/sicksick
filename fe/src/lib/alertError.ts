import { toMessage } from './api'

/**
 * 오류 안내를 띄운다.
 *
 * 호출을 이 파일 하나로 모으는 이유:
 * - 나중에 토스트나 커스텀 모달로 바꿀 때 호출부를 건드리지 않아도 된다
 * - 테스트·브라우저 자동화에서 한 지점만 가로채면 된다
 *
 * 웹뷰 앱 주의: iOS `WKWebView` 는 네이티브가 `runJavaScriptAlertPanel` 델리게이트를
 * 구현하지 않으면 `alert` 를 조용히 무시한다. Android `WebView` 도
 * `WebChromeClient.onJsAlert` 가 필요하다. 앱 래핑 시 반드시 확인할 것.
 */
export function alertMessage(message: string): void {
  window.alert(message)
}

/**
 * 오류 객체에서 사용자용 문구를 뽑아 띄운다.
 *
 * @param fallback 서버가 안내를 주지 않았을 때 쓸 화면별 기본 문구
 */
export function alertError(error: unknown, fallback: string): void {
  alertMessage(toMessage(error, fallback))
}
