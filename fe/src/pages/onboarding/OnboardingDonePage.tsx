import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Navigate, useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { Mascot } from '@/components/brand/Mascot'
import { Screen, ScreenBody } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { SESSION_QUERY_KEY } from '@/features/auth/session'
import { completeOnboarding } from '@/features/onboarding/api'
import { recentLabel } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import { alertError } from '@/lib/alertError'

import styles from './OnboardingDonePage.module.css'

/** 배경에 흩뿌리는 조각. 위치·크기·색이 제각각이라 값으로 둔다. */
const CONFETTI = [
  { top: '8%', left: '10%', size: 13, color: 'var(--color-okay)', delay: '0s' },
  { top: '14%', left: '85%', size: 10, color: '#fff', delay: '0.15s' },
  { top: '30%', left: '6%', size: 8, color: 'var(--color-info)', delay: '0.3s' },
  { top: '36%', left: '90%', size: 14, color: 'var(--color-okay)', delay: '0.45s' },
  { top: '5%', left: '50%', size: 8, color: '#fff', delay: '0.6s' },
  { top: '22%', left: '94%', size: 6, color: 'var(--color-green-200)', delay: '0.2s' },
  { top: '42%', left: '18%', size: 7, color: '#fff', delay: '0.5s' },
]

/**
 * 온보딩 완료.
 *
 * 화면이 뜬 시점이 아니라 사용자가 버튼을 누를 때 저장한다. 축하 화면을 보다가
 * 뒤로 가서 값을 고치는 경우가 있어서다.
 */
export function OnboardingDonePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { draft, clear } = useOnboarding()

  const submit = useMutation({
    mutationFn: () => completeOnboarding(draft),
    onSuccess: async (result) => {
      clear()
      // status 가 ACTIVE 로 바뀌었으므로 세션을 다시 읽는다.
      await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY })
      navigate(result.nextStep, { replace: true })
    },
    onError: (error) => alertError(error, '등록에 실패했어요.'),
  })

  // 새로고침으로 입력을 잃으면 저장할 게 없다. 처음 화면으로 되돌린다.
  if (!draft.nickname.trim() || !draft.birthDate) {
    return <Navigate to={ROUTES.onboarding.nickname} replace />
  }

  const name = draft.nickname.trim()

  return (
    <Screen className={styles.screen}>
      {CONFETTI.map((c, index) => (
        <span
          key={c.left + c.top}
          className={styles.confetti}
          aria-hidden="true"
          style={{
            top: c.top,
            left: c.left,
            width: c.size,
            height: c.size,
            borderRadius: index % 2 ? 3 : '50%',
            background: c.color,
            animationDelay: c.delay,
          }}
        />
      ))}

      <ScreenBody className={styles.hero}>
        <div className={styles.mascot}>
          <Mascot size={180} />
        </div>
        <p className={styles.greeting}>안녕하세요 {name}님!!</p>
        <p className={styles.headline}>등록이 완료됐어요!</p>
        <p className={styles.subline}>이제 우리 같이 씩씩해져볼까요?</p>
      </ScreenBody>

      <div className={styles.sheet}>
        <p className={styles.sheetLabel}>내용 정리</p>

        {draft.skipped ? (
          <div className={styles.skipped}>
            <span className={styles.skippedIcon} aria-hidden="true">
              💚
            </span>
            <div>
              <p className={styles.skippedTitle}>생년월일만 등록했어요</p>
              <p className={styles.skippedDescription}>질환은 홈에서 언제든 추가할 수 있어요</p>
            </div>
          </div>
        ) : (
          <div className={styles.summary}>
            <div className={styles.summaryRow}>
              <span className={styles.summaryLabel}>생년월일</span>
              <span className={styles.summaryValue}>{draft.birthDate || '-'}</span>
            </div>

            {draft.conditions.map((condition) => {
              const picked = draft.symptomsByCondition[condition.name] ?? []
              const recent = draft.recentByCondition[condition.name]

              return (
                <div key={condition.name} className={styles.summaryCondition}>
                  <p className={styles.summaryConditionName}>{condition.name}</p>
                  <div className={styles.summaryRow}>
                    <span className={styles.summaryFieldLabel}>경험한 증상</span>
                    <span className={styles.summaryFieldValue}>
                      {picked.map((s) => s.name).join(', ') || '-'}
                    </span>
                  </div>
                  <div className={styles.summaryRow}>
                    <span className={styles.summaryFieldLabel}>최근 증상</span>
                    <span className={styles.summaryFieldValue}>
                      {recent ? recentLabel(recent.type, recent.date) : '-'}
                    </span>
                  </div>
                </div>
              )
            })}
          </div>
        )}

        <div className={styles.action}>
          <Button disabled={submit.isPending} onClick={() => submit.mutate()}>
            오늘의 상태 기록하기
          </Button>
        </div>
      </div>
    </Screen>
  )
}
