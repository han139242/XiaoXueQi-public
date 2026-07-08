package com.neuedu.ems.config;

import com.neuedu.ems.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置类：注册JWT过滤器
 */
@Configuration
public class FilterConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(jwtAuthenticationFilter);
        // 设置过滤器拦截的路径（/* 表示拦截所有请求，放行路径在过滤器内部判断）
        registrationBean.addUrlPatterns("/*");
        // 设置过滤器优先级（值越小，优先级越高）
        registrationBean.setOrder(1);
        registrationBean.setName("jwtFilter");
        return registrationBean;
    }
}