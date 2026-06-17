package cn.aiedge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP 客户端配置
 * <p>
 * 配置 RestTemplate 用于报表数据源的 API 调用等场景。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
