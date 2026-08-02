import type { ReactNode } from 'react'

import { cx } from '@/lib/cx'

import styles from './Checkbox.module.css'

type CheckboxProps = {
  checked: boolean
  onChange: (checked: boolean) => void
  children: ReactNode
  /** 테두리 상자로 강조한다(전체 동의 행). */
  boxed?: boolean
  /** 아래쪽 구분선을 그린다(약관 목록 행). */
  divided?: boolean
  /** 라벨을 굵게. */
  strong?: boolean
  /**
   * 행 오른쪽 끝에 붙일 요소(예: 약관 보기 화살표).
   *
   * children 으로 넘기면 라벨 span 안에 들어가 텍스트와 함께 줄바꿈된다.
   * 행의 플렉스 항목으로 놓아야 오른쪽 끝에 고정된다.
   */
  trailing?: ReactNode
  className?: string
}

export function Checkbox({
  checked,
  onChange,
  children,
  boxed = false,
  divided = false,
  strong = false,
  trailing,
  className,
}: CheckboxProps) {
  return (
    <label
      className={cx(styles.row, boxed && styles.boxed, divided && styles.divided, className)}
    >
      <input
        type="checkbox"
        className={styles.input}
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span className={styles.box} aria-hidden="true">
        <svg width="11" height="9" viewBox="0 0 12 10">
          <path
            d="M1 5l3.5 3.5L11 1"
            stroke="currentColor"
            strokeWidth="2"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      <span className={cx(styles.label, strong && styles.labelStrong)}>{children}</span>
      {trailing}
    </label>
  )
}

/** 약관 행 끝의 '보기' 화살표. 아직 약관 본문 화면이 없어 표시만 한다. */
export function CheckboxChevron() {
  return (
    <svg width="7" height="12" viewBox="0 0 7 12" className={styles.chevron} aria-hidden="true">
      <path
        d="M1 1l5 5-5 5"
        stroke="var(--color-ink-4)"
        strokeWidth="1.6"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

/** '(필수)' / '(선택)' 접미 라벨. */
export function RequirementTag({ required }: { required: boolean }) {
  return (
    <span className={required ? styles.required : styles.optional}>
      {required ? '(필수)' : '(선택)'}
    </span>
  )
}
