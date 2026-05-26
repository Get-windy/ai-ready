package cn.aiedge.erp.batchsn.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 配置类
 * 
 * @author team-member
 * @date 2026-05-05
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI batchSnOpenAPI() {
        return new OpenAPI()
                // 基本信息
                .info(new Info()
                        .title("ERP批次管理与序列号系统 API")
                        .description("ERP批次管理与序列号系统的完整API文档，提供批次创建、查询、入库、出库、转移、盘点等操作接口。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI-Edge开发团队")
                                .email("support@ai-edge.cn")
                                .url("https://ai-edge.cn"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                
                // 服务器信息
                .servers(List.of(
                    new Server()
                        .url("http://localhost:8080")
                        .description("本地开发环境"),
                    new Server()
                        .url("https://erp.ai-edge.cn/api")
                        .description("生产环境")
                ))
                
                // 外部文档链接
                .externalDocs(new ExternalDocumentation()
                        .description("ERP批次管理系统详细文档")
                        .url("https://docs.ai-edge.cn/erp/batch-management"))
                
                // 安全方案
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer Token认证")))
                
                // 全局安全要求
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                
                // 预定义响应
                .components(new Components()
                        .addResponses("SuccessResponse", new ApiResponse()
                                .description("成功响应")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>()
                                                .$ref("#/components/schemas/ApiResponse")))))
                        .addResponses("ErrorResponse", new ApiResponse()
                                .description("错误响应")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>()
                                                .$ref("#/components/schemas/ApiResponse")))))
                        .addResponses("NotFoundResponse", new ApiResponse()
                                .description("资源未找到")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>()
                                                .$ref("#/components/schemas/ApiResponse")))))
                        .addResponses("ValidationErrorResponse", new ApiResponse()
                                .description("参数验证错误")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>()
                                                .$ref("#/components/schemas/ApiResponse"))))));
    }
    
    /**
     * 配置示例操作的OpenAPI文档
     */
    @Bean
    public OpenAPI addExampleOperations(OpenAPI openApi) {
        // 添加批次创建操作的示例
        Operation createBatchOperation = new Operation()
                .summary("创建批次")
                .description("创建一个新的批次记录，支持批量创建和自动生成批次号")
                .addTagsItem("批次管理")
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse()
                                .description("批次创建成功")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().example("{\n" +
                                                "  \"code\": 201,\n" +
                                                "  \"message\": \"批次创建成功\",\n" +
                                                "  \"data\": {\n" +
                                                "    \"id\": 1,\n" +
                                                "    \"batchNo\": \"B202605050001\",\n" +
                                                "    \"productCode\": \"P001\",\n" +
                                                "    \"status\": \"ACTIVE\",\n" +
                                                "    \"totalQuantity\": 100.00\n" +
                                                "  }\n" +
                                                "}")))
                                .$ref("#/components/responses/SuccessResponse"))
                        .addApiResponse("400", new ApiResponse()
                                .description("参数验证失败")
                                .$ref("#/components/responses/ValidationErrorResponse"))
                        .addApiResponse("401", new ApiResponse()
                                .description("未授权访问")
                                .$ref("#/components/responses/ErrorResponse")));
        
        // 添加批次查询操作的示例
        Operation getBatchOperation = new Operation()
                .summary("查询批次详情")
                .description("根据批次ID查询批次详细信息，支持缓存")
                .addTagsItem("批次管理")
                .addParametersItem(new Parameter()
                        .name("id")
                        .in("path")
                        .required(true)
                        .description("批次ID")
                        .schema(new Schema<>().type("integer").format("int64")))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("查询成功")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().example("{\n" +
                                                "  \"code\": 200,\n" +
                                                "  \"message\": \"成功\",\n" +
                                                "  \"data\": {\n" +
                                                "    \"id\": 1,\n" +
                                                "    \"batchNo\": \"B202605050001\",\n" +
                                                "    \"productCode\": \"P001\",\n" +
                                                "    \"productName\": \"测试产品\",\n" +
                                                "    \"status\": \"ACTIVE\",\n" +
                                                "    \"totalQuantity\": 100.00,\n" +
                                                "    \"availableQuantity\": 100.00,\n" +
                                                "    \"warehouseName\": \"主仓库\",\n" +
                                                "    \"productionDate\": \"2026-05-01\",\n" +
                                                "    \"expirationDate\": \"2027-05-01\"\n" +
                                                "  }\n" +
                                                "}")))
                                .$ref("#/components/responses/SuccessResponse"))
                        .addApiResponse("404", new ApiResponse()
                                .description("批次未找到")
                                .$ref("#/components/responses/NotFoundResponse")));
        
        // 添加到OpenAPI
        openApi.getPaths()
                .addPathItem("/api/erp/batch-sn/batches", new PathItem().post(createBatchOperation))
                .addPathItem("/api/erp/batch-sn/batches/{id}", new PathItem().get(getBatchOperation));
        
        return openApi;
    }
}