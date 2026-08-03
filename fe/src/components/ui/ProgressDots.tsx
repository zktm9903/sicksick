import { cx } from '@/lib/cx'

import styles from './ProgressDots.module.css'

type ProgressDotsProps = {
  /** 전체 단계 목록. 화면 순서와 같아야 한다. */
  steps: readonly string[]
  /** 지금 단계. `steps` 에 없으면 아무것도 채우지 않는다. */
  step: string
  /** 스크린리더가 읽을 이름. 회원가입·온보딩을 구분한다. */
  label: string
}

/** 균등 너비 막대 진행바. 지나온 단계까지 초록으로 채운다. */
export function ProgressDots({ steps, step, label }: ProgressDotsProps) {
  const currentIndex = steps.indexOf(step)

  return (
    <div
      className={styles.track}
      role="progressbar"
      aria-valuemin={1}
      aria-valuemax={steps.length}
      aria-valuenow={currentIndex + 1}
      aria-label={label}
    >
      {steps.map((name, index) => (
        <div
          key={name}
          className={cx(styles.segment, index <= currentIndex && styles.filled)}
        />
      ))}
    </div>
  )
}
