import { useCallback, useEffect, useRef, useState } from 'react'
import { BackHandler, Linking, Platform, StyleSheet } from 'react-native'
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context'
import { StatusBar } from 'expo-status-bar'
import * as SplashScreen from 'expo-splash-screen'
import { WebView, type WebViewNavigation } from 'react-native-webview'

import { APP_USER_AGENT, LOGIN_PATH, WEB_URL, isInternalUrl, isOurSite } from './src/config'
import { AuthHeader } from './src/AuthHeader'
import { ErrorScreen } from './src/ErrorScreen'

/**
 * 로고를 최소 이만큼은 보여준다.
 *
 * <p>웹이 뜨는 즉시 스플래시를 내리면, 캐시가 살아 있거나 연결이 빠를 때 로고가
 * 한 프레임 스쳤다 사라져 화면이 깜빡인 것처럼 보인다. 반대로 고정 지연을 주면
 * 느린 연결에서 기다림이 그만큼 더 길어진다. 그래서 '최소' 시간만 정한다 —
 * 웹이 이보다 늦게 뜨면 어차피 그때까지 스플래시가 떠 있다.
 */
const MIN_SPLASH_MS = 1000

/** JS 번들이 평가되는 시점. 실제 실행 시작에 가장 가까운 기준이다. */
const LAUNCHED_AT = Date.now()

// 웹이 그려질 때까지 스플래시를 유지한다. 이게 없으면 흰 화면이 먼저 보인다.
SplashScreen.preventAutoHideAsync().catch(() => {
  // 이미 숨겨졌거나 지원되지 않는 환경. 표시 타이밍만 달라질 뿐 동작에는 지장이 없다.
})

try {
  // 툭 끊기지 않고 서서히 사라지게 한다(fade 는 iOS 전용이라 안드로이드는 무시한다).
  SplashScreen.setOptions({ duration: 300, fade: true })
} catch {
  // 옵션을 못 받는 환경이면 기본 동작으로 사라진다. 기동을 막을 이유는 없다.
}

