package ru.mityunin.myShop;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Базовый класс для всех тестов, в которых используется PostgreSQL-контейнер.
 */
@SpringBootTest
public abstract class SpringBootPostgreSQLBase {

    private static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:latest") // Имя и версия образа
                .withDatabaseName("testdb") // Название базы данных
                .withUsername("junit")      // Логин
                .withPassword("junit");

        postgres.start();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Явно включаем Liquibase для тестов
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:/db/changelog/db.changelog-master.xml");
    }

    @AfterAll
    static void stopPostgres() {
        postgres.close();
    }
}
