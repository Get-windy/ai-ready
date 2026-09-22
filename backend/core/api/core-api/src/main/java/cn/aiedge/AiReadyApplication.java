package cn.aiedge;

import cn.aiedge.config.FullyQualifiedBeanNameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication(scanBasePackages = {
    "cn.aiedge.base",
    "cn.aiedge.cache",
    "cn.aiedge.common",
    "cn.aiedge.config",
    "cn.aiedge.api",
    "cn.aiedge.user.controller",
    "cn.aiedge.user.dto",
    "cn.aiedge.user.entity",
    "cn.aiedge.user.repository",
    "cn.aiedge.user.service",
    "cn.aiedge.dashboard",
    "cn.aiedge.tenant",
    "cn.aiedge.position",
    "cn.aiedge.department",
    "cn.aiedge.erp.purchase",
    "cn.aiedge.erp.finance",
    "cn.aiedge.erp.sale",
    "cn.aiedge.erp.stock",
    "cn.aiedge.erp.monitor",
    "cn.aiedge.erp.controller",
    "cn.aiedge.workflow",
    "cn.aiedge.erp.batchsn.controller",
    "cn.aiedge.erp.batchsn.config",
    "cn.aiedge.erp.batchsn.entity",
    "cn.aiedge.erp.batchsn.mapper",
    "cn.aiedge.erp.batchsn.service",
    "cn.aiedge.erp.supplier.controller",
    "cn.aiedge.erp.supplier.service",
    "cn.aiedge.erp.supplier.mapper",
    "cn.aiedge.erp.supplier.repository",
    "cn.aiedge.erp.supplier.model",
    "cn.aiedge.erp.supplier.dto",
    "cn.aiedge.erp.supplier.entity",
    "cn.aiedge.erp.printing",
    "cn.aiedge.erp.signature",
    "cn.aiedge.erp.delivery",
    "cn.aiedge.erp.customer",
    "cn.aiedge.erp.party",
    "cn.aiedge.erp.product.kit",
    "cn.aiedge.erp.payment",
    "cn.aiedge.erp.pricing",
    "cn.aiedge.crm",
    // 协议模块（独立 Maven 模块 backend/agreement）：漏了本行 ⇒ 控制器/服务不在容器里，
    // 表现为「页面空白 / 端点 404」（本仓实踩过，见 DOMAIN-MODEL §12.3 的第四条装配线）。
    "cn.aiedge.agreement",
    "cn.aiedge.erp.b2b",
    "cn.aiedge.quality",
    "cn.aiedge.trade",
    "cn.aiedge.erp.metrics",
    "cn.aiedge.erp.invoice",
    "cn.aiedge.erp.budget",
    "cn.aiedge.hr",
    "cn.aiedge.dms",
    "cn.aiedge.docquery",
    "cn.aiedge.erp.marketing",
    "cn.aiedge.payment",
    "cn.aiedge.automation",
    "cn.aiedge.customfield",
    "cn.aiedge.kanban",
    "cn.aiedge.permission",
    "cn.aiedge.notification",
    "cn.aiedge.dict",
    "cn.aiedge.integration",
    "cn.aiedge.wms",
    "cn.aiedge.audit",
    "cn.aiedge.erp.expense",
    "cn.aiedge.erp.fixedasset",
    // 定时任务（开发工具 → 定时任务，菜单 62405）：此前**漏配**该包 → 控制器/执行器/Mapper 全未装配，
    // 表现为「接口 404 + 执行日志 0 条 + 页面静默空列表」（2026-09-14 复核修复）
    "cn.aiedge.scheduler",
    // —— 系统模块（平台级，client_type=system-admin）后端装配（2026-09-18 复核）——
    // 以下 5 个包此前**从未进入 scanBasePackages**：@MapperScan 是通配的（见下方 :117），
    // 所以 Mapper 一直在、表能建，但 Service/Controller 不在容器里 → 表现为「表能建、点不动」的假可用状态。
    // 影响系统模块 16 页（共 19 控制器 / 126 端点）；cn.aiedge.export 还跨模块外溢——
    // 它是全站 Excel 导入/导出的后端（/api/import、/api/export、/api/import/v2）。
    // 排查口诀：页面空白 + 表里有数据 + 端点 404 → 先查 scanBasePackages。
    "cn.aiedge.module",      // 模块列表/版本/发布/使用统计（62101-62104，10 端点）
    "cn.aiedge.monitor",     // 服务状态/性能监控（62201/62202）+ 告警/基础设施（52 端点）
    "cn.aiedge.platform",    // 邮件/短信/存储/安全策略（62502-62505，11 端点）
    "cn.aiedge.datasource",  // 连接/慢查询/备份/同步/清理（62301-62305，22 端点）
    "cn.aiedge.export",      // 模板管理（62402，13 端点）+ 全站导入导出（31 端点）
    // 开发工具 → API测试（62404）的服务端安全兜底（2026-09-19 新增）：
    // 本页的 allowlist / 凭据剥离 / 调用审计必须由**服务端**成立（前端校验可被开发者工具绕过），
    // 端点 POST /api/dev/api-test/send 与 POST /api/dev/api-test/policy 都在本包。
    // 漏配本包的后果同上一段 5 个包：控制器不在容器里 → 页面点「发送请求」404。
    "cn.aiedge.devtool",
    "cn.aiedge.config"  // 添加新的配置包
}, exclude = {
    GatewayAutoConfiguration.class,
    GatewayClassPathWarningAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = {
    "cn.aiedge.erp.sales.pricing.repository",
    "cn.aiedge.erp.expense.repository",
    "cn.aiedge.erp.invoice.repository",
    "cn.aiedge.erp.supplier.notification.repository",
    "cn.aiedge.erp.metrics.repository",
    "cn.aiedge.erp.fixedasset.repository",
    "cn.aiedge.erp.budget.repository"
})
@EntityScan(basePackages = {
    "cn.aiedge.erp.sales.pricing.entity",
    "cn.aiedge.erp.expense.model",
    "cn.aiedge.erp.invoice.model.entity",
    "cn.aiedge.erp.supplier.entity",
    "cn.aiedge.erp.supplier.notification.entity",
    "cn.aiedge.erp.metrics.entity",
    "cn.aiedge.erp.fixedasset.model",
    "cn.aiedge.erp.budget.model"
})
@MapperScan(value = {"cn.aiedge.**.mapper", "cn.aiedge.**.dao", "cn.aiedge.common.serial.mapper",
        "cn.aiedge.erp.supplier.repository",
        // ⚠️ 下面这个包**不以 `.mapper`/`.dao` 结尾**，通配规则扫不到它。
        // 而本仓用了显式 `@MapperScan` ⇒ MyBatis 的"自动扫 @Mapper 接口"会**退让**
        // （AutoConfiguredMapperScannerRegistrar 在已有 MapperScan 时不生效），
        // 所以接口上写了 `@Mapper` 也**不会**被注册成 Bean ⇒ 依赖它的 @Component 起不来
        // ⇒ **整个应用启动失败**（实测 2026-09-23：`NoSuchBeanDefinitionException:
        // ReplenishmentProductMapper`，描述见 APPLICATION FAILED TO START）。
        //
        // 判据（放之四海）：**mapper 接口必须住在以 `mapper`/`dao` 命名的包里，
        // 否则必须在此显式登记**。`ComponentScanCoverageTest` 目前只校验**控制器**包，
        // 不校验 mapper 包 —— 这条就是那个门禁的盲区（待补）。
        "cn.aiedge.erp.purchase.replenishment"},
            nameGenerator = FullyQualifiedBeanNameGenerator.class)
@OpenAPIDefinition(
    info = @Info(
        title = "企智连·AI-Ready API",
        version = "1.0.0",
        description = "企业智能管理系统API文档",
        contact = @Contact(name = "AI-Ready Team", email = "dev@ai-ready.cn"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    )
)
@Slf4j
public class AiReadyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AiReadyApplication.class);
        app.setBeanNameGenerator(new FullyQualifiedBeanNameGenerator());
        app.run(args);
        log.info("""

            ========================================
            企智连·AI-Ready 启动成功！
            API文档: http://localhost:5655/doc.html
            ========================================
            """);
    }
}
