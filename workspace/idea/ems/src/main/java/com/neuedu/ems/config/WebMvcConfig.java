package com.neuedu.ems.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * WebMvc配置类：解决跨域问题
 * @Configuration：标识为Spring配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.root-path}")
    private String uploadRootPath;

    @Value("${file.upload.temp-root-path}")
    private String tempUploadRootPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(toResourceLocation(uploadRootPath))
                .setCachePeriod(3600)
                .resourceChain(true);


        registry.addResourceHandler("/temp/**")
                .addResourceLocations(toResourceLocation(tempUploadRootPath))
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    private String toResourceLocation(String path) {
        String location = Path.of(path).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
