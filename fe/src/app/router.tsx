import { createBrowserRouter, Navigate } from 'react-router'

import { AuthGuard } from '@/features/auth/AuthGuard'
import { OnboardingLayout } from '@/features/onboarding/OnboardingLayout'
import { ErrorPage } from '@/pages/ErrorPage'
import { LoginPage } from '@/pages/login/LoginPage'
import { BasicInfoPage } from '@/pages/onboarding/BasicInfoPage'
import { ConditionConfirmPage } from '@/pages/onboarding/ConditionConfirmPage'
import { ConditionGatePage } from '@/pages/onboarding/ConditionGatePage'
import { ConditionPage } from '@/pages/onboarding/ConditionPage'
import { NicknamePage } from '@/pages/onboarding/NicknamePage'
import { OnboardingDonePage } from '@/pages/onboarding/OnboardingDonePage'
import { RecentPage } from '@/pages/onboarding/RecentPage'
import { SymptomsPage } from '@/pages/onboarding/SymptomsPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { DonePage } from '@/pages/signup/DonePage'
import { OtpPage } from '@/pages/signup/OtpPage'
import { PhonePage } from '@/pages/signup/PhonePage'
import { TermsPage } from '@/pages/signup/TermsPage'

import { RootLayout } from './RootLayout'
import { ROUTES } from './routes'

/**
 * 화면을 하나씩 구현할 때마다 PlaceholderPage 를 실제 페이지로 교체한다.
 * (예: element: <TermsPage /> )
 */
export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    // 하위 화면에서 렌더 중 예외가 나면 개발자용 오류 화면 대신 이걸 보여준다.
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Navigate to={ROUTES.login} replace /> },
      { path: ROUTES.login, element: <LoginPage /> },

      // 회원가입 — 소셜 인증을 마친(PENDING) 유저만 진입한다.
      // AuthGuard 가 서버의 nextStep 과 경로를 대조해 어긋나면 제자리로 돌려보낸다.
      { path: ROUTES.signup.account, element: <PlaceholderPage title="계정 만들기" /> },
      {
        path: ROUTES.signup.terms,
        element: (
          <AuthGuard>
            <TermsPage />
          </AuthGuard>
        ),
      },
      {
        path: ROUTES.signup.phone,
        element: (
          <AuthGuard>
            <PhonePage />
          </AuthGuard>
        ),
      },
      {
        // 인증번호 화면은 서버 기준으로는 아직 'phone' 단계라 단계 검사를 건너뛴다.
        path: ROUTES.signup.otp,
        element: (
          <AuthGuard allowAnyStep>
            <OtpPage />
          </AuthGuard>
        ),
      },
      {
        // 완료 화면은 이미 다음 단계(온보딩)로 넘어간 상태에서 보여주는 화면이다.
        path: ROUTES.signup.done,
        element: (
          <AuthGuard allowAnyStep>
            <DonePage />
          </AuthGuard>
        ),
      },

      // 온보딩 — 8화면이 입력을 공유하고 마지막에 한 번에 저장한다.
      // OnboardingLayout 이 그 상태를 들고 있고, 진입 자격도 거기서 검사한다
      // (AuthGuard 는 nextStep 과 경로가 정확히 같아야 통과시켜 여기 쓸 수 없다).
      {
        element: <OnboardingLayout />,
        children: [
          { path: ROUTES.onboarding.nickname, element: <NicknamePage /> },
          { path: ROUTES.onboarding.conditionGate, element: <ConditionGatePage /> },
          { path: ROUTES.onboarding.condition, element: <ConditionPage /> },
          { path: ROUTES.onboarding.conditionConfirm, element: <ConditionConfirmPage /> },
          { path: ROUTES.onboarding.symptoms, element: <SymptomsPage /> },
          { path: ROUTES.onboarding.recent, element: <RecentPage /> },
          { path: ROUTES.onboarding.basic, element: <BasicInfoPage /> },
          { path: ROUTES.onboarding.done, element: <OnboardingDonePage /> },
        ],
      },

      { path: ROUTES.home, element: <PlaceholderPage title="홈" /> },
      { path: '*', element: <PlaceholderPage title="페이지를 찾을 수 없어요" /> },
    ],
  },
])
