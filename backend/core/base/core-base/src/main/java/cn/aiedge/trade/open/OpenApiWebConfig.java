package cn.aiedge.trade.open;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 开放 API（`/api/open/**`）验签装配。
 *
 * <p><b>拦截顺序</b>：`order = 50` —— 排在 Sa-Token 认证拦截器（`SaTokenConfig` 的 0 / 1）之后、
 * API 监控埋点（`ApiMonitorWebConfig` 的 100）之前。</p>
 *
 * <p>⚠️ 本拦截器能生效的前提是 `/api/open/**` 已被 `SaTokenConfig` 加入**两份**
 * `excludePathPatterns` 白名单（SecurityInterceptor 与 SaInterceptor）—— 否则未登录请求会先被
 * Sa-Token 401，验签逻辑根本轮不到执行。两处必须同改，见该文件注释。</p>
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiWebConfig implements WebMvcConfigurer {

    private final OpenApiAuthInterceptor openApiAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiAuthInterceptor)
                .addPathPatterns("/api/open/**")
                .order(50);
    }
}
