package cn.aiedge.dms.event.listener;

import cn.aiedge.dms.event.service.EventService;
import cn.aiedge.trade.monitor.event.ApiMonitorAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 监控告警 → DMS 事件通道（发件箱）桥接
 *
 * <p>《API监控开发文档》§3.5.3：告警阈值触发后「复用事件通道」。core-base 不反向依赖 DMS，
 * 故由本监听器消费 core-base 发布的 {@link ApiMonitorAlertEvent}，转写 `dms_event_outbox`
 * （event_type = `API_MONITOR_ALERT`），与《签收管理》的事件外发同一条通道，可被下游订阅/重投。</p>
 *
 * <p>静默期由 core-base 侧的告警服务判定：静默期内不会有事件发出，因此本监听器无需去重。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiMonitorAlertBridgeListener {

    /** 发件箱事件类型（下游订阅键） */
    public static final String EVENT_TYPE = "API_MONITOR_ALERT";
    /** 目标系统标识（与 SIGN 事件的 `dms:sign` 同构） */
    public static final String TARGET = "dms:monitor";

    private final EventService eventService;

    @EventListener
    public void onApiMonitorAlert(ApiMonitorAlertEvent event) {
        if (event == null) {
            return;
        }
        try {
            eventService.publishEvent(EVENT_TYPE, TARGET, toPayload(event));
            log.info("[API监控] 告警已写入事件通道: type={}, level={}, tenant={}",
                    event.alertType(), event.level(), event.tenantId());
        } catch (Exception e) {
            // 事件外发失败不影响告警展示（页面仍可看到告警）
            log.warn("[API监控] 告警写入事件通道失败: type={}, err={}", event.alertType(), e.getMessage());
        }
    }

    private String toPayload(ApiMonitorAlertEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", event.tenantId());
        payload.put("alertType", event.alertType());
        payload.put("level", event.level());
        payload.put("title", event.title());
        payload.put("detail", event.detail());
        payload.put("currentValue", event.currentValue());
        payload.put("threshold", event.threshold());
        payload.put("unit", event.unit());
        payload.put("occurredAt", event.occurredAt() == null ? null : event.occurredAt().toString());
        return toJson(payload);
    }

    /** 轻量 JSON 序列化（避免为一个事件引入额外依赖） */
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
