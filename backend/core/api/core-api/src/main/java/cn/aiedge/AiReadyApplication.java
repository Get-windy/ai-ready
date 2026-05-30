package cn.aiedge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication(scanBasePackages = {
    "cn.aiedge.base",
    "cn.aiedge.common",
    "cn.aiedge.config",
    "cn.aiedge.user",
    "cn.aiedge.auth",
    "cn.aiedge.erp.purchase"
}, exclude = {
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    GatewayAutoConfiguration.class,
    GatewayClassPathWarningAutoConfiguration.class
})
@EnableTransactionManagement
@MapperScan("cn.aiedge.**.mapper")
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
        SpringApplication.run(AiReadyApplication.class, args);
        System.out.println("""
            
            ========================================
            智企连·AI-Ready 启动成功！
            API文档: http://localhost:8080/doc.html
            ========================================
            """);
    }
}