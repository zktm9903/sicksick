import { cx } from '@/lib/cx'

import styles from './ProgressDots.module.css'

/** 회원가입 진행 단계. 프로토타입의 steps 배열과 순서를 맞춘다. */
const SIGNUP_STEPS = ['auth', 'terms', 'phone', 'otp'] as const

export type SignupStep = (typeof SIGNUP_STEPS)[number]

type ProgressDotsProps = {
  step: SignupStep
}

export function ProgressDots({ step }: ProgressDotsProps) {
  const currentIndex = SIGNUP_STEPS.indexOf(step)

  return (
    <div
      className={styles.track}
      role="progressbar"
      aria-valuemin={1}
      aria-valuemax={SIGNUP_STEPS.length}
      aria-valuenow={currentIndex + 1}
      aria-label="회원가입 진행률"
    >
      {SIGNUP_STEPS.map((name, index) => (
        <div
          key={name}
          className={cx(styles.segment, index <= currentIndex && styles.filled)}
        />
      ))}
    </div>
  )
}
