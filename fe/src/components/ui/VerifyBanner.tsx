import type { ReactNode } from 'react'

import styles from './VerifyBanner.module.css'

type VerifyBannerProps = {
  children: ReactNode
}

export function VerifyBanner({ children }: VerifyBannerProps) {
  return (
    <div className={styles.banner} role="status">
      <span className={styles.icon} aria-hidden="true">
        <svg width="12" height="10" viewBox="0 0 12 10">
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
      <span className={styles.text}>{children}</span>
    </div>
  )
}
