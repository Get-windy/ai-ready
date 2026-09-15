package cn.aiedge.trade.monitor;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.trade.monitor.entity.ApiAccessLog;
import cn.aiedge.trade.monitor.mapper.ApiAccessLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 接口调用日志写入器（入站拦截器 / 渠道出站 / 联调沙箱共用）
 *
 * <p>设计要点：</p>
 * <ol>
 *   <li>**写日志绝不影响主流程**：全部异常吞掉并 debug 记录；</li>
 *   <li>**最小落库面**：只落「脱敏后的查询串」与「截断摘要」，**不落请求/响应体**（避免客户隐私与凭据入库）；</li>
 *   <li>**租户口径**：无租户上下文（未登录线程）直接跳过，避免 NOT NULL 违约与「0 号租户」脏数据；</li>
 *   <li>**可追溯**：每次调用带 `request_id`（入站回写响应头 `X-Request-Id`），可与渠道对账。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiCallLogRecorder {

    private final ApiAccessLogMapper apiAccessLogMapper;

    /** 敏感参数名关键字（命中即掩码） */
    private static final String[] SENSITIVE_KEYS = {
            "token", "secret", "password", "passwd", "pwd", "key", "sign", "signature", "authorization", "credential"
    };

    /** JSON 中敏感键的值掩码（"accessToken":"xxx" → "accessToken":"***"） */
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(\"[A-Za-z0-9_]*(?:token|secret|password|passwd|pwd|key|sign)[A-Za-z0-9_]*\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-]+");

    private static final int MAX_PARAMS = 500;
    private static final int MAX_BODY = 2000;
    private static final int MAX_MSG = 400;
    private static final int MAX_UA = 200;

    /** 生成请求号 */
    public String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 记录一次调用（失败静默）
     *
     * @param row 日志行（tenantId 由本方法填充；direction/status/accessTime 必填）
     */
    public void record(ApiAccessLog row) {
        if (row == null) {
            return;
        }
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenantId == null) {
            // 无租户上下文（未登录线程/被认证拦截的请求）：不落库，避免 tenant_id NOT NULL 违约与脏数据
            log.debug("[API监控] 无租户上下文，跳过调用日志: path={}", row.getApiPath());
            return;
        }
        try {
            row.setTenantId(tenantId);
            if (row.getAccessTime() == null) {
                row.setAccessTime(LocalDateTime.now());
            }
            if (row.getStatus() == null) {
                row.setStatus(ApiCallStatus.SUCCESS.name());
            }
            if (row.getDirection() == null) {
                row.setDirection(ApiCallDirection.IN.name());
            }
            apiAccessLogMapper.insert(row);
        } catch (Exception e) {
            // 监控埋点不允许影响业务
            log.warn("[API监控] 写入调用日志失败: path={}, err={}", row.getApiPath(), e.getMessage());
        }
    }

    /** 快捷方法：入站 */
    public ApiAccessLog build(String direction, String channelCode, String apiPath, String apiName,
                              String method, boolean success, Integer responseCode, int costMs,
                              String errorCode, String errorMsg) {
        ApiAccessLog logRow = new ApiAccessLog();
        logRow.setDirection(direction);
        logRow.setChannelCode(channelCode);
        logRow.setApiPath(apiPath);
        logRow.setApiName(apiName);
        logRow.setRequestMethod(method);
        logRow.setStatus(success ? ApiCallStatus.SUCCESS.name() : ApiCallStatus.FAIL.name());
        logRow.setResponseCode(responseCode);
        logRow.setResponseTime(costMs);
        logRow.setErrorCode(errorCode);
        logRow.setErrorMsg(truncate(errorMsg, MAX_MSG));
        return logRow;
    }

    /** 查询串脱敏（`token=abc&sku=1` → `token=***&sku=1`） */
    public String maskQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return null;
        }
        StringBuilder masked = new StringBuilder();
        for (String pair : queryString.split("&")) {
            if (masked.length() > 0) {
                masked.append('&');
            }
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                masked.append(pair);
                continue;
            }
            String name = pair.substring(0, eq);
            String value = pair.substring(eq + 1);
            masked.append(name).append('=').append(isSensitive(name) ? "***" : maskText(value));
        }
        return truncate(masked.toString(), MAX_PARAMS);
    }

    /** 文本脱敏 + 截断（用于响应体摘要、错误信息） */
    public String maskText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String masked = BEARER.matcher(text).replaceAll("Bearer ***");
        Matcher m = JSON_SECRET.matcher(masked);
        masked = m.replaceAll("$1***$3");
        return truncate(masked, MAX_BODY);
    }

    public String truncateError(String msg) {
        return truncate(msg, MAX_MSG);
    }

    public String truncateUserAgent(String ua) {
        return truncate(ua, MAX_UA);
    }

    private static boolean isSensitive(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String key : SENSITIVE_KEYS) {
            if (lower.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
