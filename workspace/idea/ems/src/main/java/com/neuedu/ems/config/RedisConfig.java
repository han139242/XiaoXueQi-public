package com.neuedu.ems.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Constructor;

@Configuration
public class RedisConfig {

    /**
     *
     *
     *
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        // Key序列化器（不变）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Value序列化器：反射创建Jackson2JsonRedisSerializer实例，规避废弃提示
        RedisSerializer<Object> jacksonSerializer = getJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(jacksonSerializer);
        redisTemplate.setHashValueSerializer(jacksonSerializer);

        redisTemplate.afterPropertiesSet();


        return redisTemplate;
    }



    /**
     * 反射创建Jackson2JsonRedisSerializer实例，避免直接导入触发废弃提示
     */
    @SuppressWarnings("unchecked")
    private RedisSerializer<Object> getJackson2JsonRedisSerializer() {
        try {
            // 反射获取Jackson2JsonRedisSerializer类
            Class<?> serializerClass = Class.forName("org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer");
            // 创建构造器（参数为Object.class）
            Constructor<?> constructor = serializerClass.getConstructor(Class.class);
            // 创建实例
            RedisSerializer<Object> serializer = (RedisSerializer<Object>) constructor.newInstance(Object.class);

            // 配置ObjectMapper（与之前逻辑一致）
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

            // 反射设置ObjectMapper
            serializerClass.getMethod("setObjectMapper", ObjectMapper.class).invoke(serializer, objectMapper);

            return serializer;
        } catch (Exception e) {
            // 反射失败时，降级使用GenericToStringSerializer
            return new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Object.class);
        }
    }
}
