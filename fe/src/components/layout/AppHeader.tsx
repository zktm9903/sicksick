import { cx } from '@/lib/cx'

import styles from './AppHeader.module.css'

type AppHeaderProps = {
  title: string
  /** 없으면 버튼 자리는 유지하되 보이지 않는다(타이틀 위치 고정). */
  onBack?: () => void
}

export function AppHeader({ title, onBack }: AppHeaderProps) {
  return (
    <div className={styles.header}>
      <button
        type="button"
        className={cx(styles.back, !onBack && styles.backHidden)}
        onClick={onBack}
        aria-label="뒤로 가기"
        aria-hidden={onBack ? undefined : true}
        tabIndex={onBack ? undefined : -1}
      >
        <svg width="9" height="16" viewBox="0 0 9 16" aria-hidden="true">
          <path
            d="M8 1L1 8l7 7"
            stroke="currentColor"
            strokeWidth="2"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>
      <h1 className={styles.title}>{title}</h1>
    </div>
  )
}
