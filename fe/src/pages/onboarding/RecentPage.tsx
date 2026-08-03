import { Fragment } from 'react'
import { Navigate, useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import { ONBOARDING_STEPS, RECENT_OPTIONS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import type { RecentOnsetType } from '@/features/onboarding/types'
import { cx } from '@/lib/cx'

import styles from './onboarding.module.css'
import recentStyles from './RecentPage.module.css'

/** 질환별 최근 증상 시점. 증상 화면과 마찬가지로 `conditionIndex` 로 순회한다. */
export function RecentPage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const condition = draft.conditions[draft.conditionIndex]

  if (!condition) {
    return <Navigate to={ROUTES.onboarding.condition} replace />
  }

  const recent = draft.recentByCondition[condition.name]

  // 이전 값에서 파생되는 갱신이라 함수 형태로 넘긴다(같은 틱에 두 번 눌러도 덮이지 않는다).
  const select = (type: RecentOnsetType) =>
    patch((prev) => ({
      recentByCondition: {
        ...prev.recentByCondition,
        // 구간을 다시 고르면 이전에 입력한 날짜는 의미가 없다.
        [condition.name]: {
          type,
          date: type === 'EXACT' ? (prev.recentByCondition[condition.name]?.date ?? null) : null,
        },
      },
    }))

  const setDate = (date: string) =>
    patch((prev) => ({
      recentByCondition: {
        ...prev.recentByCondition,
        [condition.name]: { type: 'EXACT', date },
      },
    }))

  const valid = recent != null && (recent.type !== 'EXACT' || Boolean(recent.date))

  const goNext = () => {
    const isLast = draft.conditionIndex + 1 >= draft.conditions.length
    if (isLast) {
      navigate(ROUTES.onboarding.basic)
      return
    }
    patch({ conditionIndex: draft.conditionIndex + 1 })
    navigate(ROUTES.onboarding.symptoms)
  }

  const total = draft.conditions.length

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={() => navigate(ROUTES.onboarding.symptoms)} />
      <ProgressDots steps={ONBOARDING_STEPS} step="recent" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        {total > 1 && (
          <p className={styles.loopBadge}>
            질환 {draft.conditionIndex + 1}/{total} · {condition.name}
          </p>
        )}

        <h2 className={styles.title}>
          {condition.name}과 관련해 가장 최근
          <br />
          증상이 나타난 때는 언제인가요?
        </h2>

        <div className={recentStyles.options}>
          {RECENT_OPTIONS.map((option) => (
            <Fragment key={option.type}>
              <button
                type="button"
                className={cx(
                  recentStyles.option,
                  recent?.type === option.type && recentStyles.selected,
                )}
                aria-pressed={recent?.type === option.type}
                onClick={() => select(option.type)}
              >
                {option.label}
              </button>
              {option.type === 'EXACT' && recent?.type === 'EXACT' && (
                <TextField
                  autoFocus
                  type="date"
                  aria-label="증상이 나타난 날짜"
                  value={recent.date ?? ''}
                  onChange={(e) => setDate(e.target.value)}
                />
              )}
            </Fragment>
          ))}
        </div>
      </ScreenBody>

      <ScreenFooter>
        <Button disabled={!valid} onClick={goNext}>
          다음
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
