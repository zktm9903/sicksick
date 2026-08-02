import { isRouteErrorResponse, useNavigate, useRouteError } from 'react-router'

import { ROUTES } from '@/app/routes'
import { Mascot } from '@/components/brand/Mascot'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'

import styles from './ErrorPage.module.css'

/**
 * 화면을 그리는 도중 오류가 난 경우.
 *
 * 여기까지 오면 alert 를 띄울 화면 자체가 없으므로 전용 화면으로 안내한다.
 * 라우터의 `errorElement` 로 등록되며, 이게 없으면 React Router 의 개발자용
 * 오류 화면(스택 트레이스)이 사용자에게 그대로 노출된다.
 */
export function ErrorPage() {
  const error = useRouteError()
  const navigate = useNavigate()

  // 원인은 콘솔에만 남긴다. 화면에는 내부 정보를 내보내지 않는다.
  console.error('[route error]', error)

  const notFound = isRouteErrorResponse(error) && error.status === 404

  return (
    <Screen>
      <ScreenBody className={styles.body}>
        <Mascot size={128} />
        <h1 className={styles.title}>
          {notFound ? '페이지를 찾을 수 없어요' : '문제가 생겼어요'}
        </h1>
        <p className={styles.description}>
          {notFound ? (
            <>
              주소가 바뀌었거나
              <br />
              사라진 페이지예요.
            </>
          ) : (
            <>
              잠시 후 다시 시도해 주세요.
              <br />
              계속 이러면 알려주세요.
            </>
          )}
        </p>
      </ScreenBody>

      <ScreenFooter className={styles.footer}>
        {/* 라우터 상태가 깨졌을 수 있으므로 클라이언트 라우팅 대신 새로 불러온다. */}
        <Button onClick={() => window.location.reload()}>다시 시도</Button>
        <Button variant="text" onClick={() => navigate(ROUTES.login, { replace: true })}>
          처음으로
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
