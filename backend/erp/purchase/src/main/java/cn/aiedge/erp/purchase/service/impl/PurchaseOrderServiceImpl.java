package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);
    private final PurchaseOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrder order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setFulfillmentPercent(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy(StpUtil.getLoginIdAsLong());
        
        calculateOrderAmount(order);
        
        save(order);
        logger.info("创建采购订单成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PurchaseOrder order) {
        PurchaseOrder existing = getById(order.getId());
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法修改");
        }
        
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(StpUtil.getLoginIdAsLong());
        updateById(order);
        logger.info("更新采购订单成功: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        PurchaseOrder existing = getById(orderId);
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus() > 1) {
            throw BusinessException.badRequest("订单已审批，无法删除");
        }
        
        removeById(orderId);
        logger.info("删除采购订单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交审批");
        }
        
        order.setStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("提交采购订单审批: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw BusinessException.badRequest("订单不在待审批状态");
        }
        
        order.setStatus(2);
        order.setApprovalUserId(StpUtil.getLoginIdAsLong());
        order.setApprovalTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("审批通过采购订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        order.setStatus(0);
        order.setRemark(order.getRemark() + " [审批拒绝: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("审批拒绝采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() >= 3) {
            throw BusinessException.badRequest("订单已开始入库，无法取消");
        }
        
        order.setStatus(4);
        order.setCancellationFlag(1);
        order.setRemark(order.getRemark() + " [取消原因: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("取消采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    public Page<PurchaseOrder> pageOrders(Page<PurchaseOrder> page, Long tenantId,
                                          String orderNo, Long supplierId, Integer status) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .like(orderNo != null, PurchaseOrder::getOrderNo, orderNo)
                .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(status != null, PurchaseOrder::getStatus, status)
                .orderByDesc(PurchaseOrder::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public PurchaseOrder getOrderDetail(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    @Override
    public List<Object> getOrderItems(Long orderId) {
        return Collections.singletonList(orderItemMapper.findByOrderId(orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder submitOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交");
        }
        
        order.setStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("提交采购订单: orderId={}", orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder approveOrder(Long orderId, Long approverId, String comment) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw BusinessException.badRequest("订单不在待审批状态");
        }
        
        order.setStatus(2);
        order.setApprovalUserId(approverId);
        order.setApprovalTime(LocalDateTime.now());
        order.setRemark(order.getRemark() + " [审批意见: " + comment + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("审批采购订单: orderId={}, approverId={}, comment={}", orderId, approverId, comment);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder issueOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 2) {
            throw BusinessException.badRequest("订单未审批，无法下达");
        }
        
        order.setStatus(3);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("下达采购订单: orderId={}", orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder generateOrderFromContract(PurchaseContract contract, List<PurchaseOrderItem> items) {
        PurchaseOrder order = new PurchaseOrder();
        order.setContractId(contract.getId());
        order.setSupplierId(contract.getSupplierId());
        order.setSupplierName(contract.getSupplierName());
        order.setTotalAmount(contract.getTotalAmount());
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        save(order);

        if (items != null && !items.isEmpty()) {
            items.forEach(item -> item.setOrderId(order.getId()));
            orderItemMapper.batchInsert(items);
        }

        logger.info("从合同生成采购订单: orderId={}, contractId={}", order.getId(), contract.getId());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder startFulfillment(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw BusinessException.badRequest("订单未下达，无法开始履行");
        }
        
        order.setStatus(5);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("开始履行采购订单: orderId={}", orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder updateFulfillmentProgress(Long orderId, BigDecimal receivedAmount, BigDecimal fulfillmentPercent) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        order.setReceivedAmount(receivedAmount);
        order.setFulfillmentPercent(fulfillmentPercent);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("更新履行进度: orderId={}, receivedAmount={}, fulfillmentPercent={}", 
                orderId, receivedAmount, fulfillmentPercent);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder completeOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        order.setStatus(6);
        order.setClosedFlag(1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("完成采购订单: orderId={}", orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBySupplier(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw BusinessException.badRequest("只有已审批或已下达的订单才能进行供应商确认");
        }
        
        order.setSupplierConfirmed(true);
        order.setSupplierConfirmTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("供应商确认订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String trackingNumber, LocalDateTime estimatedArrivalTime) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!Boolean.TRUE.equals(order.getSupplierConfirmed())) {
            throw BusinessException.badRequest("订单未经过供应商确认，无法发货");
        }
        
        order.setShipped(true);
        order.setTrackingNumber(trackingNumber);
        order.setEstimatedArrivalTime(estimatedArrivalTime);
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("发货通知: orderId={}, trackingNumber={}", orderId, trackingNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId, BigDecimal receivedQuantity, String qualityCheckResult) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!Boolean.TRUE.equals(order.getShipped())) {
            throw BusinessException.badRequest("订单未发货，无法收货");
        }
        
        order.setReceived(true);
        order.setReceivedQuantity(receivedQuantity);
        order.setQualityCheckResult(qualityCheckResult);
        order.setReceiveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        BigDecimal totalQuantity = order.getTotalQuantity();
        if (totalQuantity != null && receivedQuantity.compareTo(totalQuantity) < 0) {
            order.setStatus(5);
        } else {
            order.setStatus(6);
        }
        
        updateById(order);
        logger.info("收货确认: orderId={}, receivedQuantity={}, qualityCheckResult={}", 
                orderId, receivedQuantity, qualityCheckResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long orderId, String invoiceNumber, BigDecimal invoiceAmount, LocalDate invoiceDate) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!Boolean.TRUE.equals(order.getReceived())) {
            throw BusinessException.badRequest("订单未收货，无法提交发票");
        }
        
        order.setInvoiceStatus(1);
        order.setInvoiceNumber(invoiceNumber);
        order.setInvoiceAmount(invoiceAmount);
        order.setInvoiceDate(invoiceDate.atStartOfDay());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        logger.info("发票提交: orderId={}, invoiceNumber={}, invoiceAmount={}", 
                orderId, invoiceNumber, invoiceAmount);
    }

    @Override
    public List<PurchaseOrder> getPendingApprovalOrders() {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getStatus, 1)
                .orderByDesc(PurchaseOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getPurchaseStatistics(Long tenantId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59));
        
        long totalOrders = count(wrapper);
        statistics.put("totalOrders", totalOrders);
        
        wrapper.clear();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59));
        List<PurchaseOrder> orders = list(wrapper);
        BigDecimal totalAmount = orders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        statistics.put("totalAmount", totalAmount);
        
        for (int i = 0; i <= 6; i++) {
            wrapper.clear();
            wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                    .eq(PurchaseOrder::getStatus, i)
                    .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                    .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59));
            long count = count(wrapper);
            statistics.put("status_" + i, count);
        }
        
        return statistics;
    }

    @Override
    public List<Map<String, Object>> generatePurchaseReport(Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> report = new ArrayList<>();
        
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59))
                .orderByDesc(PurchaseOrder::getCreateTime);
        
        List<PurchaseOrder> orders = list(wrapper);
        for (PurchaseOrder order : orders) {
            Map<String, Object> record = new HashMap<>();
            record.put("orderId", order.getId());
            record.put("orderNo", order.getOrderNo());
            record.put("supplierName", order.getSupplierName());
            record.put("totalAmount", order.getTotalAmount());
            record.put("status", getStatusDescription(order.getStatus()));
            record.put("createTime", order.getCreateTime());
            record.put("approvalTime", order.getApprovalTime());
            
            report.add(record);
        }
        
        return report;
    }

    private String generateOrderNo() {
        return "PO" + System.currentTimeMillis();
    }

    private void calculateOrderAmount(PurchaseOrder order) {
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setTotalAmountWithTax(BigDecimal.ZERO);
        } else {
            BigDecimal taxAmount = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
            order.setTotalAmountWithTax(order.getTotalAmount().add(taxAmount));
        }
    }

    private String getStatusDescription(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审批";
            case 2: return "已审批";
            case 3: return "已下达";
            case 4: return "已取消";
            case 5: return "履行中";
            case 6: return "已完成";
            default: return "未知";
        }
    }
}