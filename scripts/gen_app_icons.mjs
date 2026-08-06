/**
 * 씩씩이 마스코트로 앱 아이콘·스플래시 이미지를 만든다.
 *
 *   node scripts/gen_app_icons.mjs
 *
 * 원본은 웹의 `fe/src/components/brand/Mascot.tsx` 다. 로그인 화면에 보이는 그 캐릭터를
 * 그대로 쓴다. 마스코트 모양이 바뀌면 아래 MASCOT_BODY 를 맞춰 고치고 다시 돌린다.
 *
 * 렌더링은 크롬 헤드리스로 한다(맥에 기본으로 있는 sips 는 SVG 를 못 읽고, rsvg·
 * ImageMagick 은 설치돼 있지 않다). 별도 의존성을 받지 않으려는 선택이다.
 */

import { execFileSync } from 'node:child_process'
import { mkdtempSync, writeFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const OUT = join(ROOT, 'app', 'assets')

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

// ── 브랜드 색 (fe/src/styles/tokens.css · 로그인 화면 히어로와 같은 값) ──
const HERO_GRADIENT = 'linear-gradient(160deg, #F2F8EF 0%, #E8F3EC 55%, #CFE5D6 100%)'

/**
 * 마스코트 본체.
 *
 * 웹 컴포넌트에는 바닥 그림자 타원이 있지만 아이콘에서는 뺀다. 정사각형 안에 캐릭터만
 * 가운데 놓여야 하는데, 그림자가 있으면 무게중심이 아래로 쏠려 기울어 보인다.
 */
const MASCOT_BODY = `
  <path d="M60 40C86 40 96 62 92 82 89 98 74 108 60 108 46 108 31 98 28 82 24 62 34 40 60 40Z"
        fill="#A3D0AC" stroke="#33754F" stroke-width="3.4" stroke-linejoin="round"/>
  <ellipse cx="46" cy="56" rx="9" ry="6" fill="rgba(255,255,255,0.5)"/>
  <ellipse cx="43" cy="76" rx="6" ry="3.4" fill="#F2A0A0" opacity="0.55"/>
  <ellipse cx="77" cy="76" rx="6" ry="3.4" fill="#F2A0A0" opacity="0.55"/>
  <circle cx="51.5" cy="60" r="5.2" fill="#fff" stroke="#33754F" stroke-width="1.6"/>
  <circle cx="52.3" cy="61" r="2.6" fill="#1C2418"/>
  <circle cx="68.5" cy="60" r="5.2" fill="#fff" stroke="#33754F" stroke-width="1.6"/>
  <circle cx="69.3" cy="61" r="2.6" fill="#1C2418"/>
  <path d="M54 80Q60 85 66 80" stroke="#33754F" stroke-width="2.6" fill="none" stroke-linecap="round"/>
`

/**
 * 단색 실루엣. 안드로이드 모노크롬 아이콘용.
 *
 * 시스템이 불투명한 픽셀을 테마 색으로 칠하므로 색은 의미가 없고 모양만 남는다.
 * 눈·입은 마스크로 구멍을 뚫어야 표정이 살아난다 — 통짜 실루엣은 그냥 덩어리다.
 */
const MASCOT_SILHOUETTE = `
  <defs>
    <mask id="face">
      <path d="M60 40C86 40 96 62 92 82 89 98 74 108 60 108 46 108 31 98 28 82 24 62 34 40 60 40Z" fill="#fff"/>
      <circle cx="51.5" cy="60" r="5.4" fill="#000"/>
      <circle cx="68.5" cy="60" r="5.4" fill="#000"/>
      <path d="M54 80Q60 86 66 80" stroke="#000" stroke-width="4" fill="none" stroke-linecap="round"/>
    </mask>
  </defs>
  <path d="M60 40C86 40 96 62 92 82 89 98 74 108 60 108 46 108 31 98 28 82 24 62 34 40 60 40Z"
        fill="#000" mask="url(#face)"/>
`

/**
 * 캐릭터를 정사각형 가운데에 두는 viewBox.
 *
 * 본체는 원본 좌표계에서 x 26~94, y 38~110 을 차지한다(선 굵기 포함). 중심은 (60, 74)
 * 이고, 여기에 여백을 붙여 정사각형으로 자른다. 값이 작을수록 캐릭터가 크게 나온다.
 */
function viewBox(span) {
  return `${60 - span / 2} ${74 - span / 2} ${span} ${span}`
}

function svg({ span, body, size }) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox(span)}"
               width="${size}" height="${size}">${body}</svg>`
}

/**
 * @param name       파일 이름
 * @param size       정사각형 한 변 픽셀
 * @param span       캐릭터를 감싸는 정사각형 크기(작을수록 꽉 찬다)
 * @param background CSS 배경. 생략하면 투명
 * @param body       그릴 내용. 생략하면 컬러 마스코트
 */
function render({ name, size, span, background, body = MASCOT_BODY }) {
  const html = `<!doctype html><meta charset="utf-8">
<style>
  html, body { margin: 0; padding: 0; width: ${size}px; height: ${size}px; overflow: hidden; }
  body { display: grid; place-items: center; ${background ? `background: ${background};` : ''} }
</style>
${svg({ span, body, size })}`

  const dir = mkdtempSync(join(tmpdir(), 'sicksick-icon-'))
  const page = join(dir, 'page.html')
  writeFileSync(page, html, 'utf8')

  try {
    execFileSync(CHROME, [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      // 이게 없으면 투명해야 할 배경이 흰색으로 채워진다.
      '--default-background-color=00000000',
      '--force-device-scale-factor=1',
      `--window-size=${size},${size}`,
      `--screenshot=${join(OUT, name)}`,
      `file://${page}`,
    ], { stdio: 'pipe' })
    console.log(`  ✓ ${name}  ${size}×${size}${background ? '' : '  (투명)'}`)
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
}

console.log(`아이콘 생성 → ${OUT}`)

// iOS 앱 아이콘. 투명도가 있으면 앱스토어가 거부하므로 배경을 반드시 채운다.
// iOS 가 모서리를 둥글게 깎으므로 캐릭터를 너무 키우면 잘린다 — span 을 넉넉히 둔다.
render({ name: 'icon.png', size: 1024, span: 104, background: HERO_GRADIENT })

// 안드로이드 적응형 아이콘.
// 런처가 어떤 모양으로 깎을지 모르므로 캐릭터는 가운데 66% 안에 들어와야 한다.
render({ name: 'android-icon-foreground.png', size: 512, span: 132 })
render({ name: 'android-icon-background.png', size: 512, span: 104, background: HERO_GRADIENT, body: '' })
render({ name: 'android-icon-monochrome.png', size: 432, span: 132, body: MASCOT_SILHOUETTE })

// 스플래시. app.config.js 가 imageWidth 200 으로 줄여 배경색 위에 얹는다.
render({ name: 'splash-icon.png', size: 1024, span: 84 })

// Expo 웹 빌드용. 지금은 쓰지 않지만 템플릿 기본값이 남아 있지 않게 맞춰 둔다.
render({ name: 'favicon.png', size: 48, span: 84 })

console.log('완료')
