package cn.aiedge;

import cn.aiedge.config.FullyQualifiedBeanNameGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication(scanBasePackages = {
    "cn.aiedge.base",
    "cn.aiedge.common",
    "cn.aiedge.config",
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
    "cn.aiedge.finance",
    "cn.aiedge.erp.sale",
    "cn.aiedge.erp.stock",
    "cn.aiedge.erp.monitor",
    "cn.aiedge.erp.controller",
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
    "cn.aiedge.erp.product.kit",
    "cn.aiedge.erp.payment",
    "cn.aiedge.erp.pricing",
    "cn.aiedge.crm",
    "cn.aiedge.notification"
}, exclude = {
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    GatewayAutoConfiguration.class,
    GatewayClassPathWarningAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@MapperScan(value = {"cn.aiedge.**.mapper", "cn.aiedge.erp.supplier.repository"},
            nameGenerator = FullyQualifiedBeanNameGenerator.class)
@OpenAPIDefinition(
    info = @Info(
        title = "智企连·AI-Ready API",
        version = "1.0.0",
        description = "企业智能管理系统API文档",
        contact = @Contact(name = "AI-Ready Team", email = "dev@ai-ready.cn"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    )
)
public class AiReadyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AiReadyApplication.class);
        app.setBeanNameGenerator(new FullyQualifiedBeanNameGenerator());
        app.run(args);
        System.out.println("""

            ========================================
            智企连·AI-Ready 启动成功！
            API文档: http://localhost:5655/doc.html
            ========================================
            """);
    }
}
