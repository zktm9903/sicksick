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
  /**
   * 백엔드에 해당 프로바이더가 구현돼 있는지.
   *
   * 구글은 임베디드 웹뷰에서의 OAuth 를 정책적으로 차단해서(disallowed_useragent)
   * 시스템 브라우저 + 딥링크 브릿지가 필요하다. 그 작업 전까지는 눌러도 안내만 띄운다.
   */
  enabled: boolean
}

/** 간편 로그인에 노출되는 순서대로 정의한다. */
export const SOCIAL_PROVIDERS: SocialProvider[] = [
  { id: 'kakao', name: '카카오', label: '카카오로 계속하기', icon: <KakaoIcon />, enabled: true },
  { id: 'naver', name: '네이버', label: '네이버로 계속하기', icon: <NaverIcon />, enabled: true },
  { id: 'google', name: 'Google', label: 'Google로 계속하기', icon: <GoogleIcon />, enabled: false },
]
