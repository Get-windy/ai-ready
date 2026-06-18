package cn.aiedge.dms.event.service;

import cn.aiedge.dms.event.entity.DmsEventOutbox;
import cn.aiedge.dms.event.mapper.DmsEventOutboxMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 事件集成服务
 *
 * 基于发件箱模式提供可靠的事件发布、异步发送和失败重试机制。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final DmsEventOutboxMapper eventOutboxMapper;
    @Qualifier("dmsRestTemplate")
    private final RestTemplate restTemplate;

    /**
     * 发布事件
     *
     * 将事件写入发件箱，初始状态为待发送。
     *
     * @param eventType 事件类型
     * @param target    目标系统标识
     * @param payload   事件载荷（JSON字符串）
     * @return 发件箱记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publishEvent(String eventType, String target, String payload) {
        DmsEventOutbox outbox = new DmsEventOutbox();
        outbox.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        outbox.setEventType(eventType);
        outbox.setSource("DMS");
        outbox.setTarget(target);
        outbox.setPayload(payload);
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        eventOutboxMapper.insert(outbox);

        log.info("事件已发布, traceId={}, eventType={}, target={}", outbox.getTraceId(), eventType, target);
        return outbox.getId();
    }

    /**
     * 定时处理待发送事件
     *
     * 每10秒扫描一次待发送事件，通过HTTP POST发送到目标系统。
     */
    @Scheduled(fixedRate = 10000)
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void processPendingEvents() {
        List<DmsEventOutbox> pendingEvents = eventOutboxMapper.findPendingEvents();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("处理待发送事件, 数量={}", pendingEvents.size());

        for (DmsEventOutbox event : pendingEvents) {
            try {
                // 构造目标URL并发送
                String url = buildTargetUrl(event.getTarget(), event.getEventType());
                String response = restTemplate.postForObject(url, event.getPayload(), String.class);

                // 发送成功
                completeEvent(event.getId());
                log.info("事件发送成功, id={}, traceId={}, target={}", event.getId(), event.getTraceId(), url);
            } catch (Exception e) {
                log.error("事件发送失败, id={}, traceId={}, error={}", event.getId(), event.getTraceId(), e.getMessage());
                failEvent(event.getId(), e.getMessage());
            }
        }
    }

    /**
     * 重试失败事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedEvents() {
        List<DmsEventOutbox> failedEvents = eventOutboxMapper.selectList(
                new LambdaQueryWrapper<DmsEventOutbox>()
                        .eq(DmsEventOutbox::getStatus, 2)
        );

        log.info("重试失败事件, 数量={}", failedEvents.size());

        for (DmsEventOutbox event : failedEvents) {
            try {
                String url = buildTargetUrl(event.getTarget(), event.getEventType());
                restTemplate.postForObject(url, event.getPayload(), String.class);

                completeEvent(event.getId());
                log.info("失败事件重试成功, id={}, traceId={}", event.getId(), event.getTraceId());
            } catch (Exception e) {
                log.error("失败事件重试失败, id={}, traceId={}, error={}", event.getId(), event.getTraceId(), e.getMessage());
                failEvent(event.getId(), e.getMessage());
            }
        }
    }

    /**
     * 完成事件（标记为已发送）
     *
     * @param id 事件ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeEvent(Long id) {
        DmsEventOutbox event = eventOutboxMapper.selectById(id);
        if (event != null) {
            event.setStatus(1);
            event.setLastError(null);
            eventOutboxMapper.updateById(event);
        }
    }

    /**
     * 事件发送失败处理
     *
     * 增加重试次数，设置下次重试时间。
     * 超过3次重试后保持失败状态不再自动重试。
     *
     * @param id    事件ID
     * @param error 错误信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void failEvent(Long id, String error) {
        DmsEventOutbox event = eventOutboxMapper.selectById(id);
        if (event == null) {
            return;
        }

        event.setRetryCount(event.getRetryCount() == null ? 1 : event.getRetryCount() + 1);
        event.setLastError(error);
        event.setStatus(2);

        // 重试小于3次时设置下次重试时间（指数退避）
        if (event.getRetryCount() < 3) {
            event.setNextRetryTime(LocalDateTime.now().plusMinutes((long) Math.pow(2, event.getRetryCount())));
        } else {
            // 超过3次，不再自动重试
            event.setNextRetryTime(null);
        }

        eventOutboxMapper.updateById(event);
    }

    /**
     * 构造目标系统URL
     *
     * @param target    目标系统标识
     * @param eventType 事件类型
     * @return 完整URL
     */
    private String buildTargetUrl(String target, String eventType) {
        // 根据目标系统和事件类型构造URL，实际项目中应从配置中心获取
        return "http://localhost:8080/api/" + target.toLowerCase() + "/callback/" + eventType.toLowerCase();
    }
}
