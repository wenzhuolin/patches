package com.patches.plm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(
                new Info()
                        .title("Patch Lifecycle Management API")
                        .version("v1")
                        .description("补丁生命周期管理系统核心API（流程/KPI/QA）")
                        .contact(new Contact().name("Architecture Team"))
        );
    }
}
