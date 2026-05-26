package cn.aiedge.erp.signature.config;

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
                        .title("ERP电子签收模块API")
                        .version("1.0.0")
                        .description("电子签收功能API文档：拍照签收、电子签名、签收记录、配送评价")
                        .contact(new Contact()
                                .name("AI-Ready Team")
                                .email("tech-support@aiedge.cn")));
    }
}