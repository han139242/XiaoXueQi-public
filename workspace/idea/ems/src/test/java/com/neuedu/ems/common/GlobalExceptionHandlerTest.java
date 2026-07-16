package com.neuedu.ems.common;

import com.neuedu.ems.exception.AccountStatusException;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import org.mybatis.spring.MyBatisSystemException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsSpecificAccountStatusError() {
        assertAccountStatusResult(ResultCode.USER_DISABLED);
        assertAccountStatusResult(ResultCode.USER_LOCKED);
        assertAccountStatusResult(ResultCode.USER_STATUS_ERROR);
    }

    @Test
    void returnsSpecificOptimisticLockError() {
        ResultData result = handler.handleOptimisticLockConflict(new OptimisticLockConflictException());

        assertEquals(ResultCode.DATA_CONFLICT.getCode(), result.get("code"));
        assertEquals(ResultCode.DATA_CONFLICT.getMessage(), result.get("msg"));
    }

    @Test
    void unwrapsMyBatisOptimisticLockError() {
        ResultData result = handler.handleMyBatisSystemException(
                new MyBatisSystemException("optimistic lock", new OptimisticLockConflictException()));

        assertEquals(ResultCode.DATA_CONFLICT.getCode(), result.get("code"));
        assertEquals(ResultCode.DATA_CONFLICT.getMessage(), result.get("msg"));
    }

    @Test
    void returnsForbiddenForOutOfScopeData() {
        ResultData result = handler.handleDataScopeAccessDenied(new DataScopeAccessDeniedException());

        assertEquals(ResultCode.FORBIDDEN.getCode(), result.get("code"));
        assertEquals(ResultCode.FORBIDDEN.getMessage(), result.get("msg"));
    }

    @Test
    void preservesSafeBusinessMessageAndOriginalCode() {
        ResultData result = handler.handleBusinessException(
                new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "手工排程草案已被其他用户修改，请刷新后重试"));

        assertEquals(ResultCode.BUSINESS_ERROR.getCode(), result.get("code"));
        assertEquals(ResultCode.BUSINESS_ERROR.getMessage(), result.get("msg"));
        assertEquals(Map.of(
                "message", "手工排程草案已被其他用户修改，请刷新后重试",
                "businessCode", ResultCode.DATA_CONFLICT.getCode()), result.get("data"));
    }

    private void assertAccountStatusResult(ResultCode resultCode) {
        ResultData result = handler.handleAccountStatusException(new AccountStatusException(resultCode));

        assertEquals(resultCode.getCode(), result.get("code"));
        assertEquals(resultCode.getMessage(), result.get("msg"));
    }
}
