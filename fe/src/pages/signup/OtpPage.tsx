import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Navigate, useLocation, useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { SIGNUP_STEPS } from '@/features/signup/constants'
import { TextField } from '@/components/ui/TextField'
import { VerifyBanner } from '@/components/ui/VerifyBanner'
import { SESSION_QUERY_KEY } from '@/features/auth/session'
import { requestPhoneCode, verifyPhoneCode } from '@/features/signup/api'
import { alertError } from '@/lib/alertError'

import styles from './PhonePage.module.css'

const CODE_LENGTH = 6

/** 서버의 인증번호 만료(3분)와 맞춘다. */
const COUNTDOWN_SECONDS = 180

type OtpLocationState = {
  phone?: string
  devCode?: string | null
}

export function OtpPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { state } = useLocation() as { state: OtpLocationState | null }

  const phone = state?.phone
  const [code, setCode] = useState('')
  const [devCode, setDevCode] = useState(state?.devCode ?? null)
  const [seconds, setSeconds] = useState(COUNTDOWN_SECONDS)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (seconds <= 0) {
      return
    }
    const timer = setInterval(() => setSeconds((value) => value - 1), 1000)
    return () => clearInterval(timer)
  }, [seconds])

  const resend = useMutation({
    mutationFn: () => requestPhoneCode(phone!),
    onSuccess: (result) => {
      setDevCode(result.devCode)
      setCode('')
      setSeconds(COUNTDOWN_SECONDS)
      inputRef.current?.focus()
    },
    onError: (error) => alertError(error, '인증번호 재요청에 실패했어요.'),
  })

  const verify = useMutation({
    mutationFn: () => verifyPhoneCode(phone!, code),
    onSuccess: async () => {
      // 본인인증이 끝나면 nextStep 이 온보딩으로 넘어간다.
      await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY })
      navigate(ROUTES.signup.done)
    },
    onError: (error) => alertError(error, '인증에 실패했어요.'),
  })

  // 새로고침 등으로 번호를 잃으면 검증을 보낼 수 없다. 입력 화면으로 되돌린다.
  if (!phone) {
    return <Navigate to={ROUTES.signup.phone} replace />
  }

  const complete = code.length === CODE_LENGTH
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')

  return (
    <Screen>
      <AppHeader title="본인인증" onBack={() => navigate(ROUTES.signup.phone)} />
      <ProgressDots steps={SIGNUP_STEPS} step="otp" label="회원가입 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          문자로 받은
          <br />
          인증번호를 입력해 주세요
        </h2>

        <label className={styles.fieldLabel} htmlFor="otp">
          인증번호
        </label>
        <div className={styles.codeWrap}>
          <TextField
            id="otp"
            ref={inputRef}
            placeholder="6자리 입력"
            inputMode="numeric"
            autoComplete="one-time-code"
            value={code}
            onChange={(event) =>
              setCode(event.target.value.replace(/\D/g, '').slice(0, CODE_LENGTH))
            }
          />
          <button
            type="button"
            className={styles.resend}
            onClick={() => resend.mutate()}
            disabled={resend.isPending}
          >
            재요청
          </button>
        </div>

        <div className={styles.meta}>
          <span className={seconds > 0 ? styles.countdown : styles.countdownExpired}>
            {minutes}:{remainder}
          </span>
          <span className={styles.hint}>문자가 오지 않나요?</span>
        </div>

        {/* 실제 SMS 발송을 붙이기 전까지 서버가 인증번호를 함께 내려준다.
            발송이 붙으면 devCode 가 null 이 되어 이 안내는 자동으로 사라진다. */}
        {devCode && (
          <p className={styles.devCode}>
            문자 발송은 준비 중이에요. 인증번호는 <strong>{devCode}</strong> 입니다.
          </p>
        )}

        {complete && <VerifyBanner>확인!</VerifyBanner>}
      </ScreenBody>

      <ScreenFooter>
        <Button disabled={!complete || verify.isPending} onClick={() => verify.mutate()}>
          인증하기
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
