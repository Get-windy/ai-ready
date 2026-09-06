package cn.aiedge.wms.quality;

import cn.aiedge.quality.event.QualityInspectionCompletedEvent;
import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 质检完成监听器：按 产品+仓库+批次 下发冻结/放行指令。
 * - FAIL：冻结批次（合格前隔离不合格货）
 * - PASS：解冻批次（放行锁定）
 * - CONCESSION：留痕（不动作）
 * 质检模块只负责"下发指令"，成功与否记录日志，不参与扣减记账。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityGateListener {

    private final InventoryService inventoryService;

    @EventListener
    public void onQualityCompleted(QualityInspectionCompletedEvent event) {
        if (event.getProductId() == null || event.getWarehouseId() == null) {
            return;
        }
        String batchNo = event.getBatchNo();
        BigDecimal quantity = "FAIL".equals(event.getResult()) ? event.getFailQuantity() : event.getPassQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            return;
        }
        Long locationId = resolveLocation(event.getProductId(), event.getWarehouseId(), batchNo);
        try {
            if ("FAIL".equals(event.getResult())) {
                inventoryService.freeze(event.getProductId(), event.getWarehouseId(), locationId,
                        batchNo, quantity, event.getQualityNo(), "QUALITY",
                        event.getInspectionId(), null, null);
                log.info("[质检冻结] 质检单={} 不合格，冻结批次={} 数量={}", event.getQualityNo(), batchNo, quantity);
            } else if ("PASS".equals(event.getResult())) {
                inventoryService.unfreeze(event.getProductId(), event.getWarehouseId(), locationId,
                        batchNo, quantity, event.getQualityNo(), "QUALITY",
                        event.getInspectionId(), null, null);
                log.info("[质检放行] 质检单={} 合格，解冻批次={} 数量={}", event.getQualityNo(), batchNo, quantity);
            }
            // CONCESSION：让步接收，留痕不动作
        } catch (Exception ex) {
            log.warn("[质检冻结] 指令下发失败，质检单={}，原因={}", event.getQualityNo(), ex.getMessage());
        }
    }

    /** 按 仓库+产品+批次 定位默认库位（用于 WMS 库位级冻结） */
    private Long resolveLocation(Long productId, Long warehouseId, String batchNo) {
        try {
            List<WmsInventory> list = inventoryService.listAvailableBatch(productId, warehouseId);
            if (list == null || list.isEmpty()) {
                return null;
            }
            if (batchNo != null) {
                return list.stream()
                        .filter(i -> batchNo.equals(i.getBatchNo()))
                        .map(WmsInventory::getLocationId)
                        .findFirst()
                        .orElse(null);
            }
            return list.get(0).getLocationId();
        } catch (Exception ex) {
            return null;
        }
    }
}
