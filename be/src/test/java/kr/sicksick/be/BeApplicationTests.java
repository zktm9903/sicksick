package kr.sicksick.be;

import org.junit.jupiter.api.Test;

import kr.sicksick.be.support.IntegrationTest;

/**
 * 컨텍스트가 뜨는지 확인한다.
 *
 * <p>{@code ddl-auto=validate} 이므로 이 테스트의 통과는 Flyway 마이그레이션과 JPA 엔티티
 * 매핑이 서로 어긋나지 않았다는 뜻이기도 하다.
 */
class BeApplicationTests extends IntegrationTest {

    @Test
    void 컨텍스트가_로드되고_스키마와_엔티티가_일치한다() {
    }
}
