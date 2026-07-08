package com.neuedu.ems.exception;


import com.neuedu.ems.common.ResultCode;

/**
 * 自定义业务异常
 * 用于抛出明确的业务错误（如用户不存在、参数错误等）
 */

public class BusinessException extends RuntimeException {
    /**
     * 业务状态码
     */
    private final int code;

    public int getCode() {
        return code;
    }

    /**
     * 构造方法：使用自定义状态码和提示语
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法：使用枚举定义的状态码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 构造方法：带异常原因
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }
}