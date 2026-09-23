package cn.aiedge.wms.event.service.impl;

import cn.aiedge.wms.entity.WmsEventOutbox;
import cn.aiedge.wms.enums.EventStatus;
import cn.aiedge.wms.event.mapper.WmsEventOutboxMapper;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.event.service.EventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final WmsEventOutboxMapper eventOutboxMapper;

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
}
