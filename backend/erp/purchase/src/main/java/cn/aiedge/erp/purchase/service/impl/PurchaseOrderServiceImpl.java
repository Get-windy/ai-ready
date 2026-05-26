package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import cn.aiedge.erp.purchase.enums.OrderStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseOrderItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseOrderService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 采购订单服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    private final PurchaseOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrder order) {
        // 生成订单号
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.DRAFT); // 草稿状态
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setCreateBy(StpUtil.getLoginIdAsLong());
        
        // 计算订单金额
        calculateOrderAmount(order);
        
        save(order);
        log.info("创建采购订单成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PurchaseOrder order) {
        // 检查订单状态
        PurchaseOrder existing = getById(order.getId());
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus().getValue() > OrderStatus.PENDING_APPROVAL.getValue()) {
            throw BusinessException.badRequest("订单已审批，无法修改");
        }
        
        order.setUpdateTime(LocalDateTime.now());
        order.setUpdateBy(StpUtil.getLoginIdAsLong());
        updateById(order);
        log.info("更新采购订单成功: orderId={}", order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        PurchaseOrder existing = getById(orderId);
        if (existing == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (existing.getStatus().getValue() > OrderStatus.PENDING_APPROVAL.getValue()) {
            throw BusinessException.badRequest("订单已审批，无法删除");
        }
        
        removeById(orderId);
        log.info("删除采购订单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交审批");
        }
        
        order.setStatus(OrderStatus.PENDING_APPROVAL); // 待审批
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("提交采购订单审批: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw BusinessException.badRequest("订单不在待审批状态");
        }
        
        order.setStatus(OrderStatus.APPROVED); // 已审批
        order.setApprovedBy(StpUtil.getLoginIdAsLong());
        order.setApprovedTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("审批通过采购订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        order.setStatus(OrderStatus.DRAFT); // 退回草稿
        order.setRemark(order.getRemark() + " [审批拒绝: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("审批拒绝采购订单: orderId={}, reason={}", orderId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus().getValue() >= OrderStatus.ISSUED.getValue()) {
            throw BusinessException.badRequest("订单已开始入库，无法取消");
        }
        
        order.setStatus(OrderStatus.CANCELLED); // 已取消
        order.setRemark(order.getRemark() + " [取消原因: " + reason + "]");
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("取消采购订单: orderId={}, reason={}", orderId, reason);
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
        return getById(orderId);
    }

    @Override
    public List<Object> getOrderItems(Long orderId) {
        // TODO: 实现订单明细查询
        return List.of();
    }

    // ==================== 新增方法 ====================

    @Override
    public PurchaseOrder submitOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setStatus(OrderStatus.PENDING_APPROVAL); // 待审批
        updateById(order);
        return order;
    }

    @Override
    public PurchaseOrder approveOrder(Long orderId, Long approverId, String comment) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setStatus(OrderStatus.APPROVED); // 已审批
        order.setApprovedBy(approverId);
        order.setApprovedTime(LocalDateTime.now());
        updateById(order);
        return order;
    }

    @Override
    public PurchaseOrder issueOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setStatus(OrderStatus.ISSUED); // 已下达
        updateById(order);
        return order;
    }

    @Override
    @Transactional
    public PurchaseOrder generateOrderFromContract(PurchaseContract contract, List<PurchaseOrderItem> items) {
        PurchaseOrder order = new PurchaseOrder();
        order.setContractId(contract.getId());
        order.setSupplierId(contract.getSupplierId());
        order.setSupplierName(contract.getSupplierName());
        order.setTotalAmount(contract.getTotalAmount());
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.DRAFT); // 草稿
        save(order);

        if (items != null && !items.isEmpty()) {
            items.forEach(item -> item.setOrderId(order.getId()));
            orderItemMapper.batchInsert(items);
        }

        return order;
    }

    @Override
    public PurchaseOrder startFulfillment(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setStatus(OrderStatus.IN_PROGRESS); // 执行中
        updateById(order);
        return order;
    }

    @Override
    public PurchaseOrder updateFulfillmentProgress(Long orderId, BigDecimal receivedAmount, BigDecimal fulfillmentPercent) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setReceivedAmount(receivedAmount);
        order.setFulfillmentPercent(fulfillmentPercent);
        updateById(order);
        return order;
    }

    @Override
    public PurchaseOrder completeOrder(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        order.setStatus(OrderStatus.COMPLETED); // 已完成
        updateById(order);
        return order;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "PO" + System.currentTimeMillis();
    }

    /**
     * 计算订单金额
     */
    private void calculateOrderAmount(PurchaseOrder order) {
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setTotalAmountWithTax(BigDecimal.ZERO);
        }
    }

    // ==================== 新增采购执行方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBySupplier(Long orderId) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (order.getStatus() != OrderStatus.APPROVED && order.getStatus() != OrderStatus.ISSUED) {
            throw BusinessException.badRequest("只有已审批或已下达的订单才能进行供应商确认");
        }
        
        order.setSupplierConfirmed(true);
        order.setSupplierConfirmTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("供应商确认订单: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String trackingNumber, LocalDateTime estimatedArrivalTime) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!order.getSupplierConfirmed()) {
            throw BusinessException.badRequest("订单未经过供应商确认，无法发货");
        }
        
        order.setShipped(true);
        order.setTrackingNumber(trackingNumber);
        order.setEstimatedArrivalTime(estimatedArrivalTime);
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("发货通知: orderId={}, trackingNumber={}", orderId, trackingNumber);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId, BigDecimal receivedQuantity, String qualityCheckResult) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!order.getShipped()) {
            throw BusinessException.badRequest("订单未发货，无法收货");
        }
        
        order.setReceived(true);
        order.setReceivedQuantity(receivedQuantity);
        order.setQualityCheckResult(qualityCheckResult);
        order.setReceiveTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        // 更新订单状态为部分入库或已完成
        BigDecimal totalQuantity = order.getTotalQuantity();
        if (receivedQuantity.compareTo(totalQuantity) < 0) {
            order.setStatus(OrderStatus.PARTIAL_RECEIVED);
        } else {
            order.setStatus(OrderStatus.COMPLETED);
        }
        
        updateById(order);
        log.info("收货确认: orderId={}, receivedQuantity={}, qualityCheckResult={}", 
                orderId, receivedQuantity, qualityCheckResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long orderId, String invoiceNumber, BigDecimal invoiceAmount, LocalDate invoiceDate) {
        PurchaseOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!order.getReceived()) {
            throw BusinessException.badRequest("订单未收货，无法提交发票");
        }
        
        order.setInvoiceSubmitted(true);
        order.setInvoiceNumber(invoiceNumber);
        order.setInvoiceAmount(invoiceAmount);
        order.setInvoiceDate(invoiceDate);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        log.info("发票提交: orderId={}, invoiceNumber={}, invoiceAmount={}", 
                orderId, invoiceNumber, invoiceAmount);
    }

    @Override
    public List<PurchaseOrder> getPendingApprovalOrders() {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getStatus, OrderStatus.PENDING_APPROVAL)
                .orderByDesc(PurchaseOrder::getCreateTime);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getPurchaseStatistics(Long tenantId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 查询订单总数
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59));
        
        long totalOrders = count(wrapper);
        statistics.put("totalOrders", totalOrders);
        
        // 查询订单总额
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
        
        // 按状态统计
        for (OrderStatus status : OrderStatus.values()) {
            wrapper.clear();
            wrapper.eq(PurchaseOrder::getTenantId, tenantId)
                    .eq(PurchaseOrder::getStatus, status)
                    .ge(startDate != null, PurchaseOrder::getCreateTime, startDate.atStartOfDay())
                    .le(endDate != null, PurchaseOrder::getCreateTime, endDate.atTime(23, 59, 59));
            long count = count(wrapper);
            statistics.put("status_" + status.getValue(), count);
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
            record.put("status", order.getStatus().getDescription());
            record.put("createTime", order.getCreateTime());
            record.put("approvedTime", order.getApprovedTime());
            
            report.add(record);
        }
        
        return report;
    }
}