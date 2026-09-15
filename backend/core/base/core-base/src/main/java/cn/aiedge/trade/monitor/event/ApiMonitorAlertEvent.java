package cn.aiedge.trade.monitor.event;

import java.time.LocalDateTime;

/**
 * API 监控告警事件（本系统内部事件通道的**发布端**）
 *
 * <p>core-base **不反向依赖** DMS 模块，故不直接写 `dms_event_outbox`：
 * 由本事件触发 DMS 侧桥接监听器写入发件箱（与《签收管理开发文档》的事件外发同一条通道），
 * 既保持模块单向依赖，又能被下游订阅/重投。</p>
 *
 * @param tenantId    租户
 * @param alertType   告警类型（ERROR_RATE / P95_LATENCY / FAIL_COUNT / SYNC_FAILED / DEPENDENCY）
 * @param level       级别（CRITICAL / WARN）
 * @param title       标题
 * @param detail      明细（当前值 vs 阈值）
 * @param currentValue 当前值
 * @param threshold   阈值
 * @param unit        单位
 * @param occurredAt  发生时间
 */
public record ApiMonitorAlertEvent(
        Long tenantId,
        String alertType,
        String level,
        String title,
        String detail,
        String currentValue,
        String threshold,
        String unit,
        LocalDateTime occurredAt) {
}
