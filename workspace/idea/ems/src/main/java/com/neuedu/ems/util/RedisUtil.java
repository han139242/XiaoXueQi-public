package com.neuedu.ems.util;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Redis通用工具类：封装String类型的核心操作（基础版，后续可扩展Hash/List等）
 * @Component：标识为Spring组件，自动扫描注入
 */
@Component
@Slf4j
public class RedisUtil {
    @Autowired
    private ObjectMapper objectMapper;
    /**
     * 注入自定义配置的RedisTemplate
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== String类型操作 ====================

    /**
     * 设置缓存（无过期时间）
     * @param key 缓存键（不可为空）
     * @param value 缓存值（可序列化对象）
     */
    public void set(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位（如TimeUnit.MINUTES）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("过期时间不能为负数");
        }
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, value, timeout, unit);
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     */
    public void set(String key, Object value, long timeout) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("过期时间不能为负数");
        }
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        operations.set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @return 缓存值（返回Object，需自行强转）
     */
    public Object get(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        ValueOperations<String, Object> operations = redisTemplate.opsForValue();
        return operations.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        try {
            ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            Object cachedValue = operations.get(key);

            if (cachedValue == null) {
                return null;
            }

            if (clazz.isInstance(cachedValue)) {
                return clazz.cast(cachedValue);
            }

            if (cachedValue instanceof String json) {
                if (StrUtil.isBlank(json)) {
                    return null;
                }
                return objectMapper.readValue(json, clazz);
            }

            return objectMapper.convertValue(cachedValue, clazz);

        } catch (IOException e) {
            log.warn("Redis cache JSON conversion failed for key {}: {}", key, e.getMessage());
            return null;
        } catch (RuntimeException e) {
            log.warn("Redis cache conversion failed for key {}: {}", key, e.getMessage());
            return null;
        }
    }


    /**
     * 删除缓存
     * @param key 缓存键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 判断缓存是否存在
     * @param key 缓存键
     * @return true=存在，false=不存在
     */
    public boolean exists(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置缓存过期时间
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Redis缓存键不能为空");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("过期时间不能为负数");
        }
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 构建缓存key
     * @param prefix 前缀
     * @param id ID
     * @return 完整key
     */
    public static String buildKey(String prefix, Long id) {
        return StrUtil.format("health:{}:{}", prefix, id);
    }
}
