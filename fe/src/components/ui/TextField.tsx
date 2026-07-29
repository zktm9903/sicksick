import { useId, type InputHTMLAttributes } from 'react'

import { cx } from '@/lib/cx'

import styles from './TextField.module.css'

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  error?: string
  className?: string
}

export function TextField({ label, error, className, id, ...props }: TextFieldProps) {
  const autoId = useId()
  const inputId = id ?? autoId

  return (
    <div className={cx(styles.field, className)}>
      {label && (
        <label className={styles.label} htmlFor={inputId}>
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={styles.input}
        aria-invalid={error ? true : undefined}
        {...props}
      />
      {error && <p className={styles.error}>{error}</p>}
    </div>
  )
}
