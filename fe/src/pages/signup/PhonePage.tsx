import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { SIGNUP_STEPS } from '@/features/signup/constants'
import { TextField } from '@/components/ui/TextField'
import { VerifyBanner } from '@/components/ui/VerifyBanner'
import { requestPhoneCode } from '@/features/signup/api'
import { alertError } from '@/lib/alertError'

import styles from './PhonePage.module.css'

/** 휴대폰 번호는 숫자 11자리. */
const PHONE_LENGTH = 11

export function PhonePage() {
  const navigate = useNavigate()
  const [phone, setPhone] = useState('')

  const valid = phone.length === PHONE_LENGTH

  const request = useMutation({
    mutationFn: () => requestPhoneCode(phone),
    onSuccess: (result) => {
      // 인증번호 화면은 번호를 알아야 검증 요청을 보낼 수 있다.
      // devCode 는 개발 환경에서만 채워져 온다.
      navigate(ROUTES.signup.otp, { state: { phone, devCode: result.devCode } })
    },
    onError: (error) => alertError(error, '인증번호 요청에 실패했어요.'),
  })

  return (
    <Screen>
      <AppHeader title="본인인증" onBack={() => navigate(ROUTES.signup.terms)} />
      <ProgressDots steps={SIGNUP_STEPS} step="phone" label="회원가입 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          휴대폰 번호를
          <br />
          입력해 주세요
        </h2>

        <label className={styles.fieldLabel} htmlFor="phone">
          휴대폰 번호
        </label>
        <TextField
          id="phone"
          placeholder="01012345678"
          inputMode="numeric"
          autoComplete="tel"
          value={phone}
          // 하이픈·공백을 걸러 서버와 같은 형식으로 맞춘다.
          onChange={(event) =>
            setPhone(event.target.value.replace(/\D/g, '').slice(0, PHONE_LENGTH))
          }
        />

        {valid && <VerifyBanner>확인!</VerifyBanner>}
      </ScreenBody>

      <ScreenFooter>
        <Button disabled={!valid || request.isPending} onClick={() => request.mutate()}>
          인증번호 요청하기
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
