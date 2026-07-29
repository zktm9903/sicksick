import { Outlet, ScrollRestoration } from 'react-router'

/** 전역 프로바이더·스크롤 복원 등 모든 화면에 공통으로 걸리는 것들의 자리. */
export function RootLayout() {
  return (
    <>
      <Outlet />
      <ScrollRestoration />
    </>
  )
}
