package com.yuldashov.drive2.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.19")
            .withDatabaseName("drive-test")
            .withUsername("test")
            .withPassword("test");

    @Container
    public static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data")
            .waitingFor(new HttpWaitStrategy()
                    .forPath("/minio/health/live")
                    .forPort(9000)
                    .withStartupTimeout(Duration.ofMinutes(1)));

    @DynamicPropertySource
    public static void configurePropeties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        String minioEndpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        registry.add("minio.endpoint", () -> minioEndpoint);
    }

}
