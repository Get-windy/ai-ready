package cn.aiedge.wms.event.service;

import cn.aiedge.wms.entity.WmsEventOutbox;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * WMS 事件发件箱服务。
 *
 * <p><b>历史说明（2026-09-23 改造）：</b>此前的 {@code processPendingEvents()} 是个
 * 每 10 秒轮询的定时任务，用裸 {@code HttpClient} 把事件 POST 到 ERP 回调端点
 * （{@code /api/erp/wms/*}）。该端点在 Sa-Token 登录拦截范围内、内部调用不带 Authorization
 * ⇒ 每次必然 401，重试 3 次后置 FAILED；而接收端 {@code ErpCallbackServiceImpl}
 * 四个方法都只打日志（空壳），整条链路既无生产者也无实际业务 ⇒ 已移除派发与接收端，
 * 只保留发件箱的写入 / 查询 / 手动重置能力。
 *
 * <p>跨模块的实际联动一律走进程内 Spring 事件（先例：{@code InventoryChangeEvent}、
 * {@code PurchaseReceiptBackfillEvent}）。</p>
 */
public interface EventService {
    void publishEvent(String traceId, String eventType, String payload);
    boolean save(WmsEventOutbox event);
    Page<WmsEventOutbox> page(Page<WmsEventOutbox> page, WmsEventOutbox query);
    void retryEvent(Long eventId);
}
