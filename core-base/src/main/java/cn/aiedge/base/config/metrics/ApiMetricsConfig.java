package cn.aiedge.base.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 应用层监控指标配置
 * 
 * 指标类型：
 * 1. HTTP请求量 - api_request_total
 * 2. 响应时间 - api_response_time
 * 3. 错误率 - api_error_total
 * 4. 活跃请求 - api_active_requests
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Configuration
public class ApiMetricsConfig {

    public static final String API_REQUEST_TOTAL = "ai_ready_api_request_total";
    public static final String API_RESPONSE_TIME = "ai_ready_api_response_time";
    public static final String API_ERROR_TOTAL = "ai_ready_api_error_total";
    public static final String API_ACTIVE_REQUESTS = "ai_ready_api_active_requests";

    @Bean
    public Counter.Builder apiRequestCounterBuilder(MeterRegistry registry) {
        return Counter.builder(API_REQUEST_TOTAL)
                .description("Total number of API requests")
                .tags("service", "ai-ready");
    }

    @Bean
    public Timer.Builder apiResponseTimerBuilder(MeterRegistry registry) {
        return Timer.builder(API_RESPONSE_TIME)
                .description("API response time in milliseconds")
                .tags("service", "ai-ready")
                .minimumExpectedDuration(Duration.ofMillis(1))
                .maximumExpectedDuration(Duration.ofSeconds(30));
    }

    @Bean
    public Counter.Builder apiErrorCounterBuilder(MeterRegistry registry) {
        return Counter.builder(API_ERROR_TOTAL)
                .description("Total number of API errors")
                .tags("service", "ai-ready");
    }
}