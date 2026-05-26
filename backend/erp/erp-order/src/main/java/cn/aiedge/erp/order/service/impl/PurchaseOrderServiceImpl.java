package cn.aiedge.erp.order.service.impl;

import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import cn.aiedge.erp.order.entity.PurchaseOrder.PurchaseType;
import cn.aiedge.erp.order.entity.PurchaseOrder.PurchaseOrderStatus;
import cn.aiedge.erp.order.entity.PurchaseOrder.PaymentStatus;
import cn.aiedge.erp.order.mapper.PurchaseOrderMapper;
import cn.aiedge.erp.order.service.IPurchaseOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder createPurchaseOrder(PurchaseOrderCreateDTO createDTO) {
        log.info("创建采购订单: {}", createDTO.getOrderNumber());
        
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        BeanUtils.copyProperties(createDTO, purchaseOrder);
        
        // 设置默认值
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT.name());
        purchaseOrder.setApprovalStatus("PENDING");
        purchaseOrder.setPaymentStatus(PaymentStatus.UNPAID.name());
        purchaseOrder.setDeleted(false);
        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        // 计算总金额
        BigDecimal totalAmount = calculateTotalAmount(createDTO);
        purchaseOrder.setTotalAmount(totalAmount);
        
        // 保存采购订单
        this.save(purchaseOrder);
        log.info("采购订单创建成功, ID: {}", purchaseOrder.getId());
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrderCreateDTO updateDTO) {
        log.info("更新采购订单: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查状态是否允许更新
        if (!canUpdateOrder(purchaseOrder.getStatus())) {
            throw new RuntimeException("当前状态不允许更新");
        }
        
        BeanUtils.copyProperties(updateDTO, purchaseOrder);
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        // 重新计算总金额
        BigDecimal totalAmount = calculateTotalAmount(updateDTO);
        purchaseOrder.setTotalAmount(totalAmount);
        
        this.updateById(purchaseOrder);
        log.info("采购订单更新成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    public PurchaseOrder getPurchaseOrderById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<PurchaseOrder> getPurchaseOrdersByStatus(String status) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getStatus, status)
                   .eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        return this.list(queryWrapper);
    }

    @Override
    public IPage<PurchaseOrder> getPurchaseOrderPage(Integer page, Integer size, String status, Long supplierId) {
        Page<PurchaseOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(PurchaseOrder::getStatus, status);
        }
        
        if (supplierId != null) {
            queryWrapper.eq(PurchaseOrder::getSupplierId, supplierId);
        }
        
        queryWrapper.eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        
        return this.page(pageParam, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder submitForApproval(Long id) {
        log.info("提交采购订单审批: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否允许提交审批
        if (!PurchaseOrderStatus.DRAFT.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有草稿状态的订单才能提交审批");
        }
        
        // 更新状态
        purchaseOrder.setStatus(PurchaseOrderStatus.SUBMITTED.name());
        purchaseOrder.setApprovalStatus("PENDING_APPROVAL");
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单提交审批成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder approvePurchaseOrder(Long id, PurchaseOrderApproveDTO approveDTO) {
        log.info("审批采购订单: ID={}, 审批结果={}", id, approveDTO.getResult());
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否在审批中状态
        if (!PurchaseOrderStatus.SUBMITTED.name().equals(purchaseOrder.getStatus()) && 
            !PurchaseOrderStatus.APPROVAL_PENDING.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有已提交或审批中的订单才能审批");
        }
        
        // 更新审批结果
        if ("APPROVED".equals(approveDTO.getResult())) {
            purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED.name());
            purchaseOrder.setApprovalStatus("APPROVED");
        } else if ("REJECTED".equals(approveDTO.getResult())) {
            purchaseOrder.setStatus(PurchaseOrderStatus.REJECTED.name());
            purchaseOrder.setApprovalStatus("REJECTED");
        }
        
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单审批完成, ID: {}, 结果: {}", id, approveDTO.getResult());
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder confirmSupplier(Long id) {
        log.info("供应商确认采购订单: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否已批准
        if (!PurchaseOrderStatus.APPROVED.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有已批准的订单才能进行供应商确认");
        }
        
        purchaseOrder.setStatus(PurchaseOrderStatus.SUPPLIER_CONFIRMED.name());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("供应商确认成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder markAsShipped(Long id) {
        log.info("标记采购订单为已发货: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否供应商已确认
        if (!PurchaseOrderStatus.SUPPLIER_CONFIRMED.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有供应商已确认的订单才能标记为已发货");
        }
        
        purchaseOrder.setStatus(PurchaseOrderStatus.SHIPPED.name());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单标记为已发货成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder markAsReceived(Long id) {
        log.info("标记采购订单为已收货: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否已发货
        if (!PurchaseOrderStatus.SHIPPED.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有已发货的订单才能标记为已收货");
        }
        
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED.name());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单标记为已收货成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder markAsCompleted(Long id) {
        log.info("标记采购订单为已完成: ID={}", id);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否已收货
        if (!PurchaseOrderStatus.RECEIVED.name().equals(purchaseOrder.getStatus())) {
            throw new RuntimeException("只有已收货的订单才能标记为已完成");
        }
        
        purchaseOrder.setStatus(PurchaseOrderStatus.COMPLETED.name());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单标记为已完成成功, ID: {}", id);
        
        return purchaseOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPurchaseOrder(Long id, String reason) {
        log.info("取消采购订单: ID={}, 原因={}", id, reason);
        
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder == null) {
            throw new RuntimeException("采购订单不存在");
        }
        
        // 检查是否允许取消
        if (!canCancelOrder(purchaseOrder.getStatus())) {
            throw new RuntimeException("当前状态不允许取消");
        }
        
        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED.name());
        purchaseOrder.setCancellationReason(reason);
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单取消成功, ID: {}", id);
    }

    @Override
    public PurchaseOrderStatisticsDTO getPurchaseOrderStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取采购订单统计: {} - {}", startDate, endDate);
        
        PurchaseOrderStatisticsDTO statistics = new PurchaseOrderStatisticsDTO();
        
        // 获取日期范围内的订单
        List<PurchaseOrder> orders = purchaseOrderMapper.findByDateRange(startDate, endDate);
        
        // 基础统计
        statistics.setTotalOrders(orders.size());
        statistics.setTotalAmount(orders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        // 按状态统计
        Map<String, Long> statusCount = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getStatus, Collectors.counting()));
        statistics.setStatusDistribution(statusCount);
        
        // 按采购类型统计
        Map<String, Long> purchaseTypeCount = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getPurchaseType, Collectors.counting()));
        statistics.setPurchaseTypeDistribution(purchaseTypeCount);
        
        // 按付款状态统计
        Map<String, Long> paymentStatusCount = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getPaymentStatus, Collectors.counting()));
        statistics.setPaymentStatusDistribution(paymentStatusCount);
        
        // 按供应商统计
        Map<Long, Long> supplierCount = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getSupplierId, Collectors.counting()));
        statistics.setSupplierDistribution(supplierCount);
        
        // 计算平均订单金额
        if (!orders.isEmpty()) {
            BigDecimal avgAmount = statistics.getTotalAmount()
                    .divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
            statistics.setAverageOrderAmount(avgAmount);
        }
        
        log.info("采购订单统计完成, 总计: {} 个订单", orders.size());
        return statistics;
    }

    @Override
    public List<PurchaseOrder> getDuePaymentOrders(int daysBeforeDue) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(daysBeforeDue);
        return purchaseOrderMapper.findDuePaymentOrders(dueDate);
    }

    @Override
    public List<PurchaseOrder> getOrdersBySupplier(Long supplierId) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getSupplierId, supplierId)
                   .eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        return this.list(queryWrapper);
    }

    @Override
    public List<PurchaseOrder> getOrdersByBuyer(Long buyerId) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getBuyerId, buyerId)
                   .eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        return this.list(queryWrapper);
    }

    /**
     * 计算采购订单总金额
     */
    private BigDecimal calculateTotalAmount(PurchaseOrderCreateDTO createDTO) {
        // 这里简化处理，实际应该根据订单项计算
        return createDTO.getTotalAmount() != null ? createDTO.getTotalAmount() : BigDecimal.ZERO;
    }

    /**
     * 检查订单是否允许更新
     */
    private boolean canUpdateOrder(String status) {
        // 草稿状态允许更新
        return PurchaseOrderStatus.DRAFT.name().equals(status) || 
               PurchaseOrderStatus.SUBMITTED.name().equals(status);
    }

    /**
     * 检查订单是否允许取消
     */
    private boolean canCancelOrder(String status) {
        // 已完成、已取消、已拒绝状态不允许取消
        return !PurchaseOrderStatus.COMPLETED.name().equals(status) &&
               !PurchaseOrderStatus.CANCELLED.name().equals(status) &&
               !PurchaseOrderStatus.REJECTED.name().equals(status);
    }
    
    // ========== Controller兼容方法实现 ==========

    @Override
    public Page<PurchaseOrder> pagePurchaseOrders(Page<PurchaseOrder> page, Long tenantId, String orderNo, String supplierName,
                                                  Integer purchaseType, Integer status, Long buyerId,
                                                  LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        
        if (tenantId != null) {
            queryWrapper.eq(PurchaseOrder::getTenantId, tenantId);
        }
        
        if (StringUtils.hasText(orderNo)) {
            queryWrapper.like(PurchaseOrder::getOrderNumber, orderNo);
        }
        
        if (StringUtils.hasText(supplierName)) {
            queryWrapper.like(PurchaseOrder::getSupplierName, supplierName);
        }
        
        if (purchaseType != null) {
            queryWrapper.eq(PurchaseOrder::getPurchaseType, purchaseType.toString());
        }
        
        if (status != null) {
            queryWrapper.eq(PurchaseOrder::getStatus, status.toString());
        }
        
        if (buyerId != null) {
            queryWrapper.eq(PurchaseOrder::getBuyerId, buyerId);
        }
        
        if (startDate != null && endDate != null) {
            queryWrapper.between(PurchaseOrder::getOrderDate, startDate, endDate);
        }
        
        queryWrapper.eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        
        return this.page(page, queryWrapper);
    }

    @Override
    public PurchaseOrder getPurchaseOrderDetail(Long id) {
        return this.getById(id);
    }

    @Override
    public Long createPurchaseOrder(PurchaseOrderCreateDTO dto) {
        PurchaseOrder order = createPurchaseOrder((cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO) dto);
        return order.getId();
    }

    @Override
    public void updatePurchaseOrder(PurchaseOrderCreateDTO dto) {
        updatePurchaseOrder(dto.getId(), (cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO) dto);
    }

    @Override
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder != null) {
            purchaseOrder.setDeleted(true);
            purchaseOrder.setUpdatedAt(LocalDateTime.now());
            this.updateById(purchaseOrder);
        }
    }

    @Override
    public void submitForApproval(Long orderId, String remark) {
        PurchaseOrder order = submitForApproval(orderId);
        log.info("采购订单提交审批: ID={}, 备注={}", orderId, remark);
    }

    @Override
    public void approvePurchaseOrder(Long orderId, PurchaseOrderApproveDTO approveDTO) {
        approvePurchaseOrder(orderId, (cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO) approveDTO);
    }

    @Override
    public void rejectPurchaseOrder(Long orderId, String rejectReason) {
        PurchaseOrderApproveDTO rejectDTO = new PurchaseOrderApproveDTO();
        rejectDTO.setResult("REJECTED");
        rejectDTO.setRejectionReason(rejectReason);
        approvePurchaseOrder(orderId, rejectDTO);
    }

    @Override
    public List<PurchaseOrder> getPendingApprovalOrders(Long tenantId, Long approverId) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getTenantId, tenantId)
                   .in(PurchaseOrder::getStatus, 
                       PurchaseOrderStatus.SUBMITTED.name(), 
                       PurchaseOrderStatus.APPROVAL_PENDING.name())
                   .eq(PurchaseOrder::getDeleted, false)
                   .orderByDesc(PurchaseOrder::getCreatedAt);
        return this.list(queryWrapper);
    }

    @Override
    public void confirmBySupplier(Long orderId, String confirmationNo, String remark) {
        PurchaseOrder order = confirmSupplier(orderId);
        order.setSupplierConfirmationNo(confirmationNo);
        order.setUpdatedAt(LocalDateTime.now());
        this.updateById(order);
        log.info("供应商确认采购订单: ID={}, 确认单号={}, 备注={}", orderId, confirmationNo, remark);
    }

    @Override
    public void shipOrder(Long orderId, String trackingNo, String logisticsCompany, LocalDateTime estimatedArrivalDate) {
        PurchaseOrder order = markAsShipped(orderId);
        order.setTrackingNo(trackingNo);
        order.setLogisticsCompany(logisticsCompany);
        order.setEstimatedArrivalDate(estimatedArrivalDate);
        order.setUpdatedAt(LocalDateTime.now());
        this.updateById(order);
        log.info("采购订单发货: ID={}, 物流公司={}, 运单号={}", orderId, logisticsCompany, trackingNo);
    }

    @Override
    public void receiveGoods(Long orderId, Integer receivedQuantity, String qualityCheckResult, String remark) {
        PurchaseOrder order = markAsReceived(orderId);
        order.setReceivedQuantity(receivedQuantity);
        order.setQualityCheckResult(qualityCheckResult);
        order.setUpdatedAt(LocalDateTime.now());
        this.updateById(order);
        log.info("采购订单收货: ID={}, 收货数量={}, 质检结果={}", orderId, receivedQuantity, qualityCheckResult);
    }

    @Override
    public void submitInvoice(Long orderId, String invoiceNo, BigDecimal invoiceAmount, LocalDateTime invoiceDate) {
        PurchaseOrder order = this.getById(orderId);
        if (order != null) {
            order.setInvoiceNo(invoiceNo);
            order.setInvoiceAmount(invoiceAmount);
            order.setInvoiceDate(invoiceDate);
            order.setUpdatedAt(LocalDateTime.now());
            this.updateById(order);
            log.info("采购订单发票提交: ID={}, 发票号={}, 金额={}", orderId, invoiceNo, invoiceAmount);
        }
    }

    @Override
    public void updatePurchaseOrderStatus(Long orderId, Integer status) {
        PurchaseOrder order = this.getById(orderId);
        if (order != null) {
            order.setStatus(status.toString());
            order.setUpdatedAt(LocalDateTime.now());
            this.updateById(order);
        }
    }

    @Override
    public PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        return getPurchaseOrderStatistics(startDate, endDate);
    }

    @Override
    public byte[] generatePurchaseReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format) {
        // 简化的报表生成逻辑，实际应该使用报表引擎
        log.info("生成采购报表: 租户ID={}, 格式={}", tenantId, format);
        return "PDF Report Content".getBytes();
    }

    @Override
    public byte[] exportPurchaseData(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format) {
        // 简化的数据导出逻辑，实际应该生成Excel/CSV文件
        log.info("导出采购数据: 租户ID={}, 格式={}", tenantId, format);
        return "Export Data Content".getBytes();
    }
}