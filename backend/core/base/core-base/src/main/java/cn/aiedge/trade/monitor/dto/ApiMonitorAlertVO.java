package cn.aiedge.trade.monitor.dto;

/**
 * 异常告警项（《API监控开发文档》§3.5.3「告警阈值」）
 *
 * <p>由**真实指标 + 配置化阈值**判定，不产生任何模拟告警；无数据时不报警。</p>
 *
 * @param alertType    告警类型: ERROR_RATE / P95_LATENCY / FAIL_COUNT / SYNC_FAILED / DEPENDENCY
 * @param level        级别: CRITICAL / WARN
 * @param title        标题
 * @param detail       明细（当前值 vs 阈值）
 * @param currentValue 当前值
 * @param threshold    阈值
 * @param unit         单位（% / ms / 次 / 条）
 * @param silenced     是否处于静默期（静默期内已发过事件，本次未重复外发）
 * @param suggestion   处置建议
 */
public record ApiMonitorAlertVO(
        String alertType,
        String level,
        String title,
        String detail,
        String currentValue,
        String threshold,
        String unit,
        boolean silenced,
        String suggestion) {
}
