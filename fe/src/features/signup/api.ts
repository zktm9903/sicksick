import { apiGet, apiPost } from '@/lib/api'

export type Term = {
  code: string
  title: string
  required: boolean
  displayOrder: number
}

type StepResponse = { nextStep: string }

export function fetchTerms() {
  return apiGet<Term[]>('/api/v1/terms')
}

export function agreeTerms(agreements: Record<string, boolean>) {
  return apiPost<StepResponse>('/api/v1/signup/terms', { agreements })
}

/** 개발 환경에서는 devCode 로 인증번호가 그대로 내려온다. 운영에서는 항상 null. */
export function requestPhoneCode(phone: string) {
  return apiPost<{ devCode: string | null }>('/api/v1/signup/phone/code', { phone })
}

export function verifyPhoneCode(phone: string, code: string) {
  return apiPost<StepResponse>('/api/v1/signup/phone/verify', { phone, code })
}
