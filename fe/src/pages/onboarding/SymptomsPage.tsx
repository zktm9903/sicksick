import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { Chip } from '@/components/ui/Chip'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import {
  EXCLUSIVE_SYMPTOMS,
  NO_SYMPTOM,
  ONBOARDING_STEPS,
  UNKNOWN_SYMPTOM,
} from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import type { PickedSymptom } from '@/features/onboarding/types'
import { cx } from '@/lib/cx'

import styles from './onboarding.module.css'
import symptomStyles from './SymptomsPage.module.css'

/**
 * 질환별 경험 증상 선택.
 *
 * 질환 수만큼 반복하지만 URL 은 하나다. 어느 질환을 다루는지는 컨텍스트의
 * `conditionIndex` 가 정한다(프로토타입과 같은 방식).
 */
export function SymptomsPage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()
  const [custom, setCustom] = useState('')

  const condition = draft.conditions[draft.conditionIndex]

  // 새로고침으로 질환 목록을 잃으면 순회할 대상이 없다. 선택 화면으로 되돌린다.
  if (!condition) {
    return <Navigate to={ROUTES.onboarding.condition} replace />
  }

  const picked = draft.symptomsByCondition[condition.name] ?? []
  const isPicked = (name: string) => picked.some((s) => s.name === name)

  /**
   * 앞선 선택에서 파생되는 갱신이라 함수 형태로 넘긴다.
   * 객체로 넘기면 칩을 빠르게 두 번 눌렀을 때 첫 선택이 덮인다.
   */
  const toggle = (symptom: PickedSymptom) =>
    patch((prev) => {
      const current = prev.symptomsByCondition[condition.name] ?? []

      // "없어요"·"기억 안 나요"는 다른 증상과 함께 고를 수 없다.
      const next = EXCLUSIVE_SYMPTOMS.includes(symptom.name)
        ? current.some((s) => s.name === symptom.name)
          ? []
          : [symptom]
        : (() => {
            const withoutExclusive = current.filter(
              (s) => !EXCLUSIVE_SYMPTOMS.includes(s.name),
            )
            return withoutExclusive.some((s) => s.name === symptom.name)
              ? withoutExclusive.filter((s) => s.name !== symptom.name)
              : [...withoutExclusive, symptom]
          })()

      return { symptomsByCondition: { ...prev.symptomsByCondition, [condition.name]: next } }
    })

  const addCustom = () => {
    const name = custom.trim()
    if (name && !isPicked(name)) {
      toggle({ id: null, name })
    }
    setCustom('')
  }

  // 후보에 없는데 고른 것들(직접 입력)도 칩으로 보여준다.
  const extras = picked.filter(
    (s) =>
      !condition.suggestedSymptoms.some((c) => c.name === s.name) &&
      !EXCLUSIVE_SYMPTOMS.includes(s.name),
  )

  const goBack = () => {
    if (draft.conditionIndex === 0) {
      navigate(ROUTES.onboarding.conditionConfirm)
      return
    }
    // 앞 질환의 시점 화면으로 되돌아간다.
    patch({ conditionIndex: draft.conditionIndex - 1 })
    navigate(ROUTES.onboarding.recent)
  }

  const goNext = () => {
    // 증상이 없다고 답한 질환은 시점을 물을 수 없다. 다음 질환이나 기본 정보로 넘긴다.
    if (picked.some((s) => s.name === NO_SYMPTOM)) {
      const isLast = draft.conditionIndex + 1 >= draft.conditions.length
      if (isLast) {
        navigate(ROUTES.onboarding.basic)
      } else {
        patch({ conditionIndex: draft.conditionIndex + 1 })
        navigate(ROUTES.onboarding.symptoms)
      }
      return
    }
    navigate(ROUTES.onboarding.recent)
  }

  const total = draft.conditions.length

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={goBack} />
      <ProgressDots steps={ONBOARDING_STEPS} step="symptoms" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        {total > 1 && (
          <p className={styles.loopBadge}>
            질환 {draft.conditionIndex + 1}/{total} · {condition.name}
          </p>
        )}

        <h2 className={cx(styles.title, styles.titleTight)}>
          {condition.name}과 관련해 이전에
          <br />
          경험한 증상을 선택해 주세요
        </h2>
        <p className={styles.description}>
          현재는 없지만 이전에 경험한 증상도 선택해 주세요. 여러 개 선택할 수 있어요.
        </p>

        <div className={styles.chipRow}>
          {condition.suggestedSymptoms.map((s) => (
            <Chip
              key={s.id}
              label={s.name}
              active={isPicked(s.name)}
              onClick={() => toggle({ id: s.id, name: s.name })}
            />
          ))}
          {extras.map((s) => (
            <Chip key={s.name} label={s.name} active onClick={() => toggle(s)} />
          ))}
        </div>

        <div className={symptomStyles.customRow}>
          <TextField
            className={symptomStyles.customInput}
            placeholder="직접 입력"
            value={custom}
            onChange={(e) => setCustom(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addCustom()
              }
            }}
          />
          <button type="button" className={symptomStyles.addButton} onClick={addCustom}>
            추가
          </button>
        </div>

        <div className={symptomStyles.exclusiveList}>
          {[NO_SYMPTOM, UNKNOWN_SYMPTOM].map((name) => (
            <Chip
              key={name}
              label={name}
              block
              active={isPicked(name)}
              onClick={() => toggle({ id: null, name })}
            />
          ))}
        </div>
      </ScreenBody>

      <ScreenFooter>
        <Button disabled={picked.length === 0} onClick={goNext}>
          다음
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
