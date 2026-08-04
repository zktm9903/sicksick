/**
 * 씩씩이 앱 설정.
 *
 * app.json 이 아니라 코드로 두는 이유: 개발 빌드에만 평문 HTTP 예외를 켜야 하는데
 * JSON 으로는 조건 분기를 할 수 없다. 운영 빌드에 예외가 섞여 나가면 앱 전체가
 * 암호화되지 않은 통신을 허용하게 된다.
 *
 * `.ts` 가 아니라 `.js` 인 이유: eas-cli 에 번들된 설정 로더가 TypeScript 6 을
 * 처리하지 못해 빌드가 시작조차 못 한다(`expo config` 는 되는데 eas-cli 만 실패).
 * 타입은 JSDoc 으로 유지한다.
 *
 *   개발: APP_VARIANT=development npx expo start
 *   운영: eas.json 의 production 프로파일이 값을 명시한다
 */

const isDevelopment = process.env.APP_VARIANT === 'development'

/**
 * 웹뷰가 띄울 주소. 앱에는 화면이 없고 이 웹을 그대로 보여준다.
 *
 * 로컬 웹을 보려면 .env 에 맥의 LAN IP 를 적는다(git 에 올라가지 않는다):
 *   EXPO_PUBLIC_WEB_URL=http://192.168.0.10:5173
 *
 * 실기기에서는 localhost 를 쓸 수 없다 — 폰 자신을 가리킨다.
 * (iOS 시뮬레이터는 맥과 네트워크를 공유해 localhost 가 통한다)
 */
const webUrl = process.env.EXPO_PUBLIC_WEB_URL ?? 'https://sicksick.kr'

/** @type {import('expo/config').ExpoConfig} */
const config = {
  name: '씩씩이',
  slug: 'sicksick',
  owner: 'zktm9903',
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
        // 웹의 바깥 배경색(--color-bg-surround)과 맞춰 첫 화면 전환이 튀지 않게 한다.
        backgroundColor: '#EDF1EA',
        imageWidth: 200,
      },
    ],
  ],

  extra: {
    // 런타임에서 읽는 값. App.tsx 가 이걸로 웹 주소를 정한다.
    webUrl,
    // EAS 프로젝트 연결. 코드 설정 파일에는 eas-cli 가 이 값을 자동으로 써넣지
    // 못하므로(JSON 만 수정 가능) 직접 적는다.
    eas: {
      projectId: '00801e9e-f282-45e9-b5b8-2c27e2b9d5dc',
    },
  },

  updates: {
    url: 'https://u.expo.dev/00801e9e-f282-45e9-b5b8-2c27e2b9d5dc',
  },
  runtimeVersion: {
    policy: 'appVersion',
  },
}

module.exports = config
