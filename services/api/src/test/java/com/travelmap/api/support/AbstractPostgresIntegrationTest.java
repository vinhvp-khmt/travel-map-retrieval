package com.travelmap.api.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base cho integration test chạy trên PostGIS THẬT qua Testcontainers — khác hẳn phần lớn
 * test còn lại trong project (mock repository, hoặc {@code @SpringBootTest} trỏ vào H2 qua
 * {@code src/test/resources/application.yml}).
 *
 * <p>Lý do cần lớp này: {@code src/test/resources/application.yml} tắt Flyway
 * ({@code spring.flyway.enabled=false}) và dùng Hibernate {@code ddl-auto=create-drop} trên
 * H2 cho các test {@code @SpringBootTest} thường — nhanh, nhưng chỉ tạo bảng cho các
 * {@code @Entity} JPA. {@code search_log}, {@code search_history}, {@code poi_view_history}
 * không phải entity (truy cập qua {@code JdbcTemplate} thô) nên H2 KHÔNG BAO GIỜ tạo các
 * bảng đó — một bug ở tầng SQL thật (như constraint {@code NOT NULL}, kiểu cột, index) sẽ
 * không bao giờ lộ ra qua một unit test mock hay một {@code @SpringBootTest} thường. Class
 * này bật lại Flyway thật và trỏ datasource vào container Postgres/PostGIS thật, để test kế
 * thừa nó chạy đúng schema V1..V9 y hệt production.
 *
 * <p>Container dùng ảnh {@code postgis/postgis} (không phải {@code postgres} trơn) vì schema
 * dùng kiểu {@code geography}/hàm {@code ST_*} — khớp {@code infra/docker-compose.yml} dùng
 * cho local dev.
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void useRealFlywayAndValidateSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
