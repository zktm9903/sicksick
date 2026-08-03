import { useCallback, useState } from 'react'
import { Outlet } from 'react-router'

import { OnboardingGuard } from '@/features/auth/OnboardingGuard'

import type { OnboardingContext } from './context'
import type { OnboardingDraft } from './types'

const STORAGE_KEY = 'sicksick_onboarding_draft'

const EMPTY_DRAFT: OnboardingDraft = {
  nickname: '',
  birthDate: '',
  conditions: [],
  conditionIndex: 0,
  symptomsByCondition: {},
  recentByCondition: {},
  heightCm: '',
  weightKg: '',
  skipped: false,
}

/**
 * 온보딩 8화면이 공유하는 상태.
 *
 * 입력을 마지막에 한 번에 저장하므로 화면 사이에 값을 들고 다녀야 한다. 새 의존성을
 * 들이지 않고 react-router 의 Outlet context 로 해결한다.
 *
 * 새로고침하면 메모리 상태가 사라지는데, 8단계를 다시 채우게 하는 건 과하므로
 * `sessionStorage` 에 백업한다. 탭을 닫으면 사라지는 것이 자연스럽다(localStorage 와 달리
 * 다음 방문까지 남지 않는다).
 */
export function OnboardingLayout() {
  const [draft, setDraft] = useState<OnboardingDraft>(readBackup)

  // 백업 쓰기는 patch·clear 안에서만 한다.
  // draft 변화를 useEffect 로 미러링하면 clear() 직후에 빈 값이 다시 쓰여 백업이 남는다.
  const patch = useCallback<OnboardingContext['patch']>((values) => {
    setDraft((prev) => {
      // 함수 형태면 항상 최신 상태를 넘겨준다. 같은 틱에 여러 번 갱신해도 값이 덮이지 않는다.
      const next = { ...prev, ...(typeof values === 'function' ? values(prev) : values) }
      writeBackup(next)
      return next
    })
  }, [])

  const clear = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY)
    setDraft(EMPTY_DRAFT)
  }, [])

  return (
    <OnboardingGuard>
      <Outlet context={{ draft, patch, clear } satisfies OnboardingContext} />
    </OnboardingGuard>
  )
}

function readBackup(): OnboardingDraft {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return EMPTY_DRAFT
    }
    // 저장 형식이 바뀌었거나 손상됐을 수 있다. 빠진 필드는 기본값으로 채운다.
    return { ...EMPTY_DRAFT, ...(JSON.parse(raw) as Partial<OnboardingDraft>) }
  } catch {
    // 복원 실패는 조용히 처음부터 시작한다. 사용자에게 알릴 만한 일이 아니다.
    return EMPTY_DRAFT
  }
}

function writeBackup(draft: OnboardingDraft) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(draft))
  } catch {
    // 저장 공간이 없거나 사생활 보호 모드일 수 있다. 백업이 없어도 진행은 된다.
  }
}
