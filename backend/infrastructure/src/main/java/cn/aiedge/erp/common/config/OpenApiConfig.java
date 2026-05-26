package cn.aiedge.erp.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI统一配置类
 * 
 * <p>为ERP系统提供统一的API文档配置，包括：
 * <ul>
 *   <li>系统基本信息</li>
 *   <li>联系人信息</li>
 *   <li>许可证信息</li>
 *   <li>服务器环境配置</li>
 *   <li>全局标签定义</li>
 * </ul>
 * 
 * <p>所有业务模块继承此配置，确保API文档的一致性和标准化。
 * 
 * @author ERP开发团队
 * @version 1.0
 * @since 2026-05-05
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ERP系统 - 企业资源计划管理系统",
        version = "1.0.0",
        description = """
            ## ERP系统API文档
            
            ### 概述
            ERP系统（Enterprise Resource Planning）提供全面的企业资源管理功能，
            包括采购、销售、库存、财务、客户管理等模块。
            
            ### 主要功能模块
            - **采购管理**: 采购订单、供应商管理、询价报价
            - **销售管理**: 销售订单、客户管理、价格策略
            - **库存管理**: 批次管理、序列号管理、库存调拨
            - **财务管理**: 账务处理、财务报表、费用管理
            - **客户管理**: 客户信息、联系人、销售机会
            
            ### 技术栈
            - **后端框架**: Spring Boot 3.2.5 + Java 17
            - **数据库**: PostgreSQL 16
            - **API文档**: SpringDoc OpenAPI 3.0
            - **安全认证**: JWT + Spring Security
            """,
        contact = @Contact(
            name = "ERP开发团队",
            email = "erp-team@aiedge.cn",
            url = "https://erp.aiedge.cn/contact"
        ),
        license = @License(
            name = "企业私有协议 v2.0",
            url = "https://erp.aiedge.cn/license",
            identifier = "ERP-PRIVATE-2.0"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "本地开发环境",
            variables = {
                @io.swagger.v3.oas.annotations.servers.ServerVariable(
                    name = "protocol",
                    defaultValue = "http",
                    allowableValues = {"http", "https"},
                    description = "协议类型"
                )
            }
        ),
        @Server(
            url = "https://dev.erp.aiedge.cn",
            description = "开发测试环境",
            variables = {
                @io.swagger.v3.oas.annotations.servers.ServerVariable(
                    name = "env",
                    defaultValue = "dev",
                    description = "环境标识"
                )
            }
        ),
        @Server(
            url = "https://staging.erp.aiedge.cn",
            description = "预发布环境",
            variables = {
                @io.swagger.v3.oas.annotations.servers.ServerVariable(
                    name = "env",
                    defaultValue = "staging",
                    description = "环境标识"
                )
            }
        ),
        @Server(
            url = "https://erp.aiedge.cn",
            description = "生产环境",
            variables = {
                @io.swagger.v3.oas.annotations.servers.ServerVariable(
                    name = "env",
                    defaultValue = "prod",
                    description = "环境标识"
                )
            }
        )
    },
    tags = {
        @Tag(
            name = "批次管理",
            description = "批次号生成、批次属性管理、批次库存操作"
        ),
        @Tag(
            name = "采购管理",
            description = "采购订单、供应商协同、采购询价报价"
        ),
        @Tag(
            name = "销售管理",
            description = "销售订单、客户管理、销售价格策略"
        ),
        @Tag(
            name = "库存管理",
            description = "库存盘点、库存调拨、库存预警"
        ),
        @Tag(
            name = "财务管理",
            description = "账务处理、财务报表、费用报销"
        ),
        @Tag(
            name = "客户管理",
            description = "客户信息、联系人管理、销售机会跟踪"
        ),
        @Tag(
            name = "供应商管理",
            description = "供应商信息、资质管理、绩效评估"
        ),
        @Tag(
            name = "订单管理",
            description = "订单创建、订单跟踪、订单变更"
        ),
        @Tag(
            name = "费用管理",
            description = "费用报销、费用审批、费用统计"
        ),
        @Tag(
            name = "发票管理",
            description = "发票开具、发票核销、发票查询"
        ),
        @Tag(
            name = "监控指标",
            description = "系统监控、业务指标、性能分析"
        ),
        @Tag(
            name = "系统监控",
            description = "健康检查、性能监控、告警管理"
        )
    }
)
public class OpenApiConfig {
    
    /**
     * 配置说明：
     * 1. 此配置类被所有ERP模块继承
     * 2. 通过SpringDoc自动扫描生成API文档
     * 3. 统一了所有模块的API文档样式和标准
     * 4. 支持多环境服务器配置
     * 
     * 使用方法：
     * 1. 在控制器类上使用@Tag注解指定模块标签
     * 2. 在API方法上使用@Operation注解描述操作
     * 3. 在DTO类上使用@Schema注解描述数据结构
     * 4. 自动生成交互式API文档
     */
    
    // SpringDoc会自动扫描所有配置，无需额外代码
}