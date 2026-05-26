package cn.aiedge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication(scanBasePackages = {
    "cn.aiedge.base",
    "cn.aiedge.assistant",
    "cn.aiedge.audit",
    "cn.aiedge.cache",
    "cn.aiedge.common",
    "cn.aiedge.config",
    "cn.aiedge.dict",
    "cn.aiedge.export",
    "cn.aiedge.feedback",
    "cn.aiedge.finance",
    "cn.aiedge.integration",
    "cn.aiedge.inventory",
    "cn.aiedge.knowledge",
    "cn.aiedge.monitor",
    "cn.aiedge.mq",
    "cn.aiedge.notification",
    "cn.aiedge.order",
    "cn.aiedge.report",
    "cn.aiedge.role",
    "cn.aiedge.runner",
    "cn.aiedge.scheduler",
    "cn.aiedge.search",
    "cn.aiedge.storage",
    "cn.aiedge.user"
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