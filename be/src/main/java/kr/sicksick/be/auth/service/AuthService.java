package kr.sicksick.be.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.sicksick.be.auth.domain.SocialAccount;
import kr.sicksick.be.auth.domain.User;
import kr.sicksick.be.auth.oauth.OAuthUserInfo;
import kr.sicksick.be.auth.repository.SocialAccountRepository;
import kr.sicksick.be.auth.repository.UserRepository;

/** 소셜 인증 결과를 우리 계정에 연결한다. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final SocialAccountRepository socialAccounts;

    AuthService(UserRepository users, SocialAccountRepository socialAccounts) {
        this.users = users;
        this.socialAccounts = socialAccounts;
    }

    /**
     * 소셜 계정에 대응하는 유저를 찾고, 없으면 만든다.
     *
     * @throws EmailAlreadyLinkedException 이메일이 이미 다른 계정에 물려 있는 경우
     */
    @Transactional
    public User resolveUser(OAuthUserInfo info, Instant now) {
        return socialAccounts
                .findWithUserByProviderAndProviderUserId(info.provider(), info.providerUserId())
                .map(SocialAccount::getUser)
                .orElseGet(() -> signUp(info, now));
    }

    private User signUp(OAuthUserInfo info, Instant now) {
        // 소셜이 주는 이메일은 검증됐다는 보장이 없으므로 자동 병합하지 않는다.
        if (info.email() != null && users.existsByEmailAndDeletedAtIsNull(info.email())) {
            throw new EmailAlreadyLinkedException(info.email());
        }

        try {
            User user = users.saveAndFlush(User.pending(info.email(), info.nickname(), now));
            socialAccounts.saveAndFlush(SocialAccount.link(
                    user, info.provider(), info.providerUserId(), info.email(), now));
            log.info("소셜 신규 가입 — provider={} userId={}", info.provider(), user.getId());
            return user;
        } catch (DataIntegrityViolationException e) {
            // 콜백이 동시에 두 번 들어온 경우. 유니크 제약이 막아줬으니 기존 것을 쓴다.
            log.info("소셜 가입 경합 감지 — provider={}, 기존 계정으로 재조회", info.provider());
            return socialAccounts
                    .findWithUserByProviderAndProviderUserId(info.provider(), info.providerUserId())
                    .map(SocialAccount::getUser)
                    .orElseThrow(() -> e);
        }
    }
}
