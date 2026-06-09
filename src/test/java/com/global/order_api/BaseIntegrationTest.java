package com.global.order_api.core.base;



import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    @ServiceConnection
    private static  final MySQLContainer mySqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("order_api_test")
            .withUsername("test")
            .withPassword("test");
}
