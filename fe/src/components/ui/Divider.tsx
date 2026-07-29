import { cx } from '@/lib/cx'

import styles from './Divider.module.css'

type DividerProps = {
  children?: string
  className?: string
}

export function Divider({ children, className }: DividerProps) {
  return (
    <div className={cx(styles.divider, className)}>
      {children && <span className={styles.label}>{children}</span>}
    </div>
  )
}
