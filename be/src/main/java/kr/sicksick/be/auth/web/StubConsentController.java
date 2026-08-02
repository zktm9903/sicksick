package kr.sicksick.be.auth.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.util.HtmlUtils;

/**
 * 카카오·네이버 동의 화면을 대신하는 개발용 페이지.
 *
 * <p>앱 키가 없어도 로그인 → 콜백 → 토큰 발급 → 다음 단계 이동까지 전 구간을 그대로
 * 돌려볼 수 있다. 입력한 식별자가 인가 코드로 전달되므로, 같은 값으로 다시 로그인하면
 * 같은 계정으로 붙어 "이어하기" 동작도 확인된다.
 *
 * <p>{@code sicksick.oauth.stub-enabled=false} 면 빈 자체가 등록되지 않는다.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth/stub")
@ConditionalOnProperty(name = "sicksick.oauth.stub-enabled", havingValue = "true")
class StubConsentController {

    @GetMapping(value = "/consent", produces = MediaType.TEXT_HTML_VALUE)
    String consent(@RequestParam String provider, @RequestParam String state) {
        // 파라미터는 그대로 HTML 에 들어가므로 반드시 이스케이프한다.
        String safeProvider = HtmlUtils.htmlEscape(provider);
        String safeState = HtmlUtils.htmlEscape(state);

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>개발용 로그인 — %s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
                           background: #edf1ea; display: flex; align-items: center;
                           justify-content: center; min-height: 100vh; margin: 0; }
                    .card { background: #fff; padding: 32px; border-radius: 20px; width: 320px;
                            box-shadow: 0 8px 24px rgba(30,50,40,.08); }
                    h1 { font-size: 17px; margin: 0 0 6px; color: #1a2420; }
                    p { font-size: 13px; color: #6e7a74; margin: 0 0 20px; line-height: 1.5; }
                    label { font-size: 13px; font-weight: 600; color: #3f4a44; }
                    input { width: 100%%; height: 48px; margin: 8px 0 20px; padding: 0 14px;
                            font-size: 15px; border: 1.5px solid #e7ece8; border-radius: 14px;
                            box-sizing: border-box; }
                    button { width: 100%%; height: 52px; border: 0; border-radius: 999px;
                             background: #5bad7f; color: #fff; font-size: 16px; font-weight: 700;
                             cursor: pointer; }
                    .warn { margin-top: 16px; font-size: 12px; color: #8a5a2a; }
                  </style>
                </head>
                <body>
                  <form class="card" method="get" action="/api/v1/auth/oauth/%s/callback">
                    <h1>개발용 %s 로그인</h1>
                    <p>실제 소셜 인증을 건너뜁니다. 아무 식별자나 넣으세요.<br>
                       같은 값으로 다시 로그인하면 같은 계정에 붙습니다.</p>
                    <label for="code">테스트 식별자</label>
                    <input id="code" name="code" value="tester1" autocomplete="off" required>
                    <input type="hidden" name="state" value="%s">
                    <button type="submit">이 계정으로 계속하기</button>
                    <div class="warn">⚠ 개발 전용 화면입니다.</div>
                  </form>
                </body>
                </html>
                """.formatted(safeProvider, safeProvider, safeProvider, safeState);
    }
}
