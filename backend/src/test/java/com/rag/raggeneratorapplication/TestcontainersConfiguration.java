package com.rag.raggeneratorapplication;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Spins up PostgreSQL with the pgvector extension for integration tests and wires
 * it into Spring via {@link ServiceConnection}, so tests need only Docker — not a
 * hand-run database. Flyway migrations (including {@code CREATE EXTENSION vector})
 * run against this container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(
                DockerImageName.parse("pgvector/pgvector:pg16")
                        .asCompatibleSubstituteFor("postgres"));
    }
}
