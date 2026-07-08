package com.neuedu.ems.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类：生成token、解析token、验证token有效性
 * @Component：标识为Spring组件，可注入使用
 */
@Component
public class JwtUtil {

    public static void main(String[] args) {
        JwtUtil jwtUtil = new JwtUtil();
//        String token = generateToken(1L, "admin");
//        System.out.println(token);
        boolean b = jwtUtil.validateToken("eyJhbGciOiJIUzM4NCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJqdGkiOiIxIiwiaWF0IjoxNzczMDE1ODc3LCJleHAiOjE3NzMwMjMwNzd9.JqpzQL1RvI1DurXJywefMdpVKuEgQJONo4f3vK30QwQqa1h9dOAZTRshxhcSDWYL");
        System.out.println(b);

    }

    /**
     * 生成JWT token
     * @param userId 用户ID（存入Payload）
     * @param username 用户名（存入Payload）
     * @return JWT token字符串
     */
    public String generateToken(Long userId, String username) {
        // 1. 创建签名密钥（使用HS256算法，密钥长度需≥32位）
        SecretKey secretKey = Keys.hmacShaKeyFor(JwtConstants.JWT_SECRET.getBytes());

        // 2. 构建JWT的Payload（自定义声明）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId); // 存入用户ID
        claims.put("username", username); // 存入用户名

        // 3. 生成token
        return Jwts.builder()
                // 设置自定义声明
                .setClaims(claims)
                // 设置JWT ID（可选，唯一标识token）
                .setId(String.valueOf(userId))
                // 设置签发时间
                .setIssuedAt(new Date())
                // 设置过期时间（当前时间 + 过期秒数）
                .setExpiration(new Date(System.currentTimeMillis() + JwtConstants.JWT_EXPIRE_TIME * 1000))
                // 设置签名算法和密钥
                .signWith(secretKey)
                // 构建并序列化token
                .compact();
    }

    /**
     * 解析JWT token，获取所有声明（Payload）
     * @param token JWT token字符串
     * @return Claims（包含自定义声明和标准声明）
     * @throws Exception 解析失败（如token无效、过期、签名错误）
     */
    public Claims parseToken(String token) throws Exception {
        // 1. 创建签名密钥
        SecretKey secretKey = Keys.hmacShaKeyFor(JwtConstants.JWT_SECRET.getBytes());

        // 2. 解析token，验证签名和过期时间
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证JWT token是否有效
     * @param token JWT token字符串
     * @return true=有效，false=无效（过期/签名错误/格式错误）
     */
    public boolean validateToken(String token) {
        try {
            // 解析token，若解析成功则说明token有效
            Claims claims = parseToken(token);
            // 额外验证过期时间（可选，parseToken已自动验证）
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // 捕获所有解析异常，返回false
            System.out.println("JWT token验证失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 从token中获取用户名
     * @param token JWT token字符串
     * @return 用户名
     * @throws Exception 解析失败
     */
    public String getUsernameFromToken(String token) throws Exception {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从token中获取用户ID
     * @param token JWT token字符串
     * @return 用户ID
     * @throws Exception 解析失败
     */
    public Long getUserIdFromToken(String token) throws Exception {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String extractTokenFromHeader(String headerValue) {

        //         "Bear  "
        if (headerValue == null || !headerValue.startsWith(JwtConstants.JWT_PREFIX)) {
            return null;
        }
        // 截取前缀后的token（"Bearer "长度为7）
        //Bearer eyJhbGciOiJIUzM4NCJ9.eyJ1c2VySWQiOjEyLCJ1c2VybmFtZSI6InpoYW5nd3VqaSIsImp0aSI6IjEyIiwiaWF0IjoxNzgzNDc1NDIzLCJleHAiOjE3ODM0ODI2MjN9.m_EKEUoWc8kxpoZy5wZ925wHimY2r3DaJPUYLyksdUWzxG1FdQJtiWVQJKDugMMI
        return headerValue.substring(JwtConstants.JWT_PREFIX.length()).trim();
    }
}