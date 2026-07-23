package cn.aiedge.erp.purchase.inbound.service.impl;

import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import cn.aiedge.erp.purchase.inbound.enums.InboundStatus;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundItemMapper;
import cn.aiedge.erp.purchase.inbound.mapper.PurchaseInboundMapper;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
import cn.aiedge.erp.purchase.service.integration.PurchaseAccountingService;
import cn.aiedge.erp.stock.service.StockService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseInboundServiceImpl extends ServiceImpl<PurchaseInboundMapper, PurchaseInbound> implements PurchaseInboundService {

    private final PurchaseInboundItemMapper inboundItemMapper;
    private final StockService stockService;
    private final PurchaseAccountingService purchaseAccountingService;

    @Override
    public PurchaseInbound getByInboundNo(String inboundNo) {
        return lambdaQuery()
                .eq(PurchaseInbound::getInboundNo, inboundNo)
                .eq(PurchaseInbound::getDeleted, 0)
                .one();
    }

    @Override
    public Page<PurchaseInbound> pageList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseInbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseInbound::getInboundNo, keyword)
                    .or().like(PurchaseInbound::getOrderNo, keyword)
                    .or().like(PurchaseInbound::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseInbound::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseInbound::getOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseInbound::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseInbound::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseInbound::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<PurchaseInbound> exportList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status) {
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseInbound::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseInbound::getInboundNo, keyword)
                    .or().like(PurchaseInbound::getOrderNo, keyword)
                    .or().like(PurchaseInbound::getSupplierName, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseInbound::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseInbound::getOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseInbound::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseInbound::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseInbound::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<PurchaseInbound> listBySupplierId(Long supplierId) {
        return baseMapper.selectBySupplierId(supplierId);
    }

    @Override
    public List<PurchaseInbound> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateInboundNo() {
        String prefix = "PI";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseInbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseInbound::getInboundNo, prefix + dateStr)
                .eq(PurchaseInbound::getDeleted, 0)
                .orderByDesc(PurchaseInbound::getInboundNo)
                .last("LIMIT 1");
        PurchaseInbound lastInbound = getOne(wrapper);
        int seq = 1;
        if (lastInbound != null) {
            String lastNo = lastInbound.getInboundNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound createInbound(PurchaseInbound inbound, List<PurchaseInboundItem> items) {
        inbound.setInboundNo(generateInboundNo());
        inbound.setStatus(InboundStatus.DRAFT.getCode());
        inbound.setInboundDate(LocalDate.now());
        inbound.setInboundType(1);
        inbound.setTotalQuantity(BigDecimal.ZERO);
        inbound.setTotalAmount(BigDecimal.ZERO);
        inbound.setTaxAmount(BigDecimal.ZERO);
        inbound.setTotalAmountWithTax(BigDecimal.ZERO);
        save(inbound);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                PurchaseInboundItem item = items.get(i);
                item.setInboundId(inbound.getId());
                item.setLineNo(i + 1);
                item.setTenantId(inbound.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setInboundQuantity(BigDecimal.ZERO);
                calculateItemAmounts(item);
                inboundItemMapper.insert(item);
            }
        }
        calculateTotals(inbound.getId());
        return getById(inbound.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound createFromOrder(Long orderId) {
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderId(orderId);
        inbound.setInboundDate(LocalDate.now());
        inbound.setInboundType(1);
        inbound.setWarehouseId(1L);
        return createInbound(inbound, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound updateInbound(Long inboundId, PurchaseInbound inbound, List<PurchaseInboundItem> items) {
        PurchaseInbound existing = getById(inboundId);
        if (existing == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (existing.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以修改");
        }
        inbound.setId(inboundId);
        updateById(inbound);
        if (items != null) {
            List<PurchaseInboundItem> existingItems = getItems(inboundId);
            for (PurchaseInboundItem oldItem : existingItems) {
                inboundItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                PurchaseInboundItem item = items.get(i);
                item.setInboundId(inboundId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setPendingQuantity(item.getOrderQuantity());
                item.setInboundQuantity(BigDecimal.ZERO);
                calculateItemAmounts(item);
                inboundItemMapper.insert(item);
            }
        }
        calculateTotals(inboundId);
        return getById(inboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound submitForApproval(Long inboundId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以提交审批");
        }
        inbound.setStatus(InboundStatus.PENDING_APPROVAL.getCode());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound approve(Long inboundId, Long approverId, String note) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的入库单可以审批");
        }
        inbound.setStatus(InboundStatus.APPROVED.getCode());
        inbound.setApprovedBy(approverId);
        inbound.setApprovedTime(LocalDateTime.now());
        inbound.setApprovedNote(note);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound reject(Long inboundId, String reason) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的入库单可以拒绝");
        }
        inbound.setStatus(InboundStatus.DRAFT.getCode());
        inbound.setRemark(reason);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound receive(Long inboundId, Long receiverId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的入库单可以收货");
        }
        inbound.setStatus(InboundStatus.RECEIVED.getCode());
        inbound.setReceivedBy(receiverId);
        inbound.setReceivedTime(LocalDateTime.now());
        inbound.setActualArrivalTime(LocalDateTime.now());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem receiveItem(Long itemId, BigDecimal inboundQuantity, String qualityNote) {
        PurchaseInboundItem item = inboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(item.getInboundId());
        if (inbound.getStatus() != InboundStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的入库单可以处理明细");
        }
        item.setInboundQuantity(inboundQuantity);
        item.setPendingQuantity(item.getOrderQuantity().subtract(inboundQuantity));
        item.setQualityNote(qualityNote);
        calculateItemAmounts(item);
        inboundItemMapper.updateById(item);
        calculateTotals(item.getInboundId());
        return inboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound qualityCheck(Long inboundId, Long checkerId, String result) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的入库单可以质检");
        }
        inbound.setStatus(InboundStatus.QUALITY_CHECKED.getCode());
        inbound.setQualityCheckedBy(checkerId);
        inbound.setQualityCheckedTime(LocalDateTime.now());
        inbound.setQualityCheckResult(result);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound confirmWarehouse(Long inboundId, Long confirmerId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.QUALITY_CHECKED.getCode()) {
            throw new RuntimeException("只有已质检状态的入库单可以确认入库");
        }
        inbound.setStatus(InboundStatus.WAREHOUSE_CONFIRMED.getCode());
        inbound.setWarehouseConfirmedBy(confirmerId);
        inbound.setWarehouseConfirmedTime(LocalDateTime.now());
        updateById(inbound);
        updateStock(inboundId);

        // 业财直调：收货入库完成产生应付及库存凭证（对标Odoo bill on receipt）
        BigDecimal payableAmount = inbound.getTotalAmountWithTax() != null
                ? inbound.getTotalAmountWithTax()
                : (inbound.getTotalAmount() != null ? inbound.getTotalAmount() : BigDecimal.ZERO);
        if (inbound.getSupplierId() != null && payableAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                purchaseAccountingService.createPayableOnReceipt(
                        inbound.getId(), inbound.getInboundNo(),
                        String.valueOf(inbound.getSupplierId()), inbound.getSupplierName(),
                        payableAmount);
            } catch (Exception e) {
                log.error("采购收货自动记账失败，入库单ID={}, 原因={}", inboundId, e.getMessage(), e);
                // 记账失败不影响入库确认
            }
        }
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound complete(Long inboundId) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.WAREHOUSE_CONFIRMED.getCode()) {
            throw new RuntimeException("只有已入库状态的入库单可以完成");
        }
        inbound.setStatus(InboundStatus.COMPLETED.getCode());
        inbound.setCompletedBy(inbound.getCreateBy());
        inbound.setCompletedTime(LocalDateTime.now());
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInbound cancel(Long inboundId, String reason) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() == InboundStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的入库单不能取消");
        }
        // 已确认入库的单据库存已回写，取消前必须先回冲，避免库存虚增
        if (inbound.getStatus() >= InboundStatus.WAREHOUSE_CONFIRMED.getCode()) {
            reverseStock(inboundId);
        }
        inbound.setStatus(InboundStatus.CANCELLED.getCode());
        inbound.setRemark(reason);
        updateById(inbound);
        return inbound;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long inboundId) {
        BigDecimal totalQuantity = inboundItemMapper.sumInboundQuantityByInboundId(inboundId);
        BigDecimal totalAmount = inboundItemMapper.sumLineAmountByInboundId(inboundId);
        BigDecimal taxAmount = inboundItemMapper.sumTaxAmountByInboundId(inboundId);
        BigDecimal totalAmountWithTax = inboundItemMapper.sumLineTotalByInboundId(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        inbound.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        inbound.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        inbound.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        inbound.setTotalAmountWithTax(totalAmountWithTax != null ? totalAmountWithTax : BigDecimal.ZERO);
        updateById(inbound);
    }

    @Override
    public List<PurchaseInboundItem> getItems(Long inboundId) {
        return inboundItemMapper.selectByInboundId(inboundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem addItem(Long inboundId, PurchaseInboundItem item) {
        PurchaseInbound inbound = getById(inboundId);
        if (inbound == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以添加明细");
        }
        List<PurchaseInboundItem> existingItems = getItems(inboundId);
        item.setInboundId(inboundId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(inbound.getTenantId());
        item.setPendingQuantity(item.getOrderQuantity());
        item.setInboundQuantity(BigDecimal.ZERO);
        calculateItemAmounts(item);
        inboundItemMapper.insert(item);
        calculateTotals(inboundId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseInboundItem updateItem(Long itemId, PurchaseInboundItem item) {
        PurchaseInboundItem existing = inboundItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(existing.getInboundId());
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以修改明细");
        }
        item.setId(itemId);
        item.setPendingQuantity(item.getOrderQuantity());
        calculateItemAmounts(item);
        inboundItemMapper.updateById(item);
        calculateTotals(existing.getInboundId());
        return inboundItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        PurchaseInboundItem item = inboundItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("入库明细不存在");
        }
        PurchaseInbound inbound = getById(item.getInboundId());
        if (inbound.getStatus() != InboundStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的入库单可以删除明细");
        }
        inboundItemMapper.deleteById(itemId);
        calculateTotals(item.getInboundId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long inboundId) {
        List<PurchaseInboundItem> items = getItems(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        Long warehouseId = inbound.getWarehouseId();
        if (warehouseId == null) {
            throw new RuntimeException("入库单未指定仓库，无法回写库存");
        }
        for (PurchaseInboundItem item : items) {
            BigDecimal qty = item.getInboundQuantity();
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0 && item.getProductId() != null) {
                boolean success = stockService.increaseStock(item.getProductId(), warehouseId, qty);
                if (!success) {
                    throw new RuntimeException("库存回写失败: 产品ID=" + item.getProductId()
                            + ", 仓库ID=" + warehouseId + ", 数量=" + qty);
                }
                log.info("采购入库回写库存成功: 入库单ID={}, 产品ID={}, 仓库ID={}, 数量={}",
                        inboundId, item.getProductId(), warehouseId, qty);
            }
        }
    }

    /**
     * 取消已入库单据时的库存回冲（按已入库数量扣减）
     */
    private void reverseStock(Long inboundId) {
        List<PurchaseInboundItem> items = getItems(inboundId);
        PurchaseInbound inbound = getById(inboundId);
        Long warehouseId = inbound.getWarehouseId();
        for (PurchaseInboundItem item : items) {
            BigDecimal qty = item.getInboundQuantity();
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0 && item.getProductId() != null && warehouseId != null) {
                boolean success = stockService.decreaseStock(item.getProductId(), warehouseId, qty);
                if (!success) {
                    throw new RuntimeException("库存回冲失败（可能被后续出库占用，请先核查库存）: 产品ID="
                            + item.getProductId() + ", 仓库ID=" + warehouseId + ", 数量=" + qty);
                }
                log.info("采购入库取消回冲库存: 入库单ID={}, 产品ID={}, 仓库ID={}, 数量={}",
                        inboundId, item.getProductId(), warehouseId, qty);
            }
        }
    }

    private void calculateItemAmounts(PurchaseInboundItem item) {
        BigDecimal quantity = item.getInboundQuantity() != null ? item.getInboundQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setLineAmount(lineAmount);
        BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;
        BigDecimal taxAmount = lineAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        item.setTaxAmount(taxAmount);
        BigDecimal lineTotal = lineAmount.add(taxAmount);
        item.setLineTotal(lineTotal);
    }
}