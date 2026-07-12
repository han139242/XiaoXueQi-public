package com.neuedu.ems.common;

public enum ResultCode {
    // 原有状态码
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    USER_AND_PASSWORD_ERROR(1001, "用户名或密码错误"),
    
    // 认证相关状态码
    TOKEN_ERROR(1002, "令牌已过期或者失效"),
    BUSINESS_ERROR(1003, "业务处理异常"),
    USER_DISABLED(1004, "账号已停用，请联系管理员"),
    USER_LOCKED(1005, "账号已锁定，请联系管理员"),
    USER_STATUS_ERROR(1006, "账号状态异常，请联系管理员"),
    DATA_CONFLICT(1007, "数据已被其他用户修改，请刷新后重试");

    private final int code;
    private final String message;

    // 构造方法和getter
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
