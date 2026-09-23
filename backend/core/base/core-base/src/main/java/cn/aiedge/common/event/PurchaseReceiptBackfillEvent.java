package cn.aiedge.common.event;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 采购订单收货回写请求事件（进程内）。
 *
 * <p><b>用途：</b>WMS 收货确认完成后，需要把本次实收净增量累加回采购订单明细的「已收数量」。
 * 原实现由 WMS 用裸 {@code HttpClient} 调 ERP 的 {@code POST /api/erp/purchase/order/received}，
 * 但该端点在 {@code SaInterceptor} 的登录拦截范围内、内部调用不带 Authorization ⇒ 必然 401，
 * 而 WMS 侧把非 200 直接抛异常，导致 {@code confirmReceipt} 事务整体回滚 ——
 * <b>凡关联采购订单的收货单都无法确认完成</b>。现改为发布本事件由 core-api 编排回写。</p>
 *
 * <p><b>为什么由 core-api 监听：</b>依赖方向要求 wms 不依赖 erp-purchase（避免双向耦合），
 * 而 core-api 聚合全部模块依赖，是既定的跨模块编排层（先例：{@code QualityDefectDispositionService}）。</p>
 *
 * <p><b>事务性：</b>监听方为同线程同步监听（{@code @EventListener} + {@code @Transactional}），
 * 异常会传播回发布方事务 ⇒ 保持与原「回写失败即回滚」一致的强一致语义。</p>
 */
public class PurchaseReceiptBackfillEvent {

    /** 采购订单主键 */
    private final Long purchaseOrderId;

    /** 按商品聚合后的实收净增量（已扣破损），至少一行 */
    private final List<Line> items;

    /** 来源收货任务单号，仅用于日志追踪 */
    private final String sourceNo;

    public PurchaseReceiptBackfillEvent(Long purchaseOrderId, List<Line> items, String sourceNo) {
        this.purchaseOrderId = purchaseOrderId;
        this.items = items == null ? Collections.emptyList() : items;
        this.sourceNo = sourceNo;
    }

    public Long getPurchaseOrderId() { return purchaseOrderId; }

    public List<Line> getItems() { return items; }

    public String getSourceNo() { return sourceNo; }

    /** 一行回写：某商品本次实收净增量 */
    public static class Line {

        private final Long productId;

        private final BigDecimal receivedQuantity;

        public Line(Long productId, BigDecimal receivedQuantity) {
            this.productId = productId;
            this.receivedQuantity = receivedQuantity;
        }

        public Long getProductId() { return productId; }

        public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    }
}
