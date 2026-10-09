package com.movierec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=stage4-context-test-secret-at-least-32-bytes",
        "spring.flyway.enabled=false",
        "app.ai.provider=fake"
})
class MovieRecApplicationTest {
    @Test
    void applicationContextLoads() {
    }
}
