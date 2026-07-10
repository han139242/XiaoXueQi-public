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
                .title("羽毛球 MES 系统 API 文档")  // 文档标题
                .version("1.0.0")              // 接口版本
                .description("基于 Spring Boot 3.x 和 Swagger 3.x 的羽毛球 MES 后端接口文档")  // 文档描述
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
    public GroupedOpenApi mesApi() {
        return GroupedOpenApi.builder()
                .group("MES 后端接口")
                .pathsToMatch("/api/**")
                .packagesToScan("com.neuedu.ems.controller")
                .build();
    }
}
