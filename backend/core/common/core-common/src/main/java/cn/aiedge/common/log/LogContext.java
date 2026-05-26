package cn.aiedge.common.log;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

/**
 * 日志上下文工具类
 * 管理MDC中的追踪信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class LogContext {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String USER_ID = "userId";
    private static final String TENANT_ID = "tenantId";
    private static final String REQUEST_ID = "requestId";
    private static final String MODULE = "module";
    private static final String ACTION = "action";

    /**
     * 初始化追踪上下文（请求入口调用）
     */
    public static void initTrace() {
        MDC.put(TRACE_ID, generateId());
        MDC.put(SPAN_ID, generateId());
    }

    /**
     * 设置追踪ID（从请求头获取时使用）
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * 获取追踪ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 设置Span ID
     */
    public static void setSpanId(String spanId) {
        if (spanId != null) {
            MDC.put(SPAN_ID, spanId);
        }
    }

    /**
     * 设置用户ID
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * 设置租户ID
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID, tenantId);
        }
    }

    /**
     * 设置请求ID
     */
    public static void setRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID, requestId);
        }
    }

    /**
     * 设置模块名
     */
    public static void setModule(String module) {
        if (module != null) {
            MDC.put(MODULE, module);
        }
    }

    /**
     * 设置操作名
     */
    public static void setAction(String action) {
        if (action != null) {
            MDC.put(ACTION, action);
        }
    }

    /**
     * 添加自定义字段
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * 获取自定义字段
     */
    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * 获取所有上下文信息
     */
    public static Map<String, String> getContextMap() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 清除上下文（请求结束时调用）
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 生成唯一ID
     */
    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
