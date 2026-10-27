package com.company.pms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pmsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Project Management System API")
                .version("1.0")
                .description("Backend REST API for users, projects, members, tasks, "
                        + "labels, comments, issues and reports. No authentication."));
    }
}