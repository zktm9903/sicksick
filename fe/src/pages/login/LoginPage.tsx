import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { Mascot } from '@/components/brand/Mascot'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { Divider } from '@/components/ui/Divider'
import { TextField } from '@/components/ui/TextField'
import { SocialLoginButton } from '@/features/auth/SocialLoginButton'
import { SOCIAL_PROVIDERS, type SocialProvider } from '@/features/auth/socialProviders'
import { useTestProbe } from '@/features/test/useTestProbe'

import styles from './LoginPage.module.css'

export function LoginPage() {
  useTestProbe()

  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [notFound, setNotFound] = useState(false)

  const canSubmit = email.trim().length > 0 && password.length > 0

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    // TODO: 로그인 API 연동. 현재는 매칭되는 계정이 없는 상황을 가정해 회원가입을 유도한다.
    setNotFound(true)
  }

  // 간편 로그인 — 매칭되는 계정이 없으면 소셜 계정 정보로 바로 약관 동의부터 진행한다.
  const handleSocialLogin = (provider: SocialProvider) => {
    navigate(ROUTES.signup.terms, { state: { provider: provider.id } })
  }

  const goToSignup = () => navigate(ROUTES.signup.account)

  return (
    <Screen>
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>
          씩씩해질
          <br />
          시간이에요
        </h1>
        <div className={styles.heroMascot}>
          <Mascot size={112} />
        </div>
        <p className={styles.heroCaption}>내 증상, 씩씩이와 차곡차곡</p>
      </div>

      <ScreenBody className={styles.body}>
        <form className={styles.form} onSubmit={handleSubmit}>
          <TextField
            type="email"
            placeholder="아이디 (이메일)"
            autoComplete="username"
            aria-label="아이디 (이메일)"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              setNotFound(false)
            }}
          />
          <TextField
            type="password"
            placeholder="비밀번호"
            autoComplete="current-password"
            aria-label="비밀번호"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              setNotFound(false)
            }}
          />
          <Button className={styles.submit} type="submit" disabled={!canSubmit}>
            로그인
          </Button>

          {notFound && (
            <div className={styles.notice} role="status">
              <p className={styles.noticeText}>
                가입된 계정이 없어요.
                <br />
                지금 바로 회원가입해 보세요!
              </p>
              <Button className={styles.noticeAction} size="md" onClick={goToSignup}>
                회원가입
              </Button>
            </div>
          )}
        </form>

        <Divider className={styles.divider}>간편 로그인</Divider>

        <div className={styles.socialList}>
          {SOCIAL_PROVIDERS.map((provider) => (
            <SocialLoginButton
              key={provider.id}
              provider={provider}
              onClick={handleSocialLogin}
            />
          ))}
        </div>
      </ScreenBody>

      <ScreenFooter className={styles.footer}>
        <Button variant="text" onClick={goToSignup}>
          지금 바로 가입하기!
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
