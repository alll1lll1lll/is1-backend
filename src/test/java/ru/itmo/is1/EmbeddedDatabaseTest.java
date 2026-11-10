package ru.itmo.is1;

import java.io.IOException;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

abstract class EmbeddedDatabaseTest {
    private static final EmbeddedPostgres DATABASE = startDatabase();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    private static EmbeddedPostgres startDatabase() {
        try {
            EmbeddedPostgres database = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    database.close();
                } catch (IOException ignored) {
                }
            }));
            return database;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
