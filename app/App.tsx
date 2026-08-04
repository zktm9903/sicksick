import { useCallback, useEffect, useRef, useState } from 'react'
import { BackHandler, Linking, Platform, StyleSheet } from 'react-native'
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context'
import { StatusBar } from 'expo-status-bar'
import * as SplashScreen from 'expo-splash-screen'
import { WebView, type WebViewNavigation } from 'react-native-webview'

import { APP_USER_AGENT, WEB_URL, isInternalUrl } from './src/config'
import { ErrorScreen } from './src/ErrorScreen'

// 웹이 그려질 때까지 스플래시를 유지한다. 이게 없으면 흰 화면이 먼저 보인다.
SplashScreen.preventAutoHideAsync().catch(() => {
  // 이미 숨겨졌거나 지원되지 않는 환경. 표시 타이밍만 달라질 뿐 동작에는 지장이 없다.
})

export default function App() {
  const webViewRef = useRef<WebView>(null)
  const [failed, setFailed] = useState(false)

  /**
   * 안드로이드 하드웨어 뒤로가기.
   *
   * 기본 동작은 앱 종료다. 그대로 두면 가입 도중 뒤로 눌렀을 때 앱이 꺼진다.
   * 웹 히스토리가 있으면 그쪽을 먼저 되짚는다.
   *
   * 렌더마다 리스너를 다시 붙이지 않으려고 state 가 아니라 ref 에 담는다.
   */
  const canGoBack = useRef(false)

  useEffect(() => {
    if (Platform.OS !== 'android') {
      return
    }
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (canGoBack.current) {
        webViewRef.current?.goBack()
        return true
      }
      return false // 첫 화면이면 기본 동작(앱 종료)에 맡긴다.
    })
    return () => subscription.remove()
  }, [])

  const handleNavigationStateChange = useCallback((state: WebViewNavigation) => {
    canGoBack.current = state.canGoBack
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

  const hideSplash = useCallback(() => {
    SplashScreen.hideAsync().catch(() => {})
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
