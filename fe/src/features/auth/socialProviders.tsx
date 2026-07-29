import type { ReactNode } from 'react'

import { GoogleIcon } from '@/components/icons/GoogleIcon'
import { KakaoIcon } from '@/components/icons/KakaoIcon'
import { NaverIcon } from '@/components/icons/NaverIcon'

export type SocialProviderId = 'kakao' | 'naver' | 'google'

export type SocialProvider = {
  id: SocialProviderId
  /** 사용자에게 보여줄 이름 */
  name: string
  label: string
  icon: ReactNode
}

/** 간편 로그인에 노출되는 순서대로 정의한다. */
export const SOCIAL_PROVIDERS: SocialProvider[] = [
  { id: 'kakao', name: '카카오', label: '카카오로 계속하기', icon: <KakaoIcon /> },
  { id: 'naver', name: '네이버', label: '네이버로 계속하기', icon: <NaverIcon /> },
  { id: 'google', name: 'Google', label: 'Google로 계속하기', icon: <GoogleIcon /> },
]
