package kr.sicksick.be.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.sicksick.be.auth.domain.Provider;
import kr.sicksick.be.auth.domain.SocialAccount;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 로그인 식별 조회. 이메일이 아니라 이 조합이 유일한 식별키다.
     *
     * <p>user 를 fetch join 으로 함께 읽는다. {@code open-in-view=false} 라 트랜잭션을
     * 벗어나면 지연 로딩이 불가능한데, 호출부(컨트롤러)는 반환된 User 의 필드를 그대로
     * 읽기 때문이다. 이 조인이 없으면 <b>기존 계정 재로그인만</b> LazyInitializationException
     * 으로 실패한다(신규 가입은 방금 만든 엔티티라 멀쩡하다).
     */
    @Query("""
            select sa
              from SocialAccount sa
              join fetch sa.user
             where sa.provider = :provider
               and sa.providerUserId = :providerUserId
            """)
    Optional<SocialAccount> findWithUserByProviderAndProviderUserId(
            @Param("provider") Provider provider,
            @Param("providerUserId") String providerUserId);
}
