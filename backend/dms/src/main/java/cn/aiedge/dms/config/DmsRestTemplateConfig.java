package cn.aiedge.dms.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置（用于调用外部地图 API 和 ERP/WMS 服务）
 * 注意：使用特定的 bean 名称避免与 common 模块的 RestTemplateConfig 冲突
 *
 * @author AI-Ready Team
 */
@Configuration
public class DmsRestTemplateConfig {

    @Bean("dmsRestTemplate")
    public RestTemplate dmsRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
