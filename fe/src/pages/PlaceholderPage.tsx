import { useLocation, useNavigate } from 'react-router'

import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'

import styles from './PlaceholderPage.module.css'

type PlaceholderPageProps = {
  title: string
}

/** 아직 구현하지 않은 화면 자리. 페이지를 만들면 라우터에서 교체한다. */
export function PlaceholderPage({ title }: PlaceholderPageProps) {
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <Screen>
      <ScreenBody className={styles.body}>
        <h1 className={styles.title}>{title}</h1>
        <code className={styles.path}>{pathname}</code>
        <p className={styles.description}>아직 만들지 않은 화면이에요.</p>
      </ScreenBody>
      <ScreenFooter>
        <Button variant="outline" onClick={() => navigate(-1)}>
          뒤로 가기
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
