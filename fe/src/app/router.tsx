import { createBrowserRouter, Navigate } from 'react-router'

import { LoginPage } from '@/pages/login/LoginPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'

import { RootLayout } from './RootLayout'
import { ROUTES } from './routes'

/**
 * 화면을 하나씩 구현할 때마다 PlaceholderPage 를 실제 페이지로 교체한다.
 * (예: element: <TermsPage /> )
 */
export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <Navigate to={ROUTES.login} replace /> },
      { path: ROUTES.login, element: <LoginPage /> },

      // 회원가입
      { path: ROUTES.signup.account, element: <PlaceholderPage title="계정 만들기" /> },
      { path: ROUTES.signup.terms, element: <PlaceholderPage title="약관 동의" /> },
      { path: ROUTES.signup.phone, element: <PlaceholderPage title="본인인증" /> },
      { path: ROUTES.signup.otp, element: <PlaceholderPage title="인증번호 입력" /> },
      { path: ROUTES.signup.done, element: <PlaceholderPage title="본인인증 완료" /> },

      // 온보딩
      { path: ROUTES.onboarding.nickname, element: <PlaceholderPage title="닉네임 설정" /> },
      {
        path: ROUTES.onboarding.conditionGate,
        element: <PlaceholderPage title="질환 등록 여부" />,
      },
      { path: ROUTES.onboarding.condition, element: <PlaceholderPage title="질환 검색" /> },
      {
        path: ROUTES.onboarding.conditionConfirm,
        element: <PlaceholderPage title="질환 정보 확인" />,
      },
      { path: ROUTES.onboarding.symptoms, element: <PlaceholderPage title="증상 선택" /> },
      { path: ROUTES.onboarding.recent, element: <PlaceholderPage title="최근 증상 시점" /> },
      { path: ROUTES.onboarding.basic, element: <PlaceholderPage title="기본 정보" /> },
      { path: ROUTES.onboarding.done, element: <PlaceholderPage title="등록 완료" /> },

      { path: ROUTES.home, element: <PlaceholderPage title="홈" /> },
      { path: '*', element: <PlaceholderPage title="페이지를 찾을 수 없어요" /> },
    ],
  },
])
