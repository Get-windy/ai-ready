package cn.aiedge.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class SimpleTestApp {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleTestApp.class, args);
    }
    
    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"service\":\"ai-ready-test-app\"}";
    }
    
    @GetMapping("/test")
    public String test() {
        return "{\"message\":\"Test API is working\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
    }
    
    @GetMapping("/actuator/health")
    public String actuatorHealth() {
        return "{\"status\":\"UP\",\"components\":{\"db\":{\"status\":\"UP\"},\"redis\":{\"status\":\"UP\"}}}";
    }
}