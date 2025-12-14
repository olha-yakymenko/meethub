package com.meethub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
public class SimpleContextTest {

    @Test
    void contextLoads() {
        System.out.println("Context loaded successfully!");
    }
}