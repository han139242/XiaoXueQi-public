package com.neuedu.ems.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 3.x 全局配置类
 * SpringBoot 3.x 中无需添加 @EnableOpenApi（自动生效）
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 核心信息
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            // API 文档基本信息
            .info(new Info()
                .title("东软个人健康管理系统的 API 文档")  // 文档标题
                .version("1.0.0")              // 接口版本
                .description("这是基于 SpringBoot 3.5.11 + Swagger 3.x 实现的用户管理系统接口文档，包含用户的增删改查功能")  // 文档描述
                // 联系人信息
                .contact(new Contact()
                        .name("开发团队")
                        .email("maintainer@example.org")
                        .url("https://example.com"))
                // 许可证信息
                .license(new License()
                        .name("Apache 2.0 许可证")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理")
                .pathsToMatch("/user/**")
                .packagesToScan("com.neuedu.health.controller")
                .build();
    }

    @Bean
    public GroupedOpenApi captchaApi() {
        return GroupedOpenApi.builder()
                .group("验证码")
                .pathsToMatch("/captcha/**")
                .packagesToScan("com.neuedu.health.controller")
                .build();
    }
}