package com.neuedu.ems.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc配置类：解决跨域问题
 * @Configuration：标识为Spring配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {





    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:D:/upload/file/")
                .setCachePeriod(3600)
                .resourceChain(true);


        registry.addResourceHandler("/temp/**")
                .addResourceLocations("file:D:/upload/temp/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }
}