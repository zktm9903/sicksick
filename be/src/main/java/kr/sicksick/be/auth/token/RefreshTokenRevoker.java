package kr.sicksick.be.auth.token;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import kr.sicksick.be.auth.repository.RefreshTokenRepository;

/**
 * 사고 대응용 일괄 폐기를 <b>별도 트랜잭션</b>으로 수행한다.
 *
 * <p>재사용 탐지는 "폐기하고 나서 예외를 던지는" 흐름인데, 같은 트랜잭션에서 처리하면
 * 예외로 롤백되면서 폐기까지 함께 되돌아간다. 그러면 탈취된 체인이 그대로 살아남는다.
 * 폐기만 REQUIRES_NEW 로 떼어내 예외와 무관하게 커밋시킨다.
 *
 * <p>프록시를 거쳐야 새 트랜잭션이 열리므로 {@code RefreshTokenService} 안의 자기호출이
 * 아니라 별도 빈으로 둔다.
 */
@Component
class RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokens;

    RefreshTokenRevoker(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllByUserId(Long userId, Instant now) {
        return refreshTokens.revokeAllByUserId(userId, now);
    }
}
