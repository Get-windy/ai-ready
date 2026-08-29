package cn.aiedge.erp.purchase.purchasereturn.service.impl;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.enums.ReturnStatus;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnItemMapper;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnMapper;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
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
public class PurchaseReturnServiceImpl extends ServiceImpl<PurchaseReturnMapper, PurchaseReturn> implements PurchaseReturnService {

    private final PurchaseReturnItemMapper returnItemMapper;
    private final StockService stockService;

    @Override
    public PurchaseReturn getByReturnNo(String returnNo) {
        return lambdaQuery()
                .eq(PurchaseReturn::getReturnNo, returnNo)
                .eq(PurchaseReturn::getDeleted, 0)
                .one();
    }

    @Override
    public Page<PurchaseReturn> pageList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status, Integer settleStatus, int pageNum, int pageSize) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseReturn::getReturnNo, keyword)
                    .or().like(PurchaseReturn::getPurchaseOrderNo, keyword)
                    .or().like(PurchaseReturn::getSupplierName, keyword)
                    .or().like(PurchaseReturn::getSupplierNo, keyword)
                    .or().like(PurchaseReturn::getContactName, keyword)
                    .or().like(PurchaseReturn::getContactPhone, keyword)
                    .or().like(PurchaseReturn::getPurchaserName, keyword)
                    .or().like(PurchaseReturn::getDepartmentName, keyword)
                    .or().like(PurchaseReturn::getCreateByName, keyword)
                    .or().like(PurchaseReturn::getPosterName, keyword)
                    .or().like(PurchaseReturn::getWarehouseName, keyword)
                    .or().like(PurchaseReturn::getRemark, keyword)
                    .or().like(PurchaseReturn::getExtText1, keyword)
                    .or().like(PurchaseReturn::getExtText2, keyword)
                    .or().like(PurchaseReturn::getExtText3, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseReturn::getPurchaseOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseReturn::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseReturn::getStatus, status);
        }
        if (settleStatus != null) {
            wrapper.eq(PurchaseReturn::getSettleStatus, settleStatus);
        }
        wrapper.orderByDesc(PurchaseReturn::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<PurchaseReturn> exportList(String keyword, Long supplierId, Long orderId, Long warehouseId, Integer status) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(PurchaseReturn::getReturnNo, keyword)
                    .or().like(PurchaseReturn::getPurchaseOrderNo, keyword)
                    .or().like(PurchaseReturn::getSupplierName, keyword)
                    .or().like(PurchaseReturn::getSupplierNo, keyword)
                    .or().like(PurchaseReturn::getContactName, keyword)
                    .or().like(PurchaseReturn::getContactPhone, keyword)
                    .or().like(PurchaseReturn::getPurchaserName, keyword)
                    .or().like(PurchaseReturn::getDepartmentName, keyword)
                    .or().like(PurchaseReturn::getCreateByName, keyword)
                    .or().like(PurchaseReturn::getPosterName, keyword)
                    .or().like(PurchaseReturn::getWarehouseName, keyword)
                    .or().like(PurchaseReturn::getRemark, keyword)
                    .or().like(PurchaseReturn::getExtText1, keyword)
                    .or().like(PurchaseReturn::getExtText2, keyword)
                    .or().like(PurchaseReturn::getExtText3, keyword));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        }
        if (orderId != null) {
            wrapper.eq(PurchaseReturn::getPurchaseOrderId, orderId);
        }
        if (warehouseId != null) {
            wrapper.eq(PurchaseReturn::getWarehouseId, warehouseId);
        }
        if (status != null) {
            wrapper.eq(PurchaseReturn::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseReturn::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<PurchaseReturn> listBySupplierId(Long supplierId) {
        return baseMapper.selectBySupplierId(supplierId);
    }

    @Override
    public List<PurchaseReturn> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateReturnNo() {
        String prefix = "PR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PurchaseReturn::getReturnNo, prefix + dateStr)
                .eq(PurchaseReturn::getDeleted, 0)
                .orderByDesc(PurchaseReturn::getReturnNo)
                .last("LIMIT 1");
        PurchaseReturn last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getReturnNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    public void batchPrint(List<Long> ids, String template) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择要打印的退货单");
        }
        for (Long id : ids) {
            PurchaseReturn ret = baseMapper.selectById(id);
            if (ret == null) continue;
            int pc = (ret.getPrintCount() != null ? ret.getPrintCount() : 0) + 1;
            PurchaseReturn upd = new PurchaseReturn();
            upd.setId(id);
            upd.setPrintCount(pc);
            baseMapper.updateById(upd);
        }
        log.info("批量打印 {} 条退货单，模板: {}", ids.size(), template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn createReturn(PurchaseReturn returnOrder, List<PurchaseReturnItem> items) {
        returnOrder.setReturnNo(generateReturnNo());
        returnOrder.setStatus(ReturnStatus.DRAFT.getCode());
        if (returnOrder.getReturnDate() == null) {
            returnOrder.setReturnDate(LocalDate.now());
        }
        if (returnOrder.getReturnType() == null) {
            returnOrder.setReturnType(1);
        }
        returnOrder.setTotalQuantity(BigDecimal.ZERO);
        returnOrder.setTotalAmount(BigDecimal.ZERO);
        returnOrder.setTaxAmount(BigDecimal.ZERO);
        returnOrder.setTotalAmountWithTax(BigDecimal.ZERO);
        returnOrder.setSettleStatus(0);
        save(returnOrder);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                PurchaseReturnItem item = items.get(i);
                item.setReturnId(returnOrder.getId());
                item.setLineNo(i + 1);
                item.setTenantId(returnOrder.getTenantId());
                calculateItemAmounts(item);
                returnItemMapper.insert(item);
            }
        }
        calculateTotals(returnOrder.getId());
        return getById(returnOrder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn createFromOrder(Long orderId) {
        PurchaseReturn ret = new PurchaseReturn();
        ret.setPurchaseOrderId(orderId);
        ret.setReturnDate(LocalDate.now());
        ret.setReturnType(1);
        return createReturn(ret, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn updateReturn(Long returnId, PurchaseReturn returnOrder, List<PurchaseReturnItem> items) {
        PurchaseReturn existing = getById(returnId);
        if (existing == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (existing.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以修改");
        }
        returnOrder.setId(returnId);
        returnOrder.setReturnNo(existing.getReturnNo());
        returnOrder.setStatus(ReturnStatus.DRAFT.getCode());
        returnOrder.setCreateTime(existing.getCreateTime());
        returnOrder.setCreateBy(existing.getCreateBy());
        updateById(returnOrder);
        if (items != null) {
            List<PurchaseReturnItem> existingItems = getItems(returnId);
            for (PurchaseReturnItem oldItem : existingItems) {
                returnItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                PurchaseReturnItem item = items.get(i);
                item.setId(null);
                item.setReturnId(returnId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                calculateItemAmounts(item);
                returnItemMapper.insert(item);
            }
        }
        calculateTotals(returnId);
        return getById(returnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn submitForApproval(Long returnId) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以提交审批");
        }
        ret.setStatus(ReturnStatus.PENDING_APPROVAL.getCode());
        ret.setApplyTime(LocalDateTime.now());
        updateById(ret);
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn approve(Long returnId, Long approverId, String note) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() != ReturnStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的退货单可以审批");
        }
        ret.setStatus(ReturnStatus.APPROVED.getCode());
        ret.setApprovedBy(approverId);
        ret.setApprovedTime(LocalDateTime.now());
        ret.setApprovedNote(note);
        updateById(ret);
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn reject(Long returnId, String reason) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() != ReturnStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的退货单可以拒绝");
        }
        ret.setStatus(ReturnStatus.REJECTED.getCode());
        ret.setApprovedNote(reason);
        ret.setRemark(reason);
        updateById(ret);
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn complete(Long returnId) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() != ReturnStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的退货单可以完成出库");
        }
        ret.setStatus(ReturnStatus.COMPLETED.getCode());
        updateById(ret);
        updateStock(returnId);
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturn cancel(Long returnId, String reason) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() == ReturnStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的退货单不能取消");
        }
        ret.setStatus(ReturnStatus.CANCELLED.getCode());
        ret.setRemark(reason);
        updateById(ret);
        return ret;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnId) {
        BigDecimal totalQuantity = returnItemMapper.sumReturnQuantityByReturnId(returnId);
        BigDecimal totalAmount = returnItemMapper.sumLineAmountByReturnId(returnId);
        BigDecimal taxAmount = returnItemMapper.sumTaxAmountByReturnId(returnId);
        BigDecimal totalWithTax = returnItemMapper.sumLineTotalByReturnId(returnId);
        PurchaseReturn ret = getById(returnId);
        ret.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        ret.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        ret.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        ret.setTotalAmountWithTax(totalWithTax != null ? totalWithTax : BigDecimal.ZERO);
        // 折后金额默认等于金额，本单金额等于价税合计
        if (ret.getDiscountAmount() == null) {
            ret.setDiscountAmount(ret.getTotalAmount());
        }
        updateById(ret);
    }

    @Override
    public List<PurchaseReturnItem> getItems(Long returnId) {
        return returnItemMapper.selectByReturnId(returnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnItem addItem(Long returnId, PurchaseReturnItem item) {
        PurchaseReturn ret = getById(returnId);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (ret.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以添加明细");
        }
        List<PurchaseReturnItem> existingItems = getItems(returnId);
        item.setReturnId(returnId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(ret.getTenantId());
        calculateItemAmounts(item);
        returnItemMapper.insert(item);
        calculateTotals(returnId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseReturnItem updateItem(Long itemId, PurchaseReturnItem item) {
        PurchaseReturnItem existing = returnItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }
        PurchaseReturn ret = getById(existing.getReturnId());
        if (ret.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以修改明细");
        }
        item.setId(itemId);
        calculateItemAmounts(item);
        returnItemMapper.updateById(item);
        calculateTotals(existing.getReturnId());
        return returnItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId) {
        PurchaseReturnItem item = returnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }
        PurchaseReturn ret = getById(item.getReturnId());
        if (ret.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以删除明细");
        }
        returnItemMapper.deleteById(itemId);
        calculateTotals(item.getReturnId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long returnId) {
        PurchaseReturn ret = getById(returnId);
        Long warehouseId = ret.getWarehouseId();
        if (warehouseId == null) {
            throw new RuntimeException("退货单未指定出库仓库，无法回写库存");
        }
        List<PurchaseReturnItem> items = getItems(returnId);
        for (PurchaseReturnItem item : items) {
            BigDecimal qty = item.getReturnQuantity();
            if (qty != null && qty.compareTo(BigDecimal.ZERO) > 0 && item.getProductId() != null) {
                boolean success = stockService.decreaseStock(item.getProductId(), warehouseId, qty);
                if (!success) {
                    throw new RuntimeException("退货出库回写库存失败（可能库存不足）: 产品ID=" + item.getProductId()
                            + ", 仓库ID=" + warehouseId + ", 数量=" + qty);
                }
                log.info("采购退货回写库存成功: 退货单ID={}, 产品ID={}, 仓库ID={}, 数量={}",
                        returnId, item.getProductId(), warehouseId, qty);
            }
        }
    }

    private void calculateItemAmounts(PurchaseReturnItem item) {
        BigDecimal quantity = item.getReturnQuantity() != null ? item.getReturnQuantity() : BigDecimal.ZERO;
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
