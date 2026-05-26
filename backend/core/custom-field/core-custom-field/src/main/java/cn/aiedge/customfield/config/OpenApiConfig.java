package cn.aiedge.customfield.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("动态字段系统API")
                        .version("1.0.0")
                        .description("Odoo核心特性：运行时动态添加自定义字段，支持多租户隔离")
                        .contact(new Contact()
                                .name("AI-Ready Team")
                                .email("tech-support@aiedge.cn")));
    }
}