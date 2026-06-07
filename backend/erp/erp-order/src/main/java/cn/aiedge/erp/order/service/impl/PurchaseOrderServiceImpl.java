package cn.aiedge.erp.order.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.order.dto.PurchaseOrderApproveDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderCreateDTO;
import cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO;
import cn.aiedge.erp.order.entity.PurchaseOrder;
import cn.aiedge.erp.order.entity.PurchaseOrder.Status;
import cn.aiedge.erp.order.mapper.OrderCenterPurchaseOrderMapper;
import cn.aiedge.erp.order.service.IPurchaseOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("orderPurchaseOrderServiceImpl")
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<OrderCenterPurchaseOrderMapper, PurchaseOrder> implements IPurchaseOrderService {

    private final OrderCenterPurchaseOrderMapper purchaseOrderMapper;

    @Override
    public Page<PurchaseOrder> pagePurchaseOrders(Page<PurchaseOrder> page, Long tenantId, String orderNo, String supplierName,
                                                  Integer purchaseType, Integer status, Long buyerId,
                                                  LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        
        if (tenantId != null) {
            queryWrapper.eq(PurchaseOrder::getTenantId, tenantId);
        }
        
        if (StringUtils.hasText(orderNo)) {
            queryWrapper.like(PurchaseOrder::getPurchaseOrderNo, orderNo);
        }
        
        if (StringUtils.hasText(supplierName)) {
            queryWrapper.like(PurchaseOrder::getSupplierName, supplierName);
        }
        
        if (purchaseType != null) {
            queryWrapper.eq(PurchaseOrder::getPurchaseType, purchaseType);
        }
        
        if (status != null) {
            queryWrapper.eq(PurchaseOrder::getStatus, status);
        }
        
        if (buyerId != null) {
            queryWrapper.eq(PurchaseOrder::getBuyerId, buyerId);
        }
        
        if (startDate != null && endDate != null) {
            queryWrapper.between(PurchaseOrder::getCreateTime, startDate, endDate);
        }
        
        queryWrapper.eq(PurchaseOrder::getDeleted, 0)
                   .orderByDesc(PurchaseOrder::getCreateTime);
        
        return this.page(page, queryWrapper);
    }

    @Override
    public PurchaseOrder getPurchaseOrderDetail(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(PurchaseOrderCreateDTO dto) {
        log.info("创建采购订单: 供应商ID={}", dto.getSupplierId());
        
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        BeanUtils.copyProperties(dto, purchaseOrder);
        
        purchaseOrder.setStatus(Status.DRAFT.getCode());
        purchaseOrder.setPaymentStatus(0);
        purchaseOrder.setCreateTime(LocalDateTime.now());
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        BigDecimal totalAmount = dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO;
        purchaseOrder.setTotalAmount(totalAmount);
        
        this.save(purchaseOrder);
        log.info("采购订单创建成功, ID: {}", purchaseOrder.getId());
        
        return purchaseOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrder(PurchaseOrderCreateDTO dto) {
        if (dto.getId() == null) {
            throw BusinessException.badRequest("订单ID不能为空");
        }
        
        log.info("更新采购订单: ID={}", dto.getId());
        
        PurchaseOrder purchaseOrder = this.getById(dto.getId());
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (!canUpdateOrder(purchaseOrder.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许更新");
        }
        
        BeanUtils.copyProperties(dto, purchaseOrder);
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        BigDecimal totalAmount = dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO;
        purchaseOrder.setTotalAmount(totalAmount);
        
        this.updateById(purchaseOrder);
        log.info("采购订单更新成功, ID: {}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = this.getById(id);
        if (purchaseOrder != null) {
            purchaseOrder.setDeleted(1);
            purchaseOrder.setUpdateTime(LocalDateTime.now());
            this.updateById(purchaseOrder);
            log.info("采购订单删除成功, ID: {}", id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long orderId, String remark) {
        log.info("提交采购订单审批: ID={}", orderId);
        
        PurchaseOrder purchaseOrder = this.getById(orderId);
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (purchaseOrder.getStatus() != Status.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的订单才能提交审批");
        }
        
        purchaseOrder.setStatus(Status.SUBMITTED.getCode());
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单提交审批成功, ID: {}, 备注: {}", orderId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePurchaseOrder(Long orderId, PurchaseOrderApproveDTO approveDTO) {
        log.info("审批采购订单: ID={}, 审批结果={}", orderId, approveDTO.getApprovalResult());
        
        PurchaseOrder purchaseOrder = this.getById(orderId);
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (purchaseOrder.getStatus() != Status.SUBMITTED.getCode() && 
            purchaseOrder.getStatus() != Status.APPROVING.getCode()) {
            throw BusinessException.badRequest("只有已提交或审批中的订单才能审批");
        }
        
        if (approveDTO.getApprovalResult() == 1) {
            purchaseOrder.setStatus(Status.APPROVED.getCode());
        } else if (approveDTO.getApprovalResult() == 2) {
            purchaseOrder.setStatus(Status.REJECTED.getCode());
        }
        
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单审批完成, ID: {}, 结果: {}", orderId, approveDTO.getApprovalResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPurchaseOrder(Long orderId, String rejectReason) {
        PurchaseOrderApproveDTO rejectDTO = new PurchaseOrderApproveDTO();
        rejectDTO.setApprovalResult(2);
        rejectDTO.setApprovalOpinion(rejectReason);
        approvePurchaseOrder(orderId, rejectDTO);
    }

    @Override
    public List<PurchaseOrder> getPendingApprovalOrders(Long tenantId, Long approverId) {
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getTenantId, tenantId)
                   .in(PurchaseOrder::getStatus, Status.SUBMITTED.getCode(), Status.APPROVING.getCode())
                   .eq(PurchaseOrder::getDeleted, 0)
                   .orderByDesc(PurchaseOrder::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBySupplier(Long orderId, String confirmationNo, String remark) {
        log.info("供应商确认采购订单: ID={}", orderId);
        
        PurchaseOrder purchaseOrder = this.getById(orderId);
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (purchaseOrder.getStatus() != Status.APPROVED.getCode()) {
            throw BusinessException.badRequest("只有已批准的订单才能进行供应商确认");
        }
        
        purchaseOrder.setStatus(Status.CONFIRMED.getCode());
        purchaseOrder.setSupplierConfirmationNo(confirmationNo);
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("供应商确认成功, ID: {}, 确认单号: {}", orderId, confirmationNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String trackingNo, String logisticsCompany, LocalDateTime estimatedArrivalDate) {
        log.info("标记采购订单为已发货: ID={}", orderId);
        
        PurchaseOrder purchaseOrder = this.getById(orderId);
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (purchaseOrder.getStatus() != Status.CONFIRMED.getCode()) {
            throw BusinessException.badRequest("只有供应商已确认的订单才能标记为已发货");
        }
        
        purchaseOrder.setStatus(Status.SHIPPING.getCode());
        purchaseOrder.setTrackingNo(trackingNo);
        purchaseOrder.setLogisticsCompany(logisticsCompany);
        purchaseOrder.setEstimatedArrivalDate(estimatedArrivalDate);
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单发货: ID={}, 物流公司={}, 运单号={}", orderId, logisticsCompany, trackingNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveGoods(Long orderId, Integer receivedQuantity, String qualityCheckResult, String remark) {
        log.info("标记采购订单为已收货: ID={}", orderId);
        
        PurchaseOrder purchaseOrder = this.getById(orderId);
        if (purchaseOrder == null) {
            throw BusinessException.notFound("采购订单不存在");
        }
        
        if (purchaseOrder.getStatus() != Status.SHIPPING.getCode()) {
            throw BusinessException.badRequest("只有已发货的订单才能标记为已收货");
        }
        
        purchaseOrder.setStatus(Status.PARTIALLY_RECEIVED.getCode());
        purchaseOrder.setReceivedQuantity(receivedQuantity);
        purchaseOrder.setQualityCheckResult(qualityCheckResult);
        purchaseOrder.setUpdateTime(LocalDateTime.now());
        
        this.updateById(purchaseOrder);
        log.info("采购订单收货: ID={}, 收货数量={}", orderId, receivedQuantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long orderId, String invoiceNo, BigDecimal invoiceAmount, LocalDateTime invoiceDate) {
        PurchaseOrder order = this.getById(orderId);
        if (order != null) {
            order.setInvoiceNo(invoiceNo);
            order.setInvoiceAmount(invoiceAmount);
            order.setInvoiceDate(invoiceDate);
            order.setUpdateTime(LocalDateTime.now());
            this.updateById(order);
            log.info("采购订单发票提交: ID={}, 发票号={}", orderId, invoiceNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderStatus(Long orderId, Integer status) {
        PurchaseOrder order = this.getById(orderId);
        if (order != null) {
            order.setStatus(status);
            order.setUpdateTime(LocalDateTime.now());
            this.updateById(order);
            log.info("采购订单状态更新: ID={}, 新状态={}", orderId, status);
        }
    }

    @Override
    public PurchaseOrderStatisticsDTO getPurchaseStatistics(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("获取采购订单统计: {} - {}", startDate, endDate);
        
        PurchaseOrderStatisticsDTO statistics = new PurchaseOrderStatisticsDTO();
        statistics.setStartDate(startDate);
        statistics.setEndDate(endDate);
        
        LambdaQueryWrapper<PurchaseOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PurchaseOrder::getTenantId, tenantId)
                   .eq(PurchaseOrder::getDeleted, 0)
                   .between(PurchaseOrder::getCreateTime, startDate, endDate);
        
        List<PurchaseOrder> orders = this.list(queryWrapper);
        
        statistics.setTotalOrders(orders.size());
        statistics.setTotalAmount(orders.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        Map<String, Integer> statusCount = orders.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getStatus()), Collectors.summingInt(o -> 1)));
        statistics.setOrdersByStatus(statusCount);
        
        Map<String, Integer> purchaseTypeCount = orders.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getPurchaseType()), Collectors.summingInt(o -> 1)));
        statistics.setOrdersByPurchaseType(purchaseTypeCount);
        
        log.info("采购订单统计完成, 总计: {} 个订单", orders.size());
        return statistics;
    }

    @Override
    public byte[] generatePurchaseReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format) {
        log.info("生成采购报表: 租户ID={}, 格式={}", tenantId, format);
        return "PDF Report Content".getBytes();
    }

    @Override
    public byte[] exportPurchaseData(Long tenantId, LocalDateTime startDate, LocalDateTime endDate, String format) {
        log.info("导出采购数据: 租户ID={}, 格式={}", tenantId, format);
        return "Export Data Content".getBytes();
    }

    private boolean canUpdateOrder(Integer status) {
        return status != null && (status == Status.DRAFT.getCode() || status == Status.SUBMITTED.getCode());
    }
}