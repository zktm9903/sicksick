package kr.sicksick.be.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import kr.sicksick.be.auth.domain.RefreshToken;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.repository.RefreshTokenRepository;
import kr.sicksick.be.auth.repository.UserRepository;
import kr.sicksick.be.support.IntegrationTest;

/**
 * 리프레시 토큰 회전.
 *
 * <p>실제 트랜잭션 경계에서 검증해야 하는 로직이라 통합 테스트로 둔다. 특히 재사용 탐지는
 * "폐기하고 나서 예외를 던지는" 흐름이라, 같은 트랜잭션에서 처리하면 롤백으로 폐기가
 * 되돌아간다. 단위 테스트로는 그 실패를 잡을 수 없다.
 */
class RefreshTokenServiceTest extends IntegrationTest {

    @Autowired
    RefreshTokenService refreshTokens;

    @Autowired
    RefreshTokenRepository repository;

    @Autowired
    UserRepository users;

    private Long userId;

    @BeforeEach
    void createUser() {
        userId = users.save(User.pending(null, "tester", Instant.now())).getId();
    }

    @Test
    void 회전하면_새_토큰이_나오고_기존_토큰은_폐기된다() {
        Instant now = Instant.now();
        String first = refreshTokens.issue(userId, ClientType.WEB, now);

        RefreshTokenService.Rotation rotation = refreshTokens.rotate(first, ClientType.WEB, now);

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.rawToken()).isNotEqualTo(first);
        assertThat(repository.findByTokenHash(Secrets.sha256Hex(first)))
                .get()
                .matches(RefreshToken::isRevoked, "폐기됨");
    }

    /**
     * 정상 클라이언트는 폐기된 토큰을 다시 보내지 않는다. 다시 왔다면 유출로 보고
     * 그 유저의 살아있는 토큰을 전부 끊어야 한다 — 방금 발급된 새 토큰까지 포함해서.
     */
    @Test
    void 폐기된_토큰이_다시_오면_해당_유저의_토큰_전체를_끊는다() {
        Instant now = Instant.now();
        String first = refreshTokens.issue(userId, ClientType.WEB, now);
        String second = refreshTokens.rotate(first, ClientType.WEB, now).rawToken();

        assertThatThrownBy(() -> refreshTokens.rotate(first, ClientType.WEB, now))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // 폐기가 예외와 함께 롤백되면 이 검증이 깨진다.
        assertThatThrownBy(() -> refreshTokens.rotate(second, ClientType.WEB, now))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void 만료된_토큰은_회전할_수_없다() {
        Instant issuedAt = Instant.now().minus(Duration.ofDays(30));
        String token = refreshTokens.issue(userId, ClientType.WEB, issuedAt);

        // 웹 수명은 14일이므로 30일 뒤 발급분은 이미 만료 상태다.
        assertThatThrownBy(() -> refreshTokens.rotate(token, ClientType.WEB, Instant.now()))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("만료");
    }

    @Test
    void 알_수_없는_토큰은_거부한다() {
        assertThatThrownBy(() -> refreshTokens.rotate("made-up", ClientType.WEB, Instant.now()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void 앱은_웹보다_긴_수명을_받는다() {
        Instant now = Instant.now();
        String web = refreshTokens.issue(userId, ClientType.WEB, now);
        String app = refreshTokens.issue(userId, ClientType.APP, now);

        Instant webExpiry = repository.findByTokenHash(Secrets.sha256Hex(web)).orElseThrow().getExpiresAt();
        Instant appExpiry = repository.findByTokenHash(Secrets.sha256Hex(app)).orElseThrow().getExpiresAt();

        assertThat(appExpiry).isAfter(webExpiry);
    }

    /** 슬라이딩 만료가 무한히 이어지지 않도록 최초 발급 시점 기준 상한이 있다. */
    @Test
    void 회전해도_체인_시작_시각은_물려받는다() {
        Instant now = Instant.now();
        String first = refreshTokens.issue(userId, ClientType.WEB, now);
        Instant chainStart = repository.findByTokenHash(Secrets.sha256Hex(first))
                .orElseThrow().getChainStartedAt();

        String second = refreshTokens.rotate(first, ClientType.WEB, now.plusSeconds(60)).rawToken();

        assertThat(repository.findByTokenHash(Secrets.sha256Hex(second)).orElseThrow().getChainStartedAt())
                .isEqualTo(chainStart);
    }

    @Test
    void 로그아웃은_해당_토큰만_끊는다() {
        Instant now = Instant.now();
        String phone = refreshTokens.issue(userId, ClientType.APP, now);
        String laptop = refreshTokens.issue(userId, ClientType.WEB, now);

        refreshTokens.revoke(phone, now);

        assertThat(repository.findByTokenHash(Secrets.sha256Hex(phone)).orElseThrow().isRevoked()).isTrue();
        assertThat(repository.findByTokenHash(Secrets.sha256Hex(laptop)).orElseThrow().isRevoked()).isFalse();
    }
}
