package cn.aiedge.dms.tracking.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.dms.tracking.dto.RiderLocationQuery;
import cn.aiedge.dms.tracking.dto.RiderLocationVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 实时位置推送（《实时跟踪开发文档》§3.6 下发端：SSE 增量推送，避免前端轮询）
 *
 * <p>订阅方（前端 `views/dms/realtime-tracking`）以 `fetch` 流式读取 SSE（携带标准
 * `Authorization` 头，**不留鉴权例外**）；服务端每 {@code dms.tracking.stream.interval-ms}
 * （默认 5 秒）推送一帧「在线配送员位置」，前端按 riderId 合并到列表；
 * 30 秒兜底轮询保留，通道不可用时自动降级为轮询。</p>
 *
 * <p>多租户：订阅时按请求线程的租户上下文分组，推送帧按租户隔离，避免跨租户串数据。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingStreamService {

    /** 单帧最多推送的在线配送员数（防止大租户帧过大） */
    private static final int STREAM_BATCH_SIZE = 200;

    /** SSE 连接超时（毫秒）：到期后前端自动重连；避免连接无限悬挂 */
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final TrackingService trackingService;

    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    private record Subscriber(SseEmitter emitter, Long tenantId) {
    }

    /** 建立订阅（在请求线程中调用；租户上下文由其直接读取） */
    public SseEmitter subscribe() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Subscriber subscriber = new Subscriber(emitter, tenantId);
        subscribers.add(subscriber);
        emitter.onCompletion(() -> subscribers.remove(subscriber));
        emitter.onTimeout(() -> subscribers.remove(subscriber));
        emitter.onError(e -> subscribers.remove(subscriber));
        try {
            emitter.send(SseEmitter.event().name("ready")
                    .data("{\"intervalMs\":5000}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            subscribers.remove(subscriber);
            log.debug("[实时跟踪] SSE 建连即断开，已移除订阅");
        }
        log.info("[实时跟踪] SSE 订阅建立，当前订阅数={}，租户={}", subscribers.size(), tenantId);
        return emitter;
    }

    /** 按租户推送在线配送员位置帧 */
    @Scheduled(fixedDelayString = "${dms.tracking.stream.interval-ms:5000}")
    public void push() {
        if (subscribers.isEmpty()) {
            return;
        }
        Map<Long, List<Subscriber>> byTenant = subscribers.stream()
                .collect(Collectors.groupingBy(s -> s.tenantId() == null ? 0L : s.tenantId(),
                        LinkedHashMap::new, Collectors.toList()));
        byTenant.forEach((tenantId, group) -> {
            List<Map<String, Object>> frame;
            try {
                MyBatisPlusConfig.setTempTenantId(tenantId);
                frame = onlineFrame();
            } catch (Exception e) {
                log.warn("[实时跟踪] SSE 帧构造失败，tenant={}: {}", tenantId, e.getMessage());
                return;
            } finally {
                MyBatisPlusConfig.clearTempTenantId();
            }
            for (Subscriber sub : group) {
                try {
                    sub.emitter().send(SseEmitter.event().name("locations")
                            .data(frame, MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    subscribers.remove(sub);
                    try {
                        sub.emitter().complete();
                    } catch (Exception ignore) {
                        // 已断开
                    }
                }
            }
        });
    }

    /** 在线配送员位置轻量帧（复用位置聚合口径：在线判定走 dms_rider.last_report_time） */
    private List<Map<String, Object>> onlineFrame() {
        RiderLocationQuery query = new RiderLocationQuery();
        query.setPageNum(1);
        query.setPageSize(STREAM_BATCH_SIZE);
        query.setOnlineOnly(true);
        Page<RiderLocationVO> page = trackingService.riderPage(query);
        List<Map<String, Object>> list = new ArrayList<>();
        for (RiderLocationVO vo : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("riderId", vo.getRiderId());
            item.put("riderName", vo.getRiderName());
            item.put("online", vo.getOnline());
            item.put("lat", vo.getLat());
            item.put("lng", vo.getLng());
            item.put("speed", vo.getSpeed());
            item.put("direction", vo.getDirection());
            item.put("directionText", vo.getDirectionText());
            item.put("accuracy", vo.getAccuracy());
            item.put("address", vo.getAddress());
            item.put("lastReportTime", vo.getLastReportTime());
            item.put("lastReportAgoSeconds", vo.getLastReportAgoSeconds());
            list.add(item);
        }
        return list;
    }
}
