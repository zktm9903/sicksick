import type { ExpoConfig } from 'expo/config'

/**
 * 씩씩이 앱 설정.
 *
 * app.json 대신 이 파일을 쓰는 이유: 개발 빌드에만 평문 HTTP 예외를 켜야 하는데
 * JSON 으로는 조건 분기를 할 수 없다. 운영 빌드에 예외가 섞여 나가면 앱 전체가
 * 암호화되지 않은 통신을 허용하게 된다.
 *
 *   개발: APP_VARIANT=development npx expo start
 *   운영: (기본값)
 */
const isDevelopment = process.env.APP_VARIANT === 'development'

/**
 * 웹뷰가 띄울 주소. 앱에는 화면이 없고 이 웹을 그대로 보여준다.
 *
 * 로컬 웹을 보려면 .env 에 맥의 LAN IP 를 적는다(git 에 올라가지 않는다):
 *   EXPO_PUBLIC_WEB_URL=http://192.168.0.10:5173
 *
 * localhost 는 쓸 수 없다 — 앱에서의 localhost 는 폰 자신이다.
 */
const webUrl = process.env.EXPO_PUBLIC_WEB_URL ?? 'https://sicksick.kr'

const config: ExpoConfig = {
  name: '씩씩이',
  slug: 'sicksick',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  // 웹이 밝은 테마 하나만 쓰므로 시스템 다크모드를 따라가지 않는다.
  userInterfaceStyle: 'light',
  scheme: 'sicksick',

  ios: {
    bundleIdentifier: 'kr.sicksick.app',
    supportsTablet: true,
    infoPlist: {
      // 개발 중 맥의 Vite(HTTP)를 보기 위한 예외. 운영 빌드에는 넣지 않는다.
      // 운영은 Cloudflare 를 통해 HTTPS 로 붙으므로 예외가 필요 없다.
      ...(isDevelopment ? { NSAppTransportSecurity: { NSAllowsLocalNetworking: true } } : {}),
    },
  },

  android: {
    package: 'kr.sicksick.app',
    adaptiveIcon: {
      backgroundColor: '#E8F3EC',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
    // 위와 같은 이유. 개발 빌드에만 평문 HTTP 를 허용한다.
    ...(isDevelopment ? { usesCleartextTraffic: true } : {}),
  },

  web: {
    favicon: './assets/favicon.png',
  },

  plugins: [
    [
      'expo-splash-screen',
      {
        image: './assets/splash-icon.png',
        // 웹의 배경색(--color-bg-surround)과 맞춰 첫 화면 전환이 튀지 않게 한다.
        backgroundColor: '#EDF1EA',
        imageWidth: 200,
      },
    ],
  ],

  // 런타임에서 읽을 값. App.tsx 가 이걸로 웹 주소를 정한다.
  extra: {
    webUrl,
  },
}

export default config
