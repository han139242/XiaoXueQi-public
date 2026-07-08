package com.neuedu.ems.util;

/**
 * JWT常量类：统一管理JWT配置参数
 */
public class JwtConstants {

    /**
     * JWT签名密钥（重要！生产环境需加密存储，避免硬编码）
     * 建议长度≥32位，可使用随机字符串生成
     */
    public static final String JWT_SECRET = System.getenv().getOrDefault(
            "JWT_SECRET",
            "change-me-please-use-env-var-32-bytes-minimum"
    );

    /**
     * JWT过期时间（单位：秒），此处设置为2小时（7200秒）
     */
    public static final long JWT_EXPIRE_TIME = 7200L;

    /**
     * JWT请求头名称（前端携带token的请求头）
     */
    public static final String JWT_HEADER = "Authorization";

    /**
     * JWT令牌前缀（前端需拼接：Bearer + 空格 + token）
     *
     *
     *
     */
    public static final String JWT_PREFIX = "Bearer ";
}
