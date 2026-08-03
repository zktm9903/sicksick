import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import { ONBOARDING_STEPS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'

import styles from './onboarding.module.css'
import nicknameStyles from './NicknamePage.module.css'

const NICKNAME_MAX = 12

export function NicknamePage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const valid = draft.nickname.trim().length > 0 && draft.birthDate !== ''

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={() => navigate(ROUTES.signup.done)} />
      <ProgressDots steps={ONBOARDING_STEPS} step="nickname" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>뭐라고 불러드릴까요?</h2>

        <label className={styles.fieldLabel} htmlFor="nickname">
          닉네임
        </label>
        <TextField
          id="nickname"
          placeholder="닉네임을 입력해주세요"
          value={draft.nickname}
          onChange={(event) => patch({ nickname: event.target.value.slice(0, NICKNAME_MAX) })}
        />
        {draft.nickname.trim() && (
          <p className={nicknameStyles.greeting}>
            씩씩이가 앞으로 &lsquo;{draft.nickname.trim()}&rsquo;님이라고 불러드릴게요!
          </p>
        )}

        <div className={nicknameStyles.birthField}>
          <label className={styles.fieldLabel} htmlFor="birth">
            생년월일
          </label>
          <TextField
            id="birth"
            type="date"
            value={draft.birthDate}
            onChange={(event) => patch({ birthDate: event.target.value })}
          />
        </div>
      </ScreenBody>

      <ScreenFooter>
        <Button disabled={!valid} onClick={() => navigate(ROUTES.onboarding.conditionGate)}>
          다음
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
