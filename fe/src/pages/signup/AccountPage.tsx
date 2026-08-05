import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import { signUpWithPassword } from '@/features/auth/api'
import { SESSION_QUERY_KEY } from '@/features/auth/session'
import { SIGNUP_STEPS } from '@/features/signup/constants'
import { alertError } from '@/lib/alertError'

import styles from './AccountPage.module.css'

/** 서버(`SignUpRequest`)의 `@Size(min = 8)` 과 같은 값이어야 한다. */
const MIN_PASSWORD_LENGTH = 8

/** 최종 판정은 서버가 한다. 여기서는 버튼을 언제 열어줄지만 정한다. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 이메일·비밀번호로 계정 만들기.
 *
 * <p>제출하는 순간 계정이 만들어지고 로그인 상태가 된다. 소셜 가입과 같은 방식이다 —
 * 아직 약관·본인인증 전이라 `PENDING` 이고, 이어서 갈 곳은 서버가 알려준다.
 */
export function AccountPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')

  // 입력 중에는 지적하지 않는다. 두 번째 칸을 치기 시작한 뒤부터 알려준다.
  const mismatch = passwordConfirm.length > 0 && password !== passwordConfirm
  const canSubmit =
    EMAIL_PATTERN.test(email) &&
    password.length >= MIN_PASSWORD_LENGTH &&
    password === passwordConfirm

  const submit = useMutation({
    mutationFn: () => signUpWithPassword(email, password),
    onSuccess: async (result) => {
      // 가입과 동시에 세션이 생겼다. 다시 읽어야 AuthGuard 가 다음 화면을 통과시킨다.
      await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY })
      navigate(result.nextStep, { replace: true })
    },
    onError: (error) => alertError(error, '가입에 실패했어요.'),
  })

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    submit.mutate()
  }

  return (
    <Screen>
      <AppHeader title="회원가입" onBack={() => navigate(ROUTES.login)} />
      <ProgressDots steps={SIGNUP_STEPS} step="auth" label="회원가입 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          씩씩이에서
          <br />
          기록을 시작해보세요
        </h2>

        <form id="signup-account" onSubmit={handleSubmit}>
          <TextField
            className={styles.field}
            type="email"
            label="아이디 (이메일)"
            placeholder="example@email.com"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            className={styles.field}
            type="password"
            label="비밀번호"
            placeholder={`${MIN_PASSWORD_LENGTH}자 이상 입력해주세요`}
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <TextField
            className={styles.field}
            type="password"
            label="비밀번호 확인"
            placeholder="비밀번호를 다시 입력해주세요"
            autoComplete="new-password"
            value={passwordConfirm}
            error={mismatch ? '비밀번호가 일치하지 않아요.' : undefined}
            onChange={(event) => setPasswordConfirm(event.target.value)}
          />
        </form>
      </ScreenBody>

      <ScreenFooter>
        <Button type="submit" form="signup-account" disabled={!canSubmit || submit.isPending}>
          다음
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
