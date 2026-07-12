package com.neuedu.ems.exception;

/**
 * 数据版本已变化，当前更新不能覆盖其他请求的结果。
 */
public class OptimisticLockConflictException extends RuntimeException {
    public OptimisticLockConflictException() {
        super("数据已被其他用户修改，请刷新后重试");
    }
}
