import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody } from '@/components/layout/Screen'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { ONBOARDING_STEPS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import { cx } from '@/lib/cx'

import styles from './onboarding.module.css'
import gateStyles from './ConditionGatePage.module.css'

export function ConditionGatePage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const registerNow = () => {
    patch({ skipped: false })
    navigate(ROUTES.onboarding.condition)
  }

  // 건너뛰면 질환·증상 화면을 통째로 지나 기본 정보로 간다.
  // 앞서 고른 값이 남아 있으면 완료 화면 요약이 어긋나므로 함께 비운다.
  const registerLater = () => {
    patch({
      skipped: true,
      conditions: [],
      conditionIndex: 0,
      symptomsByCondition: {},
      recentByCondition: {},
    })
    navigate(ROUTES.onboarding.basic)
  }

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={() => navigate(ROUTES.onboarding.nickname)} />
      <ProgressDots steps={ONBOARDING_STEPS} step="gate" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={cx(styles.title, styles.titleTight)}>
          {draft.nickname.trim() ? `${draft.nickname.trim()}님, ` : ''}질환 정보를
          <br />
          지금 등록할까요?
        </h2>
        <p className={styles.description}>
          등록하면 증상 목록과 기록 화면이 질환에 맞게 맞춤화돼요. 나중에 언제든 프로필에서
          등록할 수 있어요.
        </p>

        <button
          type="button"
          className={cx(gateStyles.option, gateStyles.recommended)}
          onClick={registerNow}
        >
          <span className={gateStyles.optionTitle}>지금 등록할게요</span>
          <span className={gateStyles.optionDescription}>질환과 증상을 검색해서 등록해요</span>
        </button>

        <button type="button" className={gateStyles.option} onClick={registerLater}>
          <span className={gateStyles.optionTitle}>나중에 등록할게요</span>
          <span className={gateStyles.optionDescription}>생년월일만 먼저 저장하고 시작해요</span>
        </button>
      </ScreenBody>
    </Screen>
  )
}