export default function App() {
  const webViewRef = useRef<WebView>(null)
  const [failed, setFailed] = useState(false)

  /**
   * 지금 보고 있는 것이 소셜 로그인 페이지인지.
   *
   * 카카오·네이버 화면은 우리가 만든 게 아니라 빠져나올 수단이 없다. 그동안만
   * 네이티브 뒤로가기 바를 얹는다.
   */
  const [onProviderPage, setOnProviderPage] = useState(false)

  /**
   * 웹 히스토리가 있는지. 렌더마다 리스너를 다시 붙이지 않으려고 ref 에 담는다.
   */
  const canGoBack = useRef(false)

  /** 프로바이더 페이지인지의 ref 판본. 아래 뒤로가기 리스너가 읽는다. */
  const onProviderPageRef = useRef(false)

  /**
   * 소셜 로그인을 그만두고 로그인 화면으로 돌아간다.
   *
   * 히스토리를 되짚지 않고 목적지로 바로 보내는 이유: 카카오 로그인은 내부적으로
   * 여러 페이지(이메일 → 비밀번호 → 동의)를 거쳐서 goBack() 으로는 한 칸씩 여러 번
   * 눌러야 빠져나온다. 돌아갈 곳이 로그인 화면으로 정해져 있으므로 한 번에 보낸다.
   *
   * assign 이 아니라 replace 라야 카카오 페이지가 히스토리에 남지 않는다.
   * injectJavaScript 는 호스트 권한으로 실행돼 크로스 오리진·CSP 와 무관하다.
   */
  const leaveProviderPage = useCallback(() => {
    webViewRef.current?.injectJavaScript(
      `location.replace(${JSON.stringify(WEB_URL + LOGIN_PATH)}); true;`,
    )
  }, [])

  /**
   * 안드로이드 하드웨어 뒤로가기.
   *
   * 기본 동작은 앱 종료다. 그대로 두면 가입 도중 뒤로 눌렀을 때 앱이 꺼진다.
   * 웹 히스토리가 있으면 그쪽을 먼저 되짚는다.
   */
  useEffect(() => {
    if (Platform.OS !== 'android') {
      return
    }
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      // 프로바이더 페이지에서는 화면의 뒤로가기 바와 같게 동작시킨다.
      // 여기서 goBack() 을 쓰면 카카오 내부 단계를 한 칸씩 되짚는다.
      if (onProviderPageRef.current) {
        leaveProviderPage()
        return true
      }
      if (canGoBack.current) {
        webViewRef.current?.goBack()
        return true
      }
      return false // 첫 화면이면 기본 동작(앱 종료)에 맡긴다.
    })
    return () => subscription.remove()
  }, [leaveProviderPage])

  const handleNavigationStateChange = useCallback((state: WebViewNavigation) => {
    canGoBack.current = state.canGoBack

    const external = !isOurSite(state.url)
    onProviderPageRef.current = external
    setOnProviderPage(external)
  }, [])

  /**
   * 외부 링크는 시스템 브라우저로 보낸다.
   *
   * 다만 소셜 로그인 도메인은 예외다. 카카오·네이버를 외부 브라우저로 빼면 콜백이
   * 심는 쿠키가 웹뷰 저장소에 남지 않아 로그인이 끝나지 않는다.
   */
  const handleShouldStartLoad = useCallback((request: WebViewNavigation) => {
    if (isInternalUrl(request.url)) {
      return true
    }
    Linking.openURL(request.url).catch(() => {
      // 열 수 없는 스킴이면 무시한다. 앱이 죽는 것보다 낫다.
    })
    return false
  }, [])

  /** 스플래시를 이미 내렸는지. onLoadEnd·onError 가 각각 부를 수 있다. */
  const splashHidden = useRef(false)

  const hideSplash = useCallback(() => {
    if (splashHidden.current) {
      return
    }
    splashHidden.current = true

    // 웹이 빨리 떴으면 남은 시간만큼 더 보여주고, 이미 지났으면 바로 내린다.
    const remaining = MIN_SPLASH_MS - (Date.now() - LAUNCHED_AT)
    setTimeout(() => {
      SplashScreen.hideAsync().catch(() => {})
    }, Math.max(0, remaining))
  }, [])

  const retry = useCallback(() => {
    setFailed(false)
    webViewRef.current?.reload()
  }, [])

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container} edges={['top', 'left', 'right']}>
        <StatusBar style="dark" />
        {failed ? (
          <ErrorScreen onRetry={retry} />
        ) : (
          <>
            {onProviderPage && <AuthHeader onBack={leaveProviderPage} />}
            <WebView
              ref={webViewRef}
              source={{ uri: WEB_URL }}
              style={styles.webview}
              // ★ 백엔드가 이 값으로 앱을 판별해 리프레시 토큰을 90일로 준다(웹은 14일).
              //    userAgent 로 통째로 덮으면 소셜 로그인 화면이 클라이언트를 판별하지 못한다.
              applicationNameForUserAgent={APP_USER_AGENT}
              // 로그인 유지의 핵심. 쿠키가 앱을 껐다 켜도 남아야 한다.
              sharedCookiesEnabled
              thirdPartyCookiesEnabled
              javaScriptEnabled
              domStorageEnabled
              pullToRefreshEnabled
              // 웹이 자체 로딩 UI 를 갖고 있어 웹뷰 스피너는 띄우지 않는다.
              // 첫 로드까지는 스플래시가 덮고 있다.
              onLoadEnd={hideSplash}
              onNavigationStateChange={handleNavigationStateChange}
              onShouldStartLoadWithRequest={handleShouldStartLoad}
              onError={() => {
                setFailed(true)
                hideSplash()
              }}
              onHttpError={({ nativeEvent }) => {
                // 4xx 는 웹이 스스로 처리한다(401 이면 로그인 화면으로 보낸다).
                // 서버에 닿지 못하는 5xx 만 앱이 대신 안내한다.
                if (nativeEvent.statusCode >= 500) {
                  setFailed(true)
                  hideSplash()
                }
              }}
            />
          </>
        )}
      </SafeAreaView>
    </SafeAreaProvider>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    // 웹의 바깥 배경색(--color-bg-surround)과 맞춰 스크롤 바운스 때 흰 띠가 보이지 않게 한다.
    backgroundColor: '#edf1ea',
  },
  webview: {
    flex: 1,
  },
})
