package com.neuedu.ems.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * HTTP请求工具配置
 */
@Configuration
public class OkHttpConfig {
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时30秒
                .readTimeout(120, TimeUnit.SECONDS)   // 读取超时120秒（流式输出需要更长时间）
                .writeTimeout(30, TimeUnit.SECONDS)  // 写入超时30秒
                .build();
    }
}