package kr.sicksick.be.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;

/**
 * DB 가 필요한 통합 테스트의 공통 기반.
 *
 * <p>스키마가 MySQL 전용(ENGINE=InnoDB, DATETIME(6))이라 H2 로 대체하면 검증이 거짓이 된다.
 * 실제 MySQL 컨테이너에 Flyway 마이그레이션을 그대로 적용하므로, 이 클래스를 상속한
 * 테스트가 하나라도 돌면 마이그레이션이 유효하다는 것까지 함께 검증된다.
 *
 * <p><b>{@code @Import} 가 반드시 있어야 한다.</b> {@code @TestConfiguration} 중첩 클래스는
 * 테스트 클래스 자신에 선언됐을 때만 자동 탐지되고 상위 클래스에서 상속되지는 않는다.
 * 빠뜨리면 컨테이너가 뜨지 않고 {@code application.properties} 의 기본값을 따라
 * <b>로컬 개발 DB(localhost:3306)에 조용히 붙어</b> 테스트 데이터를 거기에 쌓는다.
 * 실패하지 않기 때문에 알아채기 어렵다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTest.Containers.class)
public abstract class IntegrationTest {

    /**
     * 로컬 개발 DB(compose)와 같은 8.4 를 쓴다. 운영 HeatWave 와도 메이저 버전이 같다.
     *
     * <p>static 이라 JVM 당 한 번만 뜨고 모든 테스트가 공유한다. stop 하지 않는 이유는
     * 종료 시 Ryuk 이 정리해 주기 때문이다.
     */
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        MySQLContainer mysqlContainer() {
            return MYSQL;
        }

        /** 테스트는 스텁 프로바이더를 쓴다. 실제 카카오·네이버로 요청이 나가지 않는다. */
        @Bean
        DynamicPropertyRegistrar oauthStubProperties() {
            return registry -> registry.add("sicksick.oauth.stub-enabled", () -> true);
        }
    }
}
