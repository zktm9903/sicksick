import type { ButtonHTMLAttributes } from 'react'

import { cx } from '@/lib/cx'

import styles from './Button.module.css'

type ButtonVariant = 'primary' | 'outline' | 'text'
type ButtonSize = 'md' | 'lg'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
}

export function Button({
  variant = 'primary',
  size = 'lg',
  type = 'button',
  className,
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cx(
        styles.button,
        styles[variant],
        variant !== 'text' && styles[size],
        className,
      )}
      {...props}
    />
  )
}
