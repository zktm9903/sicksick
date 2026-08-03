import { apiGet, apiPost } from '@/lib/api'

import type { Condition, OnboardingDraft, Symptom } from './types'
import { EXCLUSIVE_SYMPTOMS } from './constants'

export function searchSymptoms(query: string) {
  return apiGet<Symptom[]>(`/api/v1/symptoms?query=${encodeURIComponent(query)}`)
}

export function searchConditions(query: string) {
  return apiGet<Condition[]>(`/api/v1/conditions?query=${encodeURIComponent(query)}`)
}

/** 온보딩 입력 전체를 서버 형식으로 바꿔 한 번에 저장한다. */
export function completeOnboarding(draft: OnboardingDraft) {
  return apiPost<{ nextStep: string }>('/api/v1/onboarding/complete', {
    nickname: draft.nickname.trim(),
    birthDate: draft.birthDate,
    heightCm: toNumber(draft.heightCm),
    weightKg: toNumber(draft.weightKg),
    conditions: draft.conditions.map((condition) => {
      const picked = draft.symptomsByCondition[condition.name] ?? []
      const recent = draft.recentByCondition[condition.name]

      return {
        conditionId: condition.conditionId,
        // 마스터 질환이면 이름·코드·설명을 서버가 이미 안다. 중복 저장하지 않는다.
        customName: condition.conditionId === null ? condition.name : null,
        customCode: condition.conditionId === null ? condition.code : null,
        customDescription: condition.conditionId === null ? condition.description : null,
        status: condition.status,
        // "경험한 증상이 없어요" 같은 배타 선택지는 증상이 아니라 응답이므로 보내지 않는다.
        symptoms: picked
          .filter((s) => !EXCLUSIVE_SYMPTOMS.includes(s.name))
          .map((s) => ({ symptomId: s.id, customName: s.id === null ? s.name : null })),
        recentOnsetType: recent?.type ?? null,
        recentOnsetDate: recent?.type === 'EXACT' ? recent.date : null,
      }
    }),
  })
}

/** 빈 문자열은 "입력하지 않음"이므로 null 로 보낸다. */
function toNumber(value: string): number | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : Number(trimmed)
}
