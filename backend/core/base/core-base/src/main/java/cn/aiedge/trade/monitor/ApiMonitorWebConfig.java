package cn.aiedge.trade.monitor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 监控埋点装配：把开放接口（`/api/open/**`）纳入调用日志
 *
 * <p>`order = 100` 排在认证拦截器（`SaTokenConfig` 的 0/1）之后——
 * 埋点只覆盖**通过认证**的开放接口调用；被认证拦下的请求无租户上下文、写入器会跳过（口径见开发文档）。</p>
 *
 * <p>只挂 `/api/open/**`：管理端 `/api/trade/**` 不是「外部接口」，不计入调用量，避免指标被自身页面请求污染。</p>
 */
@Configuration
@RequiredArgsConstructor
public class ApiMonitorWebConfig implements WebMvcConfigurer {

    private final ApiCallLogRecorder apiCallLogRecorder;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiCallLogInterceptor(apiCallLogRecorder))
                .addPathPatterns("/api/open/**")
                .order(100);
    }
}
