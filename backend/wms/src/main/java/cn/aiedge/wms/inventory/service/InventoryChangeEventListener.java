package cn.aiedge.wms.inventory.service;

import cn.aiedge.common.event.InventoryChangeEvent;
import cn.aiedge.wms.inventory.service.InventoryService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存变动请求事件监听（进程内）。
 * <p>ERP 侧发布 {@link InventoryChangeEvent}（其他出入库/采购入库等），本监听统一经
 * {@link InventoryService#increase/decrease} 过账——WMS 是库存唯一写入口（双写 wms_inventory + 镜像 erp_stock），
 * ERP 侧不再直写 erp_stock（A 方案红线）。</p>
 */
@Component
public class InventoryChangeEventListener {

    private final InventoryService inventoryService;

    public InventoryChangeEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onInventoryChange(InventoryChangeEvent e) {
        if (e.getQuantity() == null || e.getQuantity().signum() == 0) {
            return;
        }
        if (e.getType() == InventoryChangeEvent.ChangeType.INCREASE) {
            inventoryService.increase(e.getProductId(), e.getWarehouseId(), e.getLocationId(), e.getBatchNo(),
                    e.getQuantity(), java.util.UUID.randomUUID().toString(), e.getSourceType(), e.getSourceId(), e.getSourceNo(),
                    e.getOperatorId(), e.getOperatorName());
        } else {
            inventoryService.decrease(e.getProductId(), e.getWarehouseId(), e.getLocationId(), e.getBatchNo(),
                    e.getQuantity(), java.util.UUID.randomUUID().toString(), e.getSourceType(), e.getSourceId(), e.getSourceNo(),
                    e.getOperatorId(), e.getOperatorName());
        }
    }
}
