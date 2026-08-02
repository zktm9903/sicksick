import { useNavigate } from 'react-router'

import { Mascot } from '@/components/brand/Mascot'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { useSession } from '@/features/auth/session'

import styles from './DonePage.module.css'

export function DonePage() {
  const navigate = useNavigate()
  const { data: state } = useSession()

  // AuthGuard 를 통과했으므로 여기서는 인증 상태다. 다만 로딩 중일 수 있어 방어한다.
  const session = state?.status === 'authenticated' ? state.session : null
  const name = session?.nickname

  return (
    <Screen>
      <ScreenBody className={styles.body}>
        <Mascot size={128} />
        <h2 className={styles.title}>{name ? `${name}님, ` : ''}본인인증 완료!</h2>
        <p className={styles.description}>
          씩씩이와 함께 나만의 기록을
          <br />
          차근차근 쌓아가 볼까요?
        </p>
      </ScreenBody>

      <ScreenFooter className={styles.footer}>
        {/* 다음 화면은 서버가 정한다. 온보딩은 아직 플레이스홀더다. */}
        <Button onClick={() => navigate(session?.nextStep ?? '/')}>시작하기</Button>
      </ScreenFooter>
    </Screen>
  )
}
