import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { useLogout } from '@/features/auth/session'

import styles from './HomePage.module.css'

/**
 * 홈 — 아직 내용이 없는 임시 화면.
 *
 * <p>로그아웃 버튼만 두는 이유: 흐름을 확인하려면 계정을 갈아타야 하는데 화면에 세션을
 * 끊을 방법이 없다. 앱(웹뷰)에서는 DevTools 로 쿠키를 지울 수도 없다. 실제 홈을 만들 때
 * 이 화면은 통째로 교체된다.
 *
 * <p>{@code PlaceholderPage} 를 쓰지 않는 이유: 그건 404 화면도 함께 쓰는데, 거기서는
 * "뒤로 가기"가 여전히 맞는 동작이다. 홈은 흐름의 종착지라 뒤로 갈 곳이 없다.
 */
export function HomePage() {
  const logout = useLogout()

  return (
    <Screen>
      <ScreenBody className={styles.body}>
        <h1 className={styles.title}>홈</h1>
        <p className={styles.description}>
          아직 만들지 않은 화면이에요.
          <br />
          여기서부터 증상 기록을 붙여 나갈 거예요.
        </p>
      </ScreenBody>

      <ScreenFooter>
        <Button variant="outline" disabled={logout.isPending} onClick={() => logout.mutate()}>
          로그아웃
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
