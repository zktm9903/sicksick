import { useOutletContext } from 'react-router'

import type { OnboardingDraft } from './types'

export type OnboardingContext = {
  draft: OnboardingDraft
  /**
   * 일부 필드만 갱신한다.
   *
   * 이전 값에서 파생되는 갱신(목록에 추가·토글 등)은 반드시 함수 형태로 넘겨야 한다.
   * 객체를 넘기면 렌더 시점의 draft 를 읽으므로, 같은 틱에 두 번 갱신하면 앞의 것이
   * 덮인다(칩을 빠르게 두 번 누르면 첫 선택이 사라진다).
   */
  patch: (
    values: Partial<OnboardingDraft> | ((prev: OnboardingDraft) => Partial<OnboardingDraft>),
  ) => void
  /** 저장을 마친 뒤 백업을 지운다. */
  clear: () => void
}

/**
 * 온보딩 화면들이 공유하는 입력 상태.
 *
 * `OnboardingLayout` 이 Outlet context 로 내려준 값을 읽는다. 레이아웃 밖에서 부르면
 * react-router 가 undefined 를 주므로 온보딩 라우트 안에서만 쓴다.
 */
export function useOnboarding() {
  return useOutletContext<OnboardingContext>()
}
