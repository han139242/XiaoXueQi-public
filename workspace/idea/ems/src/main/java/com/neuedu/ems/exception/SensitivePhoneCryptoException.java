package com.neuedu.ems.exception;

/**
 * 不向调用方暴露密钥、密文或认证失败细节的敏感字段加密错误。
 */
public class SensitivePhoneCryptoException extends RuntimeException {

    public SensitivePhoneCryptoException() {
        super("联系电话敏感数据暂不可用");
    }

    public SensitivePhoneCryptoException(Throwable cause) {
        super("联系电话敏感数据暂不可用", cause);
    }
}
