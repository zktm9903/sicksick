import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // API 는 항상 현재 도메인 기준 상대경로(/api/...)로 호출한다.
    // 개발 중에도 브라우저에는 same-origin 요청으로 보이므로 CORS 설정이 필요 없고,
    // 운영에서는 리버스 프록시가 이 역할을 대신한다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
