package com.neuedu.ems.exception;

/**
 * 可安全返回给前端的 AI 服务异常。
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
