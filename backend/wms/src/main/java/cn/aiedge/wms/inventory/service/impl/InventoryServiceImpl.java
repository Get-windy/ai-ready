package cn.aiedge.wms.inventory.service.impl;

import cn.aiedge.wms.entity.WmsInventory;
import cn.aiedge.wms.entity.WmsInventoryLog;
import cn.aiedge.wms.enums.InventoryChangeType;
import cn.aiedge.wms.enums.InventoryDirection;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.inventory.mapper.WmsInventoryMapper;
import cn.aiedge.wms.inventory.mapper.WmsInventoryLogMapper;
import cn.aiedge.wms.inventory.service.InventoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper inventoryLogMapper;

    @Override
    public WmsInventory getByUniqueKey(Long productId, Long warehouseId, Long locationId, String batchNo) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventory::getProductId, productId);
        wrapper.eq(WmsInventory::getWarehouseId, warehouseId);
        wrapper.eq(WmsInventory::getLocationId, locationId);
        wrapper.eq(batchNo != null, WmsInventory::getBatchNo, batchNo);
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    public Page<WmsInventory> pageInventory(Page<WmsInventory> page, WmsInventory query) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildInventoryQueryWrapper(query);
        wrapper.orderByDesc(WmsInventory::getId);
        return inventoryMapper.selectPage(page, wrapper);
    }

    @Override
    public List<WmsInventoryLog> listLogByProductId(Long productId) {
        LambdaQueryWrapper<WmsInventoryLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventoryLog::getProductId, productId);
        wrapper.orderByDesc(WmsInventoryLog::getId);
        return inventoryLogMapper.selectList(wrapper);
    }

    @Override
    public Page<WmsInventoryLog> pageLog(Page<WmsInventoryLog> page, WmsInventoryLog query) {
        LambdaQueryWrapper<WmsInventoryLog> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsInventoryLog::getId, query.getId());
            }
            if (query.getTraceId() != null) {
                wrapper.eq(WmsInventoryLog::getTraceId, query.getTraceId());
            }
            if (query.getProductId() != null) {
                wrapper.eq(WmsInventoryLog::getProductId, query.getProductId());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsInventoryLog::getWarehouseId, query.getWarehouseId());
            }
            if (query.getLocationId() != null) {
                wrapper.eq(WmsInventoryLog::getLocationId, query.getLocationId());
            }
            if (query.getBatchNo() != null) {
                wrapper.eq(WmsInventoryLog::getBatchNo, query.getBatchNo());
            }
            if (query.getChangeType() != null) {
                wrapper.eq(WmsInventoryLog::getChangeType, query.getChangeType());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsInventoryLog::getSourceType, query.getSourceType());
            }
            if (query.getSourceId() != null) {
                wrapper.eq(WmsInventoryLog::getSourceId, query.getSourceId());
            }
        }
        wrapper.orderByDesc(WmsInventoryLog::getId);
        return inventoryLogMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increase(Long productId, Long warehouseId, Long locationId, String batchNo,
                         BigDecimal quantity, String traceId, String sourceType,
                         Long sourceId, String sourceNo, Long operatorId, String operatorName) {
        log.info("库存增加: productId={}, warehouseId={}, quantity={}, traceId={}",
                productId, warehouseId, quantity, traceId);

        WmsInventory inventory = selectForUpdate(productId, warehouseId, locationId, batchNo);

        BigDecimal beforeQty;
        BigDecimal beforeAvailable;

        if (inventory == null) {
            inventory = new WmsInventory();
            inventory.setProductId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setLocationId(locationId);
            inventory.setBatchNo(batchNo);
            inventory.setQuantity(quantity);
            inventory.setAvailableQuantity(quantity);
            inventory.setFrozenQuantity(BigDecimal.ZERO);
            beforeQty = BigDecimal.ZERO;
            beforeAvailable = BigDecimal.ZERO;
            inventoryMapper.insert(inventory);
            log.debug("新增库存记录: productId={}, locationId={}", productId, locationId);
        } else {
            beforeQty = inventory.getQuantity();
            beforeAvailable = inventory.getAvailableQuantity();
            inventory.setQuantity(inventory.getQuantity().add(quantity));
            inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(quantity));
            int affected = inventoryMapper.updateById(inventory);
            if (affected == 0) {
                throw new WmsBusinessException("库存更新失败，数据已被修改");
            }
        }

        recordLog(traceId, productId, warehouseId, locationId, batchNo,
                InventoryChangeType.INBOUND, InventoryDirection.IN, quantity,
                beforeQty, inventory.getQuantity(),
                beforeAvailable, inventory.getAvailableQuantity(),
                sourceType, sourceId, sourceNo, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrease(Long productId, Long warehouseId, Long locationId, String batchNo,
                         BigDecimal quantity, String traceId, String sourceType,
                         Long sourceId, String sourceNo, Long operatorId, String operatorName) {
        log.info("库存扣减: productId={}, warehouseId={}, quantity={}, traceId={}",
                productId, warehouseId, quantity, traceId);

        WmsInventory inventory = selectForUpdate(productId, warehouseId, locationId, batchNo);
        if (inventory == null) {
            throw new WmsBusinessException("库存记录不存在，无法扣减");
        }
        if (inventory.getAvailableQuantity().compareTo(quantity) < 0) {
            throw WmsBusinessException.insufficientStock(
                    inventory.getProductCode(), inventory.getLocationCode());
        }

        BigDecimal beforeQty = inventory.getQuantity();
        BigDecimal beforeAvailable = inventory.getAvailableQuantity();

        inventory.setQuantity(inventory.getQuantity().subtract(quantity));
        inventory.setAvailableQuantity(inventory.getAvailableQuantity().subtract(quantity));

        int affected = inventoryMapper.updateById(inventory);
        if (affected == 0) {
            throw new WmsBusinessException("库存扣减失败，数据已被修改");
        }

        recordLog(traceId, productId, warehouseId, locationId, batchNo,
                InventoryChangeType.OUTBOUND, InventoryDirection.OUT, quantity,
                beforeQty, inventory.getQuantity(),
                beforeAvailable, inventory.getAvailableQuantity(),
                sourceType, sourceId, sourceNo, operatorId, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freeze(Long productId, Long warehouseId, Long locationId, String batchNo,
                       BigDecimal quantity, String traceId, String sourceType,
                       Long sourceId, Long operatorId, String operatorName) {
        log.info("库存冻结: productId={}, warehouseId={}, quantity={}, traceId={}",
                productId, warehouseId, quantity, traceId);

        WmsInventory inventory = selectForUpdate(productId, warehouseId, locationId, batchNo);
        if (inventory == null) {
            throw new WmsBusinessException("库存记录不存在，无法冻结");
        }
        if (inventory.getAvailableQuantity().compareTo(quantity) < 0) {
            throw WmsBusinessException.insufficientStock(
                    inventory.getProductCode(), inventory.getLocationCode());
        }

        BigDecimal beforeAvailable = inventory.getAvailableQuantity();
        BigDecimal beforeFrozen = inventory.getFrozenQuantity();

        inventory.setAvailableQuantity(inventory.getAvailableQuantity().subtract(quantity));
        inventory.setFrozenQuantity(inventory.getFrozenQuantity().add(quantity));

        int affected = inventoryMapper.updateById(inventory);
        if (affected == 0) {
            throw new WmsBusinessException("库存冻结失败，数据已被修改");
        }

        BigDecimal afterAvailable = inventory.getAvailableQuantity();
        BigDecimal afterFrozen = inventory.getFrozenQuantity();

        WmsInventoryLog log = new WmsInventoryLog();
        log.setTraceId(traceId);
        log.setProductId(productId);
        log.setWarehouseId(warehouseId);
        log.setLocationId(locationId);
        log.setBatchNo(batchNo);
        log.setChangeType(InventoryChangeType.FREEZE);
        log.setDirection(InventoryDirection.OUT);
        log.setQuantity(quantity);
        log.setBeforeQuantity(beforeAvailable);
        log.setAfterQuantity(afterAvailable);
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        inventoryLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(Long productId, Long warehouseId, Long locationId, String batchNo,
                         BigDecimal quantity, String traceId, String sourceType,
                         Long sourceId, Long operatorId, String operatorName) {
        log.info("库存解冻: productId={}, warehouseId={}, quantity={}, traceId={}",
                productId, warehouseId, quantity, traceId);

        WmsInventory inventory = selectForUpdate(productId, warehouseId, locationId, batchNo);
        if (inventory == null) {
            throw new WmsBusinessException("库存记录不存在，无法解冻");
        }
        if (inventory.getFrozenQuantity().compareTo(quantity) < 0) {
            throw new WmsBusinessException(
                    String.format("冻结库存不足: 当前冻结量=%s, 需解冻=%s",
                            inventory.getFrozenQuantity(), quantity));
        }

        BigDecimal beforeAvailable = inventory.getAvailableQuantity();
        BigDecimal beforeFrozen = inventory.getFrozenQuantity();

        inventory.setAvailableQuantity(inventory.getAvailableQuantity().add(quantity));
        inventory.setFrozenQuantity(inventory.getFrozenQuantity().subtract(quantity));

        int affected = inventoryMapper.updateById(inventory);
        if (affected == 0) {
            throw new WmsBusinessException("库存解冻失败，数据已被修改");
        }

        BigDecimal afterAvailable = inventory.getAvailableQuantity();
        BigDecimal afterFrozen = inventory.getFrozenQuantity();

        WmsInventoryLog log = new WmsInventoryLog();
        log.setTraceId(traceId);
        log.setProductId(productId);
        log.setWarehouseId(warehouseId);
        log.setLocationId(locationId);
        log.setBatchNo(batchNo);
        log.setChangeType(InventoryChangeType.UNFREEZE);
        log.setDirection(InventoryDirection.IN);
        log.setQuantity(quantity);
        log.setBeforeQuantity(beforeAvailable);
        log.setAfterQuantity(afterAvailable);
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        inventoryLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long productId, Long warehouseId, Long fromLocationId, Long toLocationId,
                     String batchNo, BigDecimal quantity, String traceId, String sourceType,
                     Long sourceId, Long operatorId, String operatorName) {
        log.info("库存移库: productId={}, warehouseId={}, from={}, to={}, quantity={}, traceId={}",
                productId, warehouseId, fromLocationId, toLocationId, quantity, traceId);

        // 源库位扣减
        WmsInventory fromInventory = selectForUpdate(productId, warehouseId, fromLocationId, batchNo);
        if (fromInventory == null) {
            throw new WmsBusinessException("源库位库存记录不存在");
        }
        if (fromInventory.getAvailableQuantity().compareTo(quantity) < 0) {
            throw WmsBusinessException.insufficientStock(
                    fromInventory.getProductCode(), fromInventory.getLocationCode());
        }

        BigDecimal fromBeforeQty = fromInventory.getQuantity();
        BigDecimal fromBeforeAvailable = fromInventory.getAvailableQuantity();

        fromInventory.setQuantity(fromInventory.getQuantity().subtract(quantity));
        fromInventory.setAvailableQuantity(fromInventory.getAvailableQuantity().subtract(quantity));

        int affected = inventoryMapper.updateById(fromInventory);
        if (affected == 0) {
            throw new WmsBusinessException("源库位库存扣减失败，数据已被修改");
        }

        // 目标库位增加
        WmsInventory toInventory = selectForUpdate(productId, warehouseId, toLocationId, batchNo);
        BigDecimal toBeforeQty;
        BigDecimal toBeforeAvailable;

        if (toInventory == null) {
            toInventory = new WmsInventory();
            toInventory.setProductId(productId);
            toInventory.setWarehouseId(warehouseId);
            toInventory.setLocationId(toLocationId);
            toInventory.setBatchNo(batchNo);
            toInventory.setQuantity(quantity);
            toInventory.setAvailableQuantity(quantity);
            toInventory.setFrozenQuantity(BigDecimal.ZERO);
            toBeforeQty = BigDecimal.ZERO;
            toBeforeAvailable = BigDecimal.ZERO;
            // 复制源库位的商品信息
            toInventory.setProductCode(fromInventory.getProductCode());
            toInventory.setProductName(fromInventory.getProductName());
            toInventory.setProductSpec(fromInventory.getProductSpec());
            toInventory.setProductUnit(fromInventory.getProductUnit());
            inventoryMapper.insert(toInventory);
        } else {
            toBeforeQty = toInventory.getQuantity();
            toBeforeAvailable = toInventory.getAvailableQuantity();
            toInventory.setQuantity(toInventory.getQuantity().add(quantity));
            toInventory.setAvailableQuantity(toInventory.getAvailableQuantity().add(quantity));
            affected = inventoryMapper.updateById(toInventory);
            if (affected == 0) {
                throw new WmsBusinessException("目标库位库存更新失败，数据已被修改");
            }
        }

        // 记录日志 - 源库位出库
        recordLog(traceId, productId, warehouseId, fromLocationId, batchNo,
                InventoryChangeType.MOVE, InventoryDirection.OUT, quantity,
                fromBeforeQty, fromInventory.getQuantity(),
                fromBeforeAvailable, fromInventory.getAvailableQuantity(),
                sourceType, sourceId, null, operatorId, operatorName);

        // 记录日志 - 目标库位入库
        recordLog(traceId, productId, warehouseId, toLocationId, batchNo,
                InventoryChangeType.MOVE, InventoryDirection.IN, quantity,
                toBeforeQty, toInventory.getQuantity(),
                toBeforeAvailable, toInventory.getAvailableQuantity(),
                sourceType, sourceId, null, operatorId, operatorName);
    }

    // ==================== 私有方法 ====================

    /**
     * 悲观锁查询库存记录 (SELECT ... FOR UPDATE)
     */
    private WmsInventory selectForUpdate(Long productId, Long warehouseId, Long locationId, String batchNo) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventory::getProductId, productId);
        wrapper.eq(WmsInventory::getWarehouseId, warehouseId);
        wrapper.eq(WmsInventory::getLocationId, locationId);
        wrapper.eq(batchNo != null, WmsInventory::getBatchNo, batchNo);
        wrapper.last("FOR UPDATE");
        return inventoryMapper.selectOne(wrapper);
    }

    /**
     * 构建库存分页查询条件
     */
    private LambdaQueryWrapper<WmsInventory> buildInventoryQueryWrapper(WmsInventory query) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsInventory::getId, query.getId());
            }
            if (query.getProductId() != null) {
                wrapper.eq(WmsInventory::getProductId, query.getProductId());
            }
            if (query.getProductCode() != null) {
                wrapper.like(WmsInventory::getProductCode, query.getProductCode());
            }
            if (query.getProductName() != null) {
                wrapper.like(WmsInventory::getProductName, query.getProductName());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsInventory::getWarehouseId, query.getWarehouseId());
            }
            if (query.getWarehouseName() != null) {
                wrapper.like(WmsInventory::getWarehouseName, query.getWarehouseName());
            }
            if (query.getLocationId() != null) {
                wrapper.eq(WmsInventory::getLocationId, query.getLocationId());
            }
            if (query.getLocationCode() != null) {
                wrapper.like(WmsInventory::getLocationCode, query.getLocationCode());
            }
            if (query.getBatchNo() != null) {
                wrapper.eq(WmsInventory::getBatchNo, query.getBatchNo());
            }
            if (query.getSupplierId() != null) {
                wrapper.eq(WmsInventory::getSupplierId, query.getSupplierId());
            }
        }
        return wrapper;
    }

    /**
     * 记录库存异动日志
     */
    private void recordLog(String traceId, Long productId, Long warehouseId, Long locationId,
                           String batchNo, Integer changeType, Integer direction,
                           BigDecimal quantity, BigDecimal beforeQty, BigDecimal afterQty,
                           BigDecimal beforeAvailable, BigDecimal afterAvailable,
                           String sourceType, Long sourceId, String sourceNo,
                           Long operatorId, String operatorName) {
        WmsInventoryLog log = new WmsInventoryLog();
        log.setTraceId(traceId);
        log.setProductId(productId);
        log.setWarehouseId(warehouseId);
        log.setLocationId(locationId);
        log.setBatchNo(batchNo);
        log.setChangeType(changeType);
        log.setDirection(direction);
        log.setQuantity(quantity);
        log.setBeforeQuantity(beforeQty);
        log.setAfterQuantity(afterQty);
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setSourceNo(sourceNo);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        inventoryLogMapper.insert(log);
    }
}
