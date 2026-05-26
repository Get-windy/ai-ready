package cn.aiedge.erp.delivery.config;

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
                        .title("ERP配送路线优化模块API")
                        .version("1.0.0")
                        .description("配送路线优化功能API文档：高德API集成、多点路径规划、实时路况调整、导航功能")
                        .contact(new Contact()
                                .name("AI-Ready Team")
                                .email("tech-support@aiedge.cn")));
    }
}