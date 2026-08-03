import type { RecentOnsetType } from './types'

/**
 * 최근 증상 시점 선택지.
 *
 * 화면 문구일 뿐 다른 데이터가 참조하지 않으므로 DB 가 아니라 코드에 둔다.
 * 값(`type`)은 서버의 `RecentOnsetType` 과 짝을 이룬다.
 */
export const RECENT_OPTIONS: { type: RecentOnsetType; label: string }[] = [
  { type: 'EXACT', label: '정확한 날짜' },
  { type: 'D7', label: '최근 7일 이내' },
  { type: 'D30', label: '최근 8~30일' },
  { type: 'M3', label: '최근 1~3개월' },
  { type: 'OLD', label: '3개월보다 오래전' },
  { type: 'UNKNOWN', label: '잘 기억나지 않아요' },
]

export function recentLabel(type: RecentOnsetType, date: string | null): string {
  if (type === 'EXACT' && date) {
    const d = new Date(date)
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
  }
  return RECENT_OPTIONS.find((o) => o.type === type)?.label ?? ''
}

/**
 * 증상 선택 화면의 배타 선택지.
 *
 * 이 둘은 다른 증상과 함께 고를 수 없다. "없어요" 를 고르면 시점 화면도 건너뛴다 —
 * 없는 증상의 시점을 물을 수 없기 때문이다.
 */
export const NO_SYMPTOM = '경험한 증상이 없어요'
export const UNKNOWN_SYMPTOM = '잘 기억나지 않아요'
export const EXCLUSIVE_SYMPTOMS = [NO_SYMPTOM, UNKNOWN_SYMPTOM]

/** 온보딩 진행바 단계. 화면 순서와 같아야 한다. */
export const ONBOARDING_STEPS = [
  'nickname',
  'gate',
  'condition',
  'confirm',
  'symptoms',
  'recent',
  'basic',
] as const

export type OnboardingStep = (typeof ONBOARDING_STEPS)[number]
