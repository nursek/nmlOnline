package com.mg.nmlonline;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=test-secret-key-for-ci-at-least-32-chars-long",
    "JWT_PEPPER=test-pepper-value-for-ci-tests-only"
})
class NmlOnlineApplicationTests {

    @Test
    void contextLoads() {
        //TODO : add tests ?
    }

}
