package cn.aiedge.trade.monitor;

import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 开放接口（`/api/open/**`）调用日志拦截器
 *
 * <p>埋点**统一下沉在拦截器**，不在每个 Controller 里逐处埋点（《API监控开发文档》§3.4 埋点方式）。</p>
 *
 * <p>口径：</p>
 * <ul>
 *   <li>`api_path` 记 **URI 模板**（如 `/api/open/inventory/sync/{channelCode}`），便于按接口聚合；</li>
 *   <li>`api_name` 取 {@link OpenApiCatalog} 中文名，未登记回退处理方法名；</li>
 *   <li>`channel_code` 取路径变量，其次查询参数；</li>
 *   <li>状态：未抛异常且响应码 &lt; 400 → SUCCESS，否则 FAIL；`error_msg` 记异常摘要（脱敏截断）；</li>
 *   <li>`request_id` 由本拦截器生成并**回写响应头 `X-Request-Id`**，供调用方对账；</li>
 *   <li>本拦截器排在认证拦截器之后：**只记录通过认证的调用**（被 401 拦下的请求因无租户上下文不落库，见写入器）。</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ApiCallLogInterceptor implements HandlerInterceptor {

    /** 请求属性：开始时间 */
    private static final String ATTR_START = ApiCallLogInterceptor.class.getName() + ".START";
    /** 请求属性：请求号 */
    private static final String ATTR_REQUEST_ID = ApiCallLogInterceptor.class.getName() + ".REQUEST_ID";

    private final ApiCallLogRecorder recorder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        String requestId = recorder.newRequestId();
        request.setAttribute(ATTR_REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(ATTR_START);
        if (!(start instanceof Long startMs)) {
            return;
        }
        int costMs = (int) Math.max(System.currentTimeMillis() - startMs, 0);
        int httpStatus = response.getStatus();
        boolean success = ex == null && httpStatus < 400;

        String path = resolvePath(request);
        String apiName = resolveApiName(request, handler, path);
        String channelCode = resolveChannelCode(request);

        ApiAccessLog logRow = recorder.build(
                ApiCallDirection.IN.name(), channelCode, path, apiName,
                request.getMethod(), success, httpStatus, costMs,
                ex != null ? ex.getClass().getSimpleName() : (success ? null : String.valueOf(httpStatus)),
                ex != null ? ex.getMessage() : null);
        logRow.setRequestId((String) request.getAttribute(ATTR_REQUEST_ID));
        logRow.setRequestParams(recorder.maskQueryString(request.getQueryString()));
        logRow.setIpAddress(resolveClientIp(request));
        logRow.setUserAgent(recorder.truncateUserAgent(request.getHeader("User-Agent")));
        logRow.setAccessTime(LocalDateTime.now());
        recorder.record(logRow);
    }

    /** URI 模板优先（可按接口聚合）；取不到时回退实际路径 */
    private String resolvePath(HttpServletRequest request) {
        Object best = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = best != null ? best.toString() : request.getRequestURI();
        return recorder.maskText(path);
    }

    private String resolveApiName(HttpServletRequest request, Object handler, String path) {
        return OpenApiCatalog.nameOfPath(path).orElseGet(() ->
                handler instanceof HandlerMethod method ? method.getMethod().getName() : null);
    }

    @SuppressWarnings("unchecked")
    private String resolveChannelCode(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars instanceof Map<?, ?> map) {
            Object channel = ((Map<String, Object>) map).get("channelCode");
            if (channel != null) {
                return recorder.maskText(channel.toString());
            }
        }
        return recorder.maskText(request.getParameter("channelCode"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }
}
