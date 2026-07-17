package com.neuedu.ems;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "mes.notification.scheduling-enabled=false",
        PhoneCryptoTestProperties.ACTIVE_KEY,
        PhoneCryptoTestProperties.KEY_IDS,
        PhoneCryptoTestProperties.ENCRYPTION_KEY,
        PhoneCryptoTestProperties.LOOKUP_HMAC_KEY
})
class EmsApplicationTests {

    @Test
    void contextLoads() {
    }

}
