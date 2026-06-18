package cn.aiedge.common.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化日志转换器
 * 将日志输出为JSON格式，便于日志采集和分析
 * 
 * 用法：在logback-spring.xml中配置
 * <conversionRule conversionWord="structured" converterClass="cn.aiedge.common.log.StructuredLogConverter"/>
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class StructuredLogConverter extends MessageConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    @Override
    public String convert(ILoggingEvent event) {
        try {
            Map<String, Object> logMap = new LinkedHashMap<>();
            
            // 基础信息
            logMap.put("timestamp", DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
            logMap.put("level", event.getLevel().toString());
            logMap.put("logger", event.getLoggerName());
            logMap.put("thread", event.getThreadName());
            logMap.put("message", event.getFormattedMessage());
            
            // MDC上下文信息
            Map<String, String> mdc = event.getMDCPropertyMap();
            if (mdc != null && !mdc.isEmpty()) {
                // 提取常用追踪字段
                if (mdc.containsKey("traceId")) {
                    logMap.put("traceId", mdc.get("traceId"));
                }
                if (mdc.containsKey("spanId")) {
                    logMap.put("spanId", mdc.get("spanId"));
                }
                if (mdc.containsKey("userId")) {
                    logMap.put("userId", mdc.get("userId"));
                }
                if (mdc.containsKey("tenantId")) {
                    logMap.put("tenantId", mdc.get("tenantId"));
                }
                if (mdc.containsKey("requestId")) {
                    logMap.put("requestId", mdc.get("requestId"));
                }
                
                // 其他MDC信息放入extra
                Map<String, String> extra = new LinkedHashMap<>();
                mdc.forEach((key, value) -> {
                    if (!isCommonField(key)) {
                        extra.put(key, value);
                    }
                });
                if (!extra.isEmpty()) {
                    logMap.put("extra", extra);
                }
            }
            
            // 异常信息
            if (event.getThrowableProxy() != null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("type", event.getThrowableProxy().getClassName());
                error.put("message", event.getThrowableProxy().getMessage());
                error.put("stackTrace", getStackTrace(event));
                logMap.put("error", error);
            }
            
            return objectMapper.writeValueAsString(logMap);
            
        } catch (Exception e) {
            // 降级为普通文本
            return event.getFormattedMessage();
        }
    }

    private boolean isCommonField(String key) {
        return "traceId".equals(key) || "spanId".equals(key) || 
               "userId".equals(key) || "tenantId".equals(key) || 
               "requestId".equals(key);
    }

    private String getStackTrace(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        var proxy = event.getThrowableProxy();
        if (proxy != null && proxy.getStackTraceElementProxyArray() != null) {
            int limit = 10; // 限制堆栈行数
            var elements = proxy.getStackTraceElementProxyArray();
            for (int i = 0; i < Math.min(limit, elements.length); i++) {
                sb.append(elements[i].getSTEAsString()).append("\n");
            }
            if (elements.length > limit) {
                sb.append("... ").append(elements.length - limit).append(" more");
            }
        }
        return sb.toString();
    }
}
