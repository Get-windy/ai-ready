package cn.aiedge.webhook.config;

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
                        .title("Webhook机制API")
                        .version("1.0.0")
                        .description("事件推送通知机制，支持订阅系统事件并接收通知")
                        .contact(new Contact()
                                .name("AI-Ready Team")
                                .email("tech-support@aiedge.cn")));
    }
}