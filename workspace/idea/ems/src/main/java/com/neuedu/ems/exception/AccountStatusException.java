package com.neuedu.ems.exception;

import com.neuedu.ems.common.ResultCode;

/**
 * 账号状态不允许登录时抛出的认证异常。
 */
public class AccountStatusException extends RuntimeException {
    private final ResultCode resultCode;

    public AccountStatusException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
