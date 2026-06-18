package cn.aiedge.wms.inventory.service;

import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.entity.WmsInventoryLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    WmsInventory getByUniqueKey(Long productId, Long warehouseId, Long locationId, String batchNo);
    Page<WmsInventory> pageInventory(Page<WmsInventory> page, WmsInventory query);
    List<WmsInventoryLog> listLogByProductId(Long productId);
    Page<WmsInventoryLog> pageLog(Page<WmsInventoryLog> page, WmsInventoryLog query);
    // 核心操作
    void increase(Long productId, Long warehouseId, Long locationId, String batchNo, BigDecimal quantity, String traceId, String sourceType, Long sourceId, String sourceNo, Long operatorId, String operatorName);
    void decrease(Long productId, Long warehouseId, Long locationId, String batchNo, BigDecimal quantity, String traceId, String sourceType, Long sourceId, String sourceNo, Long operatorId, String operatorName);
    void freeze(Long productId, Long warehouseId, Long locationId, String batchNo, BigDecimal quantity, String traceId, String sourceType, Long sourceId, Long operatorId, String operatorName);
    void unfreeze(Long productId, Long warehouseId, Long locationId, String batchNo, BigDecimal quantity, String traceId, String sourceType, Long sourceId, Long operatorId, String operatorName);
    void move(Long productId, Long warehouseId, Long fromLocationId, Long toLocationId, String batchNo, BigDecimal quantity, String traceId, String sourceType, Long sourceId, Long operatorId, String operatorName);
}
