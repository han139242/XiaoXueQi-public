package com.neuedu.ems.exception;

/**
 * 当前用户功能权限有效，但目标记录超出其数据范围。
 */
public class DataScopeAccessDeniedException extends RuntimeException {
    public DataScopeAccessDeniedException() {
        super("目标数据超出当前账号的数据范围");
    }
}
