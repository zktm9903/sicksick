import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { ONBOARDING_STEPS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'

import styles from './onboarding.module.css'

export function ConditionConfirmPage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const goToSymptoms = () => {
    // 증상·시점 화면은 질환을 하나씩 순회한다. 항상 처음부터 시작한다.
    patch({ conditionIndex: 0 })
    navigate(ROUTES.onboarding.symptoms)
  }

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={() => navigate(ROUTES.onboarding.condition)} />
      <ProgressDots steps={ONBOARDING_STEPS} step="confirm" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          질환 정보를
          <br />
          확인해 주세요
        </h2>

        <div className={styles.cardList}>
          {draft.conditions.map((condition) => (
            <div key={condition.name} className={styles.card}>
              <p className={styles.cardTitle}>{condition.name}</p>
              <div className={styles.cardMeta}>
                <div>질병 분류 코드: {condition.code || '-'}</div>
                <div>내용: {condition.description || '-'}</div>
              </div>

              <div className={styles.cardSection}>
                <p className={styles.cardSectionLabel}>주요 증상</p>
                {condition.suggestedSymptoms.length > 0 ? (
                  <div className={styles.tagList}>
                    {condition.suggestedSymptoms.map((s) => (
                      <span key={s.id} className={styles.tag}>
                        {s.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className={styles.cardMeta}>없음</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </ScreenBody>

      <ScreenFooter className={styles.footerStack}>
        <Button onClick={goToSymptoms}>다음</Button>
        <Button variant="text" onClick={() => navigate(ROUTES.onboarding.condition)}>
          다시 선택하기
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
