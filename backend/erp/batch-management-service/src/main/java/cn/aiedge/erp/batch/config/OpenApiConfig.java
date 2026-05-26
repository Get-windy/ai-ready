package cn.aiedge.erp.batch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenAPI/Swagger配置类
 * 配置批次管理API的文档信息
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/batch}")
    private String contextPath;

    @Value("${spring.application.name:batch-management-service}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI batchManagementOpenAPI() {
        // API基本信息
        Info info = new Info()
                .title("ERP批次管理API")
                .version("1.0.0")
                .description("""
                        ERP批次管理服务REST API接口文档
                        
                        ## 功能概述
                        - **批次管理**: 批次的增删改查、状态管理、流转操作
                        - **批次操作**: 批次转移、质检、拆分、合并
                        - **批次流转**: 入库、出库、库存调整
                        - **批次追溯**: 批次溯源、历史记录查询
                        - **批次预警**: 过期预警、库存预警、质量预警
                        
                        ## 技术特性
                        - **RESTful API**: 遵循RESTful设计规范
                        - **OpenAPI 3.0**: 标准API文档规范
                        - **统一响应格式**: 标准化的API响应结构
                        - **错误处理**: 统一的错误码和错误信息
                        
                        ## 环境信息
                        - 应用名称: %s
                        - 环境: %s
                        - 上下文路径: %s
                        """.formatted(applicationName, activeProfile, contextPath))
                .contact(new Contact()
                        .name("ERP开发团队")
                        .email("erp-dev@aiedge.cn")
                        .url("https://docs.aiedge.cn/erp/batch"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"))
                .termsOfService("https://aiedge.cn/terms");

        // API标签（分组）
        List<Tag> tags = Arrays.asList(
                new Tag().name("批次管理").description("批次的基础增删改查操作"),
                new Tag().name("批次状态").description("批次状态变更和管理操作"),
                new Tag().name("批次操作").description("批次的转移、质检、拆分、合并等操作"),
                new Tag().name("批次流转").description("批次的入库、出库等流转操作"),
                new Tag().name("批次查询").description("批次的搜索、过滤、统计查询"),
                new Tag().name("批次预警").description("批次过期预警、库存预警等"),
                new Tag().name("公共接口").description("公共的API接口")
        );

        // 服务器配置
        List<Server> servers = Arrays.asList(
                new Server()
                        .url("http://localhost:8082" + contextPath)
                        .description("本地开发环境"),
                new Server()
                        .url("https://erp-dev.aiedge.cn" + contextPath)
                        .description("开发测试环境"),
                new Server()
                        .url("https://erp.aiedge.cn" + contextPath)
                        .description("生产环境")
        );

        // 安全配置（如果需要）
        // io.swagger.v3.oas.models.security.SecurityScheme securityScheme = new SecurityScheme()
        //         .type(SecurityScheme.Type.HTTP)
        //         .scheme("bearer")
        //         .bearerFormat("JWT");

        // io.swagger.v3.oas.models.Components components = new Components()
        //         .addSecuritySchemes("bearerAuth", securityScheme);

        // io.swagger.v3.oas.models.security.SecurityRequirement securityRequirement = new SecurityRequirement()
        //         .addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .tags(tags)
                .servers(servers);
                // .components(components)
                // .security(List.of(securityRequirement));
    }
}