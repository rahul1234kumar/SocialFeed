package com.example.socialfeed;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared test infrastructure: a real MySQL in Docker.
 * Use it with @Import(TestcontainersConfiguration.class) on any @SpringBootTest.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection // Spring points the DataSource at this container (URL, user, password)
    MySQLContainer mysqlContainer() {
        // Same image as docker-compose.yml, so tests run against the same MySQL version as dev.
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
