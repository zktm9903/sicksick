import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import { ONBOARDING_STEPS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import { cx } from '@/lib/cx'

import styles from './onboarding.module.css'

export function BasicInfoPage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const valid = draft.heightCm.trim() !== '' || draft.weightKg.trim() !== ''

  // 뒤로 가기는 들어온 경로를 되짚는다. 건너뛰었다면 질환 화면을 거치지 않았다.
  const goBack = () =>
    navigate(draft.skipped ? ROUTES.onboarding.conditionGate : ROUTES.onboarding.recent)

  const goNext = () => navigate(ROUTES.onboarding.done)

  const skip = () => {
    patch({ heightCm: '', weightKg: '' })
    navigate(ROUTES.onboarding.done)
  }

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={goBack} />
      <ProgressDots steps={ONBOARDING_STEPS} step="basic" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={cx(styles.title, styles.titleTight)}>
          상태 확인에 필요한
          <br />
          기본 정보를 알려주세요
        </h2>
        <p className={styles.description}>
          약물 용량, 체중 변화 등을 분석할 때 활용돼요. 지금 입력하지 않아도 괜찮아요.
        </p>

        <label className={styles.fieldLabel} htmlFor="height">
          키 (cm)
        </label>
        <TextField
          id="height"
          placeholder="선택 입력"
          inputMode="numeric"
          value={draft.heightCm}
          onChange={(e) => patch({ heightCm: e.target.value.replace(/\D/g, '').slice(0, 3) })}
        />

        <label className={cx(styles.fieldLabel, styles.description)} htmlFor="weight">
          몸무게 (kg)
        </label>
        <TextField
          id="weight"
          placeholder="선택 입력"
          inputMode="numeric"
          value={draft.weightKg}
          onChange={(e) => patch({ weightKg: e.target.value.replace(/\D/g, '').slice(0, 3) })}
        />
      </ScreenBody>

      <ScreenFooter className={styles.footerStack}>
        <Button disabled={!valid} onClick={goNext}>
          다음
        </Button>
        <Button variant="text" onClick={skip}>
          나중에 입력할게요
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
