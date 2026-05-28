package cn.aiedge.gateway;

import cn.aiedge.gateway.config.GatewayConfig;
import cn.aiedge.gateway.config.properties.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

/**
 * API网关启动类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewayProperties.class)
@Import({
    GatewayConfig.class
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
