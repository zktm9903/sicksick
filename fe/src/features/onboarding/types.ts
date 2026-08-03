/** 서버가 내려주는 증상 요약. */
export type SymptomSummary = {
  id: number
  name: string
}

/** 증상 검색 결과. */
export type Symptom = SymptomSummary & {
  /** 사용자가 검색할 법한 일상 표현. 없을 수 있다. */
  nameKo: string | null
  description: string | null
  category: string
  detailLocation: string | null
}

/** 질환 검색 결과. */
export type Condition = {
  id: number
  name: string
  code: string | null
  description: string | null
  /** 이 질환의 주요 증상. 증상 선택 화면의 후보로 쓴다. */
  symptoms: SymptomSummary[]
}

export type ConditionStatus = 'DIAGNOSED' | 'OBSERVING'

export type RecentOnsetType = 'EXACT' | 'D7' | 'D30' | 'M3' | 'OLD' | 'UNKNOWN'

/**
 * 사용자가 고른 증상 하나.
 *
 * 마스터 증상이면 `id` 가, 직접 입력한 증상이면 `id: null` 과 이름이 들어간다.
 */
export type PickedSymptom = {
  id: number | null
  name: string
}

/**
 * 온보딩 중 화면들이 함께 다루는 질환 한 건.
 *
 * `conditionId` 가 null 이면 마스터에 없어 사용자가 직접 등록한 질환이다.
 */
export type DraftCondition = {
  conditionId: number | null
  name: string
  code: string | null
  description: string | null
  status: ConditionStatus
  /** 마스터 질환의 주요 증상. 증상 선택 화면의 후보 목록이 된다. */
  suggestedSymptoms: SymptomSummary[]
}

export type RecentOnset = {
  type: RecentOnsetType
  /** `EXACT` 일 때만 채워진다. */
  date: string | null
}

/** 온보딩 전 구간에서 모으는 입력값. 마지막에 한 번에 서버로 보낸다. */
export type OnboardingDraft = {
  nickname: string
  birthDate: string
  conditions: DraftCondition[]
  /** 증상·시점 화면이 지금 몇 번째 질환을 다루는지. */
  conditionIndex: number
  /** 질환 이름 → 고른 증상들. */
  symptomsByCondition: Record<string, PickedSymptom[]>
  /** 질환 이름 → 최근 증상 시점. */
  recentByCondition: Record<string, RecentOnset>
  heightCm: string
  weightKg: string
  /** "나중에 등록할게요" 로 질환 등록을 건너뛰었는지. */
  skipped: boolean
}
