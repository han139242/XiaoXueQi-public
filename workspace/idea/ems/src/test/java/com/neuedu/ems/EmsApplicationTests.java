package com.neuedu.ems;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mes.notification.scheduling-enabled=false")
class EmsApplicationTests {

    @Test
    void contextLoads() {
    }

}
