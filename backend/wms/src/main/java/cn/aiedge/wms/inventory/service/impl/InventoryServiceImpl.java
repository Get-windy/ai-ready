package cn.aiedge.wms.inventory.service.impl;

import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.StockService;
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

/**
 * WMS 库存服务（库位级明细账 wms_inventory + wms_inventory_log）。
 *
 * <p><b>双库存轨镜像决策</b>：系统存在两套库存账——本轨（WMS 域，库位级明细）与
 * erp_stock（ERP 域，仓库级汇总，由 erp-stock 模块维护）。为消除两账漂移，
 * increase/decrease 在本轨更新成功后，于同一事务内将增量按 (productId, warehouseId)
 * 镜像到 erp_stock（库位/批次维度在 ERP 轨丢弃）。</p>
 *
 * <ul>
 *   <li>increase 镜像失败（返回 false）时抛异常回滚，保持两轨一致，不允许只成功一轨；</li>
 *   <li>decrease 镜像返回 false 时<b>不阻断</b>本轨作业：历史漂移已导致 erp_stock 存量
 *       可能小于 WMS 轨，若强制回滚会让历史数据问题卡死仓库现场作业；此处仅记 error
 *       日志（含 productId/warehouseId/应扣量）作为后续对账线索，事务继续提交。</li>
 * </ul>
 *
 * <p>freeze/unfreeze/move 不改变仓库级总量（冻结是可用量内部转移、移库仅库位间转移），
 * 故不镜像。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper inventoryLogMapper;
    private final StockService stockService;
    private final ProductService productService;

    @Override
    public WmsInventory getByUniqueKey(Long productId, Long warehouseId, Long locationId, String batchNo) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventory::getProductId, productId);
        wrapper.eq(WmsInventory::getWarehouseId, warehouseId);
        // null 视为仓库级库存（无库位/无批次），显式匹配 IS NULL，避免 "= null" 永不命中导致重复插行
        wrapper.isNull(locationId == null, WmsInventory::getLocationId);
        wrapper.eq(locationId != null, WmsInventory::getLocationId, locationId);
        wrapper.isNull(batchNo == null, WmsInventory::getBatchNo);
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
            fillProductSnapshot(inventory, productId);
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

        // 镜像增量到 ERP 轨仓库级汇总账 erp_stock（同事务；库位/批次维度丢弃）。
        // 返回 false 视为两轨不一致，抛异常回滚，不允许只成功一轨。
        if (!stockService.increaseStock(productId, warehouseId, quantity)) {
            throw new WmsBusinessException(
                    String.format("ERP轨库存镜像失败(increaseStock返回false): productId=%d, warehouseId=%d, quantity=%s",
                            productId, warehouseId, quantity));
        }
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

        // 镜像扣减到 ERP 轨仓库级汇总账 erp_stock（同事务；库位/批次维度丢弃）。
        // 返回 false 表示 ERP 轨存量不足（历史漂移所致），不回滚本轨——避免历史漂移
        // 卡死仓库现场作业；记 error 日志留作后续对账线索，事务继续提交。
        if (!stockService.decreaseStock(productId, warehouseId, quantity)) {
            log.error("ERP轨库存镜像扣减失败(decreaseStock返回false，疑为历史漂移): productId={}, warehouseId={}, 应扣量={}, traceId={}",
                    productId, warehouseId, quantity, traceId);
        }
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
        // null 视为仓库级库存（无库位/无批次），显式匹配 IS NULL，避免 "= null" 永不命中导致重复插行
        wrapper.isNull(locationId == null, WmsInventory::getLocationId);
        wrapper.eq(locationId != null, WmsInventory::getLocationId, locationId);
        wrapper.isNull(batchNo == null, WmsInventory::getBatchNo);
        wrapper.eq(batchNo != null, WmsInventory::getBatchNo, batchNo);
        wrapper.last("FOR UPDATE");
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    public List<WmsInventory> listAvailableBatch(Long productId, Long warehouseId) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInventory::getProductId, productId)
                .eq(WmsInventory::getWarehouseId, warehouseId)
                .isNotNull(WmsInventory::getBatchNo)
                .gt(WmsInventory::getAvailableQuantity, BigDecimal.ZERO)
                // FEFO 先进先出：近效期批次优先出库（validityDate 早者在前，NULL 在 PG ASC 排序时排最后）
                .orderByAsc(WmsInventory::getValidityDate)
                .orderByAsc(WmsInventory::getBatchNo);
        return inventoryMapper.selectList(wrapper);
    }

    /**
     * 库存建行时从商品主数据补冗余快照字段（productCode/productName/spec/unit）。
     * 收货/上架/移库等经 increase 建行时，若无商品信息，库存列表商品字段会为空。
     */
    private void fillProductSnapshot(WmsInventory inv, Long productId) {
        if (productId == null) return;
        try {
            Product p = productService.getById(productId);
            if (p != null) {
                inv.setProductCode(p.getProductCode());
                inv.setProductName(p.getProductName());
                inv.setProductSpec(p.getSpec());
                inv.setProductUnit(p.getUnit());
            }
        } catch (Exception e) {
            log.warn("库存建行补商品快照失败: productId={}", productId, e);
        }
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
