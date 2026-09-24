package com.capstone.smart_parcel;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "jwt.secret=parcel-integration-tests-only-secret-at-least-32-bytes",
        "spring.data.redis.host=127.0.0.1","spring.data.redis.port=6379",
        "spring.data.redis.password=","spring.data.redis.ssl.enabled=false",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
public abstract class PostgresIntegrationBase {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url=System.getenv("TEST_DB_URL");
        if (url==null || !url.matches("jdbc:postgresql://[^/]+/[a-zA-Z0-9_]*_test"))
            throw new IllegalStateException("Set TEST_DB_URL to a disposable PostgreSQL database ending in _test; see docs/multi-belt.md");
        properties.add("spring.datasource.url",()->url);
        properties.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_DB_USER","postgres"));
        properties.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_DB_PASSWORD",""));
    }
}
