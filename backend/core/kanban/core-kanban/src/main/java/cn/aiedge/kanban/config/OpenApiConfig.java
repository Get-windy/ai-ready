package cn.aiedge.kanban.config;

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
                        .title("看板视图API")
                        .version("1.0.0")
                        .description("Odoo核心特性：卡片式任务管理视图，支持拖拽排序、分组显示")
                        .contact(new Contact()
                                .name("AI-Ready Team")
                                .email("tech-support@aiedge.cn")));
    }
}