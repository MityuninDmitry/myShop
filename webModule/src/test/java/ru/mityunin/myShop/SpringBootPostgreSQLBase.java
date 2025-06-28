package ru.mityunin.myShop;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mityunin.myShop.model.User;

import java.time.Duration;

/**
 * Базовый класс для всех тестов, в которых используется PostgreSQL-контейнер.
 */
@Testcontainers
@SpringBootTest
@Import({TestApiClientConfig.class, TestRedisConfig.class})
@ActiveProfiles("test")
public abstract class SpringBootPostgreSQLBase {
    private static final Network network = Network.newNetwork();
    private static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:latest") // Имя и версия образа
                .withNetwork(network)
                .withDatabaseName("testdb") // Название базы данных
                .withUsername("junit")      // Логин
                .withPassword("junit");

        postgres.start();
    }




    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @Autowired
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;


    @BeforeEach
    void clearRedis() {
        reactiveRedisTemplate.execute(connection -> connection.serverCommands().flushAll())
                .blockLast(Duration.ofSeconds(1));
    }
    @AfterEach
    void tearDown() {
        reactiveRedisTemplate.getConnectionFactory().getReactiveConnection().close();
    }
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName());

        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // Для Liquibase (если нужно)
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:/db/changelog/db.changelog-master.xml");
        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
    }
    @AfterAll
    public static void cleanup() {
        if (redisContainer != null) {
            redisContainer.stop();
        }

    }


    public User createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setRole(role);
        return user;
    }

}


