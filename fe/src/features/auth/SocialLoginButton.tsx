import styles from './SocialLoginButton.module.css'
import type { SocialProvider } from './socialProviders'

type SocialLoginButtonProps = {
  provider: SocialProvider
  onClick: (provider: SocialProvider) => void
}

export function SocialLoginButton({ provider, onClick }: SocialLoginButtonProps) {
  return (
    <button
      type="button"
      className={styles.button}
      data-provider={provider.id}
      onClick={() => onClick(provider)}
    >
      {provider.icon}
      {provider.label}
    </button>
  )
}
