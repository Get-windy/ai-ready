package cn.aiedge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简化版Spring Boot测试应用
 * 用于解除devops-engineer和test-agent-2的阻塞
 * 
 * @author AI-Ready Team
 */
@SpringBootApplication
@RestController
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
        System.out.println("""
            
            ========================================
            AI-Ready 测试应用启动成功！
            应用地址: http://localhost:8080
            健康检查: http://localhost:8080/actuator/health
            API文档: http://localhost:8080/swagger-ui.html
            ========================================
            """);
    }

    @GetMapping("/")
    public String home() {
        return "AI-Ready Test Application is running!";
    }

    @GetMapping("/api/v1/health")
    public String health() {
        return "{\"status\": \"UP\", \"message\": \"Test application is healthy\"}";
    }

    @GetMapping("/api/v1/users")
    public String getUsers() {
        return """
            [
                {"id": 1, "name": "Test User 1", "email": "user1@test.com"},
                {"id": 2, "name": "Test User 2", "email": "user2@test.com"},
                {"id": 3, "name": "Test User 3", "email": "user3@test.com"}
            ]
            """;
    }

    @GetMapping("/api/v1/products")
    public String getProducts() {
        return """
            [
                {"id": 1, "name": "Product A", "price": 99.99},
                {"id": 2, "name": "Product B", "price": 149.99},
                {"id": 3, "name": "Product C", "price": 199.99}
            ]
            """;
    }
}