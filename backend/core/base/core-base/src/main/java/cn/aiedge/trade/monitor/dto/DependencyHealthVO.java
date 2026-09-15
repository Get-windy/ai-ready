package cn.aiedge.trade.monitor.dto;

/**
 * 依赖项健康（《API监控开发文档》§3.1「依赖健康面板」/ §3.5.2「依赖健康分级」）
 *
 * <p>整站 UP/DOWN 意义有限，故**逐依赖**返回：状态、探测耗时、最近错误摘要。</p>
 *
 * @param key           依赖键（db / redis / mq / map / channel:TAOBAO …）
 * @param name          中文名
 * @param category      类别: DATABASE / CACHE / MQ / MAP / CHANNEL
 * @param status        UP 正常 / DOWN 异常 / NOT_CONFIGURED 未接入或未配置
 * @param latencyMs     探测耗时(ms)，未探测为 null
 * @param detail        说明（服务商、Key 来源、渠道启用数等）
 * @param lastErrorTime 最近失败时间（可为空）
 * @param lastError     最近错误摘要（可为空）
 */
public record DependencyHealthVO(
        String key,
        String name,
        String category,
        String status,
        Integer latencyMs,
        String detail,
        String lastErrorTime,
        String lastError) {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";
}
