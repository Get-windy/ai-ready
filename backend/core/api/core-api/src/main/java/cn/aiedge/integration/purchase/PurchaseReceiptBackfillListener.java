package cn.aiedge.integration.purchase;

import cn.aiedge.common.event.PurchaseReceiptBackfillEvent;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 采购订单收货回写编排（P0 修复）。
 *
 * <p><b>解决的问题：</b>WMS 收货确认后需要把实收净增量累加回采购订单明细「已收数量」。
 * 原实现由 WMS 侧裸 {@code HttpClient} 调 {@code POST /api/erp/purchase/order/received}：
 * 该端点在 Sa-Token 的登录拦截范围内且内部调用不带 Authorization ⇒ 必然 401；
 * 而 WMS 侧对非 200 直接抛异常，导致 {@code confirmReceipt}（{@code @Transactional}）
 * <b>整体回滚 —— 凡关联采购订单的收货单都无法确认完成</b>。</p>
 *
 * <p><b>为什么监听器放在 core-api：</b>依赖方向要求 wms 不依赖 erp-purchase
 * （避免双向耦合），core-api 聚合全部模块依赖，是既定的跨模块编排层
 * （同层先例：{@code QualityDefectDispositionService}）。</p>
 *
 * <p><b>事务语义：</b>{@code @EventListener} 为同线程同步调用，本方法异常会传播回
 * 发布方事务 ⇒ 与改造前「回写失败即回滚」的强一致语义保持一致。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseReceiptBackfillListener {

    private final PurchaseOrderItemMapper purchaseOrderItemMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onReceiptBackfill(PurchaseReceiptBackfillEvent ev) {
        if (ev == null || ev.getPurchaseOrderId() == null || ev.getItems().isEmpty()) {
            return;
        }
        int touched = 0;
        for (PurchaseReceiptBackfillEvent.Line line : ev.getItems()) {
            if (line.getProductId() == null || line.getReceivedQuantity() == null
                    || line.getReceivedQuantity().signum() <= 0) {
                continue;
            }
            LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PurchaseOrderItem::getOrderId, ev.getPurchaseOrderId())
                    .eq(PurchaseOrderItem::getProductId, line.getProductId());
            List<PurchaseOrderItem> matched = purchaseOrderItemMapper.selectList(wrapper);
            for (PurchaseOrderItem poItem : matched) {
                purchaseOrderItemMapper.addReceivedQuantity(poItem.getId(), line.getReceivedQuantity());
                touched++;
            }
        }
        log.info("采购订单收货回写完成: orderId={}, 收货单={}, 回写行数={}, 命中明细行={}",
                ev.getPurchaseOrderId(), ev.getSourceNo(), ev.getItems().size(), touched);
    }
}
