package cn.aiedge.erp.sale.return.service.impl;

import cn.aiedge.erp.sale.return.entity.SaleReturn;
import cn.aiedge.erp.sale.return.entity.SaleReturnItem;
import cn.aiedge.erp.sale.return.enums.ReturnStatus;
import cn.aiedge.erp.sale.return.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.return.mapper.SaleReturnMapper;
import cn.aiedge.erp.sale.return.service.SaleReturnService;
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
public class SaleReturnServiceImpl extends ServiceImpl<SaleReturnMapper, SaleReturn> implements SaleReturnService {

    private final SaleReturnItemMapper returnItemMapper;

    @Override
    public SaleReturn getByReturnNo(String returnNo) {
        return lambdaQuery()
                .eq(SaleReturn::getReturnNo, returnNo)
                .eq(SaleReturn::getDeleted, 0)
                .one();
    }

    @Override
    public Page<SaleReturn> pageList(String keyword, Long customerId, Long orderId, Integer status, Integer returnType, int pageNum, int pageSize) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleReturn::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SaleReturn::getReturnNo, keyword)
                    .or().like(SaleReturn::getOrderNo, keyword)
                    .or().like(SaleReturn::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(SaleReturn::getCustomerId, customerId);
        }
        if (orderId != null) {
            wrapper.eq(SaleReturn::getOrderId, orderId);
        }
        if (status != null) {
            wrapper.eq(SaleReturn::getStatus, status);
        }
        if (returnType != null) {
            wrapper.eq(SaleReturn::getReturnType, returnType);
        }
        wrapper.orderByDesc(SaleReturn::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<SaleReturn> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<SaleReturn> listByOrderId(Long orderId) {
        return baseMapper.selectByOrderId(orderId);
    }

    @Override
    public String generateReturnNo() {
        String prefix = "SR";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(SaleReturn::getReturnNo, prefix + dateStr)
                .eq(SaleReturn::getDeleted, 0)
                .orderByDesc(SaleReturn::getReturnNo)
                .last("LIMIT 1");
        SaleReturn lastReturn = getOne(wrapper);
        int seq = 1;
        if (lastReturn != null) {
            String lastNo = lastReturn.getReturnNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn createReturn(SaleReturn returnOrder, List<SaleReturnItem> items) {
        returnOrder.setReturnNo(generateReturnNo());
        returnOrder.setStatus(ReturnStatus.DRAFT.getCode());
        returnOrder.setReturnDate(LocalDate.now());
        returnOrder.setTotalQuantity(BigDecimal.ZERO);
        returnOrder.setTotalAmount(BigDecimal.ZERO);
        returnOrder.setRefundAmount(BigDecimal.ZERO);
        save(returnOrder);
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                SaleReturnItem item = items.get(i);
                item.setReturnId(returnOrder.getId());
                item.setLineNo(i + 1);
                item.setTenantId(returnOrder.getTenantId());
                item.setAcceptedQuantity(BigDecimal.ZERO);
                item.setRejectedQuantity(BigDecimal.ZERO);
                item.setRefundAmount(BigDecimal.ZERO);
                calculateItemAmounts(item);
                returnItemMapper.insert(item);
            }
        }
        calculateTotals(returnOrder.getId());
        return getById(returnOrder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn createFromOrder(Long orderId) {
        SaleReturn returnOrder = new SaleReturn();
        returnOrder.setOrderId(orderId);
        returnOrder.setReturnDate(LocalDate.now());
        returnOrder.setReturnType(1);
        returnOrder.setWarehouseId(1L);
        return createReturn(returnOrder, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn updateReturn(Long returnId, SaleReturn returnOrder, List<SaleReturnItem> items) {
        SaleReturn existing = getById(returnId);
        if (existing == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (existing.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以修改");
        }
        returnOrder.setId(returnId);
        updateById(returnOrder);
        if (items != null) {
            List<SaleReturnItem> existingItems = getItems(returnId);
            for (SaleReturnItem oldItem : existingItems) {
                returnItemMapper.deleteById(oldItem.getId());
            }
            for (int i = 0; i < items.size(); i++) {
                SaleReturnItem item = items.get(i);
                item.setReturnId(returnId);
                item.setLineNo(i + 1);
                item.setTenantId(existing.getTenantId());
                item.setAcceptedQuantity(BigDecimal.ZERO);
                item.setRejectedQuantity(BigDecimal.ZERO);
                item.setRefundAmount(BigDecimal.ZERO);
                calculateItemAmounts(item);
                returnItemMapper.insert(item);
            }
        }
        calculateTotals(returnId);
        return getById(returnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn submitForApproval(Long returnId) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以提交审批");
        }
        returnOrder.setStatus(ReturnStatus.PENDING_APPROVAL.getCode());
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn approve(Long returnId, Long approverId, String note) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的退货单可以审批");
        }
        returnOrder.setStatus(ReturnStatus.APPROVED.getCode());
        returnOrder.setApprovedBy(approverId);
        returnOrder.setApprovedTime(LocalDateTime.now());
        returnOrder.setApprovedNote(note);
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn reject(Long returnId, Long rejecterId, String reason) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.PENDING_APPROVAL.getCode()) {
            throw new RuntimeException("只有待审批状态的退货单可以拒绝");
        }
        returnOrder.setStatus(ReturnStatus.REJECTED.getCode());
        returnOrder.setRemark(reason);
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn receive(Long returnId, Long receiverId) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.APPROVED.getCode()) {
            throw new RuntimeException("只有已审批状态的退货单可以收货");
        }
        returnOrder.setStatus(ReturnStatus.RECEIVED.getCode());
        returnOrder.setReceivedBy(receiverId);
        returnOrder.setReceivedTime(LocalDateTime.now());
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem receiveItem(Long itemId, BigDecimal acceptedQuantity, BigDecimal rejectedQuantity, String qualityNote) {
        SaleReturnItem item = returnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }
        SaleReturn returnOrder = getById(item.getReturnId());
        if (returnOrder.getStatus() != ReturnStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的退货单可以处理明细");
        }
        item.setAcceptedQuantity(acceptedQuantity);
        item.setRejectedQuantity(rejectedQuantity);
        item.setQualityNote(qualityNote);
        item.setQualityStatus(acceptedQuantity.compareTo(BigDecimal.ZERO) > 0 ? "合格" : "不合格");
        BigDecimal refundQty = acceptedQuantity;
        item.setRefundAmount(refundQty.multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        returnItemMapper.updateById(item);
        calculateTotals(item.getReturnId());
        return returnItemMapper.selectById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn confirmWarehouse(Long returnId, Long confirmerId) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.RECEIVED.getCode()) {
            throw new RuntimeException("只有已收货状态的退货单可以确认入库");
        }
        returnOrder.setStatus(ReturnStatus.WAREHOUSE_CONFIRMED.getCode());
        returnOrder.setWarehouseConfirmedBy(confirmerId);
        returnOrder.setWarehouseConfirmedTime(LocalDateTime.now());
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn processRefund(Long returnId, String refundMethod, String refundAccount, String refundNote) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.WAREHOUSE_CONFIRMED.getCode()) {
            throw new RuntimeException("只有已入库状态的退货单可以处理退款");
        }
        returnOrder.setStatus(ReturnStatus.PENDING_REFUND.getCode());
        returnOrder.setRefundMethod(refundMethod);
        returnOrder.setRefundAccount(refundAccount);
        returnOrder.setRefundNote(refundNote);
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn completeRefund(Long returnId) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.PENDING_REFUND.getCode()) {
            throw new RuntimeException("只有待退款状态的退货单可以完成退款");
        }
        returnOrder.setStatus(ReturnStatus.REFUNDED.getCode());
        returnOrder.setRefundProcessedBy(returnOrder.getCreateBy());
        returnOrder.setRefundProcessedTime(LocalDateTime.now());
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn complete(Long returnId) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.REFUNDED.getCode()) {
            throw new RuntimeException("只有已退款状态的退货单可以完成");
        }
        returnOrder.setStatus(ReturnStatus.COMPLETED.getCode());
        returnOrder.setCompletedBy(returnOrder.getCreateBy());
        returnOrder.setCompletedTime(LocalDateTime.now());
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturn cancel(Long returnId, String reason) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() == ReturnStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的退货单不能取消");
        }
        returnOrder.setStatus(ReturnStatus.CANCELLED.getCode());
        returnOrder.setRemark(reason);
        updateById(returnOrder);
        return returnOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotals(Long returnId) {
        BigDecimal totalQuantity = returnItemMapper.sumReturnQuantityByReturnId(returnId);
        BigDecimal totalAmount = returnItemMapper.sumReturnAmountByReturnId(returnId);
        BigDecimal refundAmount = returnItemMapper.sumRefundAmountByReturnId(returnId);
        SaleReturn returnOrder = getById(returnId);
        returnOrder.setTotalQuantity(totalQuantity != null ? totalQuantity : BigDecimal.ZERO);
        returnOrder.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        returnOrder.setRefundAmount(refundAmount != null ? refundAmount : BigDecimal.ZERO);
        if (returnOrder.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = returnOrder.getRefundAmount().divide(returnOrder.getTotalAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            returnOrder.setRefundRate(rate);
        }
        updateById(returnOrder);
    }

    @Override
    public List<SaleReturnItem> getItems(Long returnId) {
        return returnItemMapper.selectByReturnId(returnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem addItem(Long returnId, SaleReturnItem item) {
        SaleReturn returnOrder = getById(returnId);
        if (returnOrder == null) {
            throw new RuntimeException("退货单不存在");
        }
        if (returnOrder.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以添加明细");
        }
        List<SaleReturnItem> existingItems = getItems(returnId);
        item.setReturnId(returnId);
        item.setLineNo(existingItems.size() + 1);
        item.setTenantId(returnOrder.getTenantId());
        item.setAcceptedQuantity(BigDecimal.ZERO);
        item.setRejectedQuantity(BigDecimal.ZERO);
        item.setRefundAmount(BigDecimal.ZERO);
        calculateItemAmounts(item);
        returnItemMapper.insert(item);
        calculateTotals(returnId);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaleReturnItem updateItem(Long itemId, SaleReturnItem item) {
        SaleReturnItem existing = returnItemMapper.selectById(itemId);
        if (existing == null) {
            throw new RuntimeException("退货明细不存在");
        }
        SaleReturn returnOrder = getById(existing.getReturnId());
        if (returnOrder.getStatus() != ReturnStatus.DRAFT.getCode()) {
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
        SaleReturnItem item = returnItemMapper.selectById(itemId);
        if (item == null) {
            throw new RuntimeException("退货明细不存在");
        }
        SaleReturn returnOrder = getById(item.getReturnId());
        if (returnOrder.getStatus() != ReturnStatus.DRAFT.getCode()) {
            throw new RuntimeException("只有草稿状态的退货单可以删除明细");
        }
        returnItemMapper.deleteById(itemId);
        calculateTotals(item.getReturnId());
    }

    @Override
    public BigDecimal calculateRefundAmount(Long returnId) {
        return returnItemMapper.sumRefundAmountByReturnId(returnId);
    }

    private void calculateItemAmounts(SaleReturnItem item) {
        BigDecimal quantity = item.getReturnQuantity() != null ? item.getReturnQuantity() : BigDecimal.ZERO;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal returnAmount = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        item.setReturnAmount(returnAmount);
        item.setOriginalAmount(item.getOrderQuantity() != null ? item.getOrderQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
    }
}