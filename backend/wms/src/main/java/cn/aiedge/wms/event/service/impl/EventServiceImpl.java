package cn.aiedge.wms.event.service.impl;

import cn.aiedge.wms.entity.WmsEventOutbox;
import cn.aiedge.wms.enums.EventStatus;
import cn.aiedge.wms.event.mapper.WmsEventOutboxMapper;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.event.service.EventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final WmsEventOutboxMapper eventOutboxMapper;
    private final ObjectMapper objectMapper;

    @Value("${wms.erp.base-url:http://localhost:5655}")
    private String erpBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(String traceId, String eventType, String payload) {
        WmsEventOutbox event = new WmsEventOutbox();
        event.setTraceId(traceId != null ? traceId : UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(eventType);
        event.setSourceSystem("WMS");
        event.setTargetSystem("ERP");
        event.setPayload(payload);
        event.setStatus(EventStatus.PENDING);
        event.setRetryCount(0);
        event.setMaxRetry(3);
        event.setNextRetryTime(LocalDateTime.now().plusSeconds(10));
        eventOutboxMapper.insert(event);
        log.info("事件已发布: traceId={}, eventType={}", event.getTraceId(), eventType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsEventOutbox event) {
        return eventOutboxMapper.insert(event) > 0;
    }

    @Override
    public Page<WmsEventOutbox> page(Page<WmsEventOutbox> page, WmsEventOutbox query) {
        LambdaQueryWrapper<WmsEventOutbox> wrapper = new LambdaQueryWrapper<>();
        if (query.getEventType() != null) wrapper.eq(WmsEventOutbox::getEventType, query.getEventType());
        if (query.getStatus() != null) wrapper.eq(WmsEventOutbox::getStatus, query.getStatus());
        if (query.getTraceId() != null) wrapper.eq(WmsEventOutbox::getTraceId, query.getTraceId());
        wrapper.orderByDesc(WmsEventOutbox::getCreateTime);
        return eventOutboxMapper.selectPage(page, wrapper);
    }

    @Override
    @Scheduled(fixedDelay = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void processPendingEvents() {
        // 查询待发送事件（到重试时间的）
        LambdaQueryWrapper<WmsEventOutbox> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsEventOutbox::getStatus, EventStatus.PENDING);
        wrapper.le(WmsEventOutbox::getNextRetryTime, LocalDateTime.now());
        wrapper.orderByAsc(WmsEventOutbox::getCreateTime);
        List<WmsEventOutbox> pendingList = eventOutboxMapper.selectList(wrapper);

        for (WmsEventOutbox event : pendingList) {
            try {
                String url = getErpCallbackUrl(event.getEventType());
                if (url == null) {
                    log.warn("未知事件类型，跳过: {}", event.getEventType());
                    event.setStatus(EventStatus.FAILED);
                    event.setLastError("未知事件类型");
                    eventOutboxMapper.updateById(event);
                    continue;
                }

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(erpBaseUrl + url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(event.getPayload() != null ? event.getPayload() : "{}"))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 201) {
                    event.setStatus(EventStatus.SUCCESS);
                    event.setCompletedTime(LocalDateTime.now());
                    event.setLastError(null);
                    log.info("事件发送成功: traceId={}, eventType={}", event.getTraceId(), event.getEventType());
                } else {
                    handleFailure(event, "HTTP " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception e) {
                handleFailure(event, e.getMessage());
                log.error("事件发送失败: traceId={}, error={}", event.getTraceId(), e.getMessage());
            }
            eventOutboxMapper.updateById(event);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedEvents() {
        LambdaQueryWrapper<WmsEventOutbox> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsEventOutbox::getStatus, EventStatus.FAILED);
        wrapper.lt(WmsEventOutbox::getRetryCount, 3);
        List<WmsEventOutbox> failedList = eventOutboxMapper.selectList(wrapper);
        for (WmsEventOutbox event : failedList) {
            event.setStatus(EventStatus.PENDING);
            event.setNextRetryTime(LocalDateTime.now().plusSeconds(10));
            eventOutboxMapper.updateById(event);
        }
        if (!failedList.isEmpty()) {
            log.info("已重置 {} 条失败事件为待发送", failedList.size());
        }
    }

    private String getErpCallbackUrl(String eventType) {
        return switch (eventType) {
            case "RECEIPT_COMPLETED" -> "/api/erp/wms/receipt-complete";
            case "SHIP_COMPLETED" -> "/api/erp/wms/ship-complete";
            case "INVENTORY_CHANGE" -> "/api/erp/wms/inventory-change";
            case "CHECK_DIFF" -> "/api/erp/wms/check-diff";
            default -> null;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryEvent(Long eventId) {
        WmsEventOutbox event = eventOutboxMapper.selectById(eventId);
        if (event == null) {
            throw new WmsBusinessException("事件不存在: " + eventId);
        }
        event.setStatus(EventStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryTime(LocalDateTime.now().plusSeconds(5));
        eventOutboxMapper.updateById(event);
        log.info("事件已重置为待重试: eventId={}", eventId);
    }

    private void handleFailure(WmsEventOutbox event, String error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(error);
        if (event.getRetryCount() >= event.getMaxRetry()) {
            event.setStatus(EventStatus.FAILED);
            event.setLastError(error + " (已重试" + event.getRetryCount() + "次)");
        } else {
            event.setNextRetryTime(LocalDateTime.now().plusSeconds(event.getRetryCount() * 20L));
        }
    }
}
