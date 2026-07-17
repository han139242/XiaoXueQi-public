package com.neuedu.ems.exception;

/**
 * 电话号码属于资源内的敏感字段，资源写权限并不自动允许修改该字段。
 */
public class SensitivePhoneAccessDeniedException extends RuntimeException {

    public SensitivePhoneAccessDeniedException() {
        super("当前账号无联系电话写入权限");
    }
}
