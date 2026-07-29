import type { ReactNode } from 'react'

import { cx } from '@/lib/cx'

import styles from './Screen.module.css'

type SectionProps = {
  children: ReactNode
  className?: string
}

/** 화면 전체를 감싸는 세로 플렉스 컨테이너. */
export function Screen({ children, className }: SectionProps) {
  return <div className={cx(styles.screen, className)}>{children}</div>
}

/** 남는 높이를 채우며 스크롤되는 본문 영역. */
export function ScreenBody({ children, className }: SectionProps) {
  return <div className={cx(styles.body, className)}>{children}</div>
}

/** 하단에 고정되는 CTA 영역. */
export function ScreenFooter({ children, className }: SectionProps) {
  return <div className={cx(styles.footer, className)}>{children}</div>
}
