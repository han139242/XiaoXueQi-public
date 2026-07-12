package com.neuedu.ems.common;

import com.neuedu.ems.exception.AccountStatusException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsSpecificAccountStatusError() {
        assertAccountStatusResult(ResultCode.USER_DISABLED);
        assertAccountStatusResult(ResultCode.USER_LOCKED);
        assertAccountStatusResult(ResultCode.USER_STATUS_ERROR);
    }

    private void assertAccountStatusResult(ResultCode resultCode) {
        ResultData result = handler.handleAccountStatusException(new AccountStatusException(resultCode));

        assertEquals(resultCode.getCode(), result.get("code"));
        assertEquals(resultCode.getMessage(), result.get("msg"));
    }
}
