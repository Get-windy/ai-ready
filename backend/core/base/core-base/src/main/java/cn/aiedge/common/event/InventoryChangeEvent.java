package cn.aiedge.common.event;

import java.math.BigDecimal;

/**
 * 库存变动请求事件（进程内）。
 * <p>用途：ERP 侧业务动作（其他出入库/采购入库等）不再直接写 {@code erp_stock}，
 * 而是发布本事件；WMS 侧 {@code InventoryChangeEventListener} 监听后统一经
 * {@code InventoryService.increase/decrease} 过账（唯一写入口，双写 wms_inventory + 镜像 erp_stock）。</p>
 * <p>约束：erp 侧不再直写 erp_stock（A 方案红线），库存变动只能走 InventoryService。</p>
 */
public class InventoryChangeEvent {

    public enum ChangeType { INCREASE, DECREASE }

    private final ChangeType type;
    private final Long productId;
    private final Long warehouseId;
    private final Long locationId;
    private final String batchNo;
    private final BigDecimal quantity;
    private final String sourceType;
    private final Long sourceId;
    private final String sourceNo;
    private final Long operatorId;
    private final String operatorName;

    public InventoryChangeEvent(ChangeType type, Long productId, Long warehouseId, Long locationId,
                                String batchNo, BigDecimal quantity, String sourceType, Long sourceId,
                                String sourceNo, Long operatorId, String operatorName) {
        this.type = type;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.locationId = locationId;
        this.batchNo = batchNo;
        this.quantity = quantity;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceNo = sourceNo;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }

    public ChangeType getType() { return type; }
    public Long getProductId() { return productId; }
    public Long getWarehouseId() { return warehouseId; }
    public Long getLocationId() { return locationId; }
    public String getBatchNo() { return batchNo; }
    public BigDecimal getQuantity() { return quantity; }
    public String getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public String getSourceNo() { return sourceNo; }
    public Long getOperatorId() { return operatorId; }
    public String getOperatorName() { return operatorName; }
}
