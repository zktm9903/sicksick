import { cx } from '@/lib/cx'

import styles from './Chip.module.css'

type ChipProps = {
  label: string
  active: boolean
  onClick: () => void
  /** 목록처럼 세로로 쌓을 때 폭을 채운다. */
  block?: boolean
}

/** 알약 모양 토글. 증상 선택과 진단 상태 전환에 쓴다. */
export function Chip({ label, active, onClick, block = false }: ChipProps) {
  return (
    <button
      type="button"
      className={cx(styles.chip, active && styles.active, block && styles.block)}
      aria-pressed={active}
      onClick={onClick}
    >
      {label}
    </button>
  )
}
