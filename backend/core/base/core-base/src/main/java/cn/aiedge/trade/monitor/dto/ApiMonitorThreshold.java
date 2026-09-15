package cn.aiedge.trade.monitor.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * API 监控阈值（配置中心 `dms_config.monitor.*`，全局默认 + 租户覆盖）
 *
 * <p>「保存即热生效」：每次取用时读配置中心；缺失/非法时回落到**代码默认值**（本节即默认值清单，
 * 与迁移 V11.360.0 预置值一致），**不写死判定结果**。</p>
 *
 * @param errorRatePercent  错误率告警线(%)：当日错误率高于该值告警，默认 5
 * @param p95Ms             P95 耗时告警线(ms)，默认 2000
 * @param failCount         当日失败次数告警线(次)，默认 20
 * @param syncFailCount     库存同步失败数告警线(条)，默认 0（有失败即告警）
 * @param silenceMinutes    告警静默期(分钟)：同类型告警静默期内不重复发事件，默认 30
 * @param autoRefreshSeconds 页面自动刷新间隔(秒)，0=不自动刷新，默认 30
 * @param retentionDays     调用日志保留天数，0=不自动清理，默认 30
 */
public record ApiMonitorThreshold(
        BigDecimal errorRatePercent,
        int p95Ms,
        int failCount,
        int syncFailCount,
        int silenceMinutes,
        int autoRefreshSeconds,
        int retentionDays) {

    /** 配置键（与《配送参数》配置中心 MONITOR 分组、迁移 V11.360.0 一致） */
    public static final String KEY_ERROR_RATE = "monitor.threshold.error-rate";
    public static final String KEY_P95 = "monitor.threshold.p95-ms";
    public static final String KEY_FAIL_COUNT = "monitor.threshold.fail-count";
    public static final String KEY_SYNC_FAIL = "monitor.threshold.sync-fail-count";
    public static final String KEY_SILENCE = "monitor.threshold.silence-minutes";
    public static final String KEY_AUTO_REFRESH = "monitor.threshold.auto-refresh-seconds";
    public static final String KEY_RETENTION = "monitor.log.retention-days";

    /** 代码默认值（配置缺失时使用） */
    public static ApiMonitorThreshold defaults() {
        return new ApiMonitorThreshold(new BigDecimal("5"), 2000, 20, 0, 30, 30, 30);
    }

    /**
     * 由配置中心键值构造（缺键 → 用默认值；非法值 → 用默认值）
     *
     * @param values 配置键 → 值（可为 null）
     */
    public static ApiMonitorThreshold from(Map<String, String> values) {
        ApiMonitorThreshold d = defaults();
        if (values == null || values.isEmpty()) {
            return d;
        }
        return new ApiMonitorThreshold(
                decimalOf(values.get(KEY_ERROR_RATE), d.errorRatePercent()),
                intOf(values.get(KEY_P95), d.p95Ms()),
                intOf(values.get(KEY_FAIL_COUNT), d.failCount()),
                intOf(values.get(KEY_SYNC_FAIL), d.syncFailCount()),
                intOf(values.get(KEY_SILENCE), d.silenceMinutes()),
                intOf(values.get(KEY_AUTO_REFRESH), d.autoRefreshSeconds()),
                intOf(values.get(KEY_RETENTION), d.retentionDays()));
    }

    private static int intOf(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static BigDecimal decimalOf(String raw, BigDecimal fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
