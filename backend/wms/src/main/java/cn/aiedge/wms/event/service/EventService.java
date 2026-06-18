package cn.aiedge.wms.event.service;

import cn.aiedge.wms.entity.WmsEventOutbox;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface EventService {
    void publishEvent(String traceId, String eventType, String payload);
    boolean save(WmsEventOutbox event);
    Page<WmsEventOutbox> page(Page<WmsEventOutbox> page, WmsEventOutbox query);
    void processPendingEvents();
    void retryFailedEvents();
    void retryEvent(Long eventId);
}
