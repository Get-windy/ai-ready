package cn.aiedge.erp.purchase.return.service.impl;

import cn.aiedge.erp.purchase.return.dto.PurchaseReturnDTO;
import cn.aiedge.erp.purchase.return.dto.PurchaseReturnItemDTO;
import cn.aiedge.erp.purchase.return.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.return.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.return.mapper.PurchaseReturnMapper;
import cn.aiedge.erp.purchase.return.mapper.PurchaseReturnItemMapper;
import cn.aiedge.erp.purchase.return.service.PurchaseReturnService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseReturnServiceImpl extends ServiceImpl<PurchaseReturnMapper, PurchaseReturn> implements PurchaseReturnService {

    private final PurchaseReturnItemMapper itemMapper;

    @Override
    @Transactional
    public Long createPurchaseReturn(PurchaseReturnDTO dto) {
        PurchaseReturn entity = convertToEntity(dto);
        entity.setReturnCode(generateReturnCode());
        entity.setStatus("draft");
        entity.setSupplierConfirmationStatus("pending");
        entity.setClaimStatus("pending");
        
        baseMapper.insert(entity);
        
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<PurchaseReturnItem> items = convertToItems(entity.getId(), dto.getItems());
            for (PurchaseReturnItem item : items) {
                itemMapper.insert(item);
            }
        }
        
        log.info("创建采购换货单成功: {}", entity.getReturnCode());
        return entity.getId();
    }

    @Override
    @Transactional
    public boolean updatePurchaseReturn(Long id, PurchaseReturnDTO dto) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        if (!"draft".equals(entity.getStatus())) {
            throw new RuntimeException("只有草稿状态的换货单可以修改");
        }
        
        updateEntityFromDTO(entity, dto);
        baseMapper.updateById(entity);
        
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturnItem::getReturnOrderId, id);
        itemMapper.delete(wrapper);
        
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<PurchaseReturnItem> items = convertToItems(id, dto.getItems());
            for (PurchaseReturnItem item : items) {
                itemMapper.insert(item);
            }
        }
        
        log.info("更新采购换货单成功: {}", entity.getReturnCode());
        return true;
    }

    @Override
    @Transactional
    public String submitForApproval(Long id) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        if (!"draft".equals(entity.getStatus())) {
            throw new RuntimeException("只有草稿状态的换货单可以提交审批");
        }
        
        entity.setStatus("submitted");
        entity.setCurrentApprovalNode("采购部门审批");
        entity.setApplyDate(LocalDateTime.now());
        baseMapper.updateById(entity);
        
        log.info("提交换货单审批成功: {}", entity.getReturnCode());
        return "process_" + entity.getReturnCode();
    }

    @Override
    @Transactional
    public boolean approve(Long id, Long approverId, String approverName, String comment) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        String currentStatus = entity.getStatus();
        if (!"submitted".equals(currentStatus) && !"under_review".equals(currentStatus)) {
            throw new RuntimeException("当前状态不允许审批");
        }
        
        entity.setStatus("approved");
        entity.setCurrentApprovalNode(null);
        entity.setSupplierConfirmationStatus("pending");
        baseMapper.updateById(entity);
        
        log.info("换货单审批通过: {}, 审批人: {}", entity.getReturnCode(), approverName);
        return true;
    }

    @Override
    @Transactional
    public boolean reject(Long id, Long approverId, String approverName, String comment) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setStatus("rejected");
        entity.setCurrentApprovalNode(null);
        entity.setRemark(comment);
        baseMapper.updateById(entity);
        
        log.info("换货单审批拒绝: {}, 审批人: {}, 原因: {}", entity.getReturnCode(), approverName, comment);
        return true;
    }

    @Override
    @Transactional
    public boolean supplierConfirm(Long id, String confirmationNote) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        if (!"approved".equals(entity.getStatus())) {
            throw new RuntimeException("只有已批准的换货单可以由供应商确认");
        }
        
        entity.setSupplierConfirmationStatus("confirmed");
        entity.setSupplierConfirmedAt(LocalDateTime.now());
        entity.setSupplierConfirmationNote(confirmationNote);
        entity.setStatus("supplier_confirmed");
        baseMapper.updateById(entity);
        
        log.info("供应商确认换货方案: {}", entity.getReturnCode());
        return true;
    }

    @Override
    @Transactional
    public boolean supplierReject(Long id, String rejectionReason) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setSupplierConfirmationStatus("rejected");
        entity.setSupplierConfirmedAt(LocalDateTime.now());
        entity.setSupplierConfirmationNote(rejectionReason);
        entity.setStatus("rejected");
        baseMapper.updateById(entity);
        
        log.info("供应商拒绝换货方案: {}, 原因: {}", entity.getReturnCode(), rejectionReason);
        return true;
    }

    @Override
    @Transactional
    public boolean updateReturnLogistics(Long id, String trackingNumber, String logisticsCompany) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setReturnTrackingNumber(trackingNumber);
        entity.setLogisticsCompany(logisticsCompany);
        entity.setStatus("returning");
        baseMapper.updateById(entity);
        
        log.info("更新退货物流信息: {}, 物流单号: {}", entity.getReturnCode(), trackingNumber);
        return true;
    }

    @Override
    @Transactional
    public boolean updateReplacementLogistics(Long id, String trackingNumber, String logisticsCompany) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setReplacementTrackingNumber(trackingNumber);
        entity.setLogisticsCompany(logisticsCompany);
        entity.setStatus("replacing");
        baseMapper.updateById(entity);
        
        log.info("更新换货物流信息: {}, 物流单号: {}", entity.getReturnCode(), trackingNumber);
        return true;
    }

    @Override
    @Transactional
    public boolean markReturnComplete(Long id, BigDecimal returnQuantity) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setStatus("returned");
        baseMapper.updateById(entity);
        
        log.info("标记退货完成: {}", entity.getReturnCode());
        return true;
    }

    @Override
    @Transactional
    public boolean markReplacementComplete(Long id, BigDecimal replacedQuantity) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setStatus("replaced");
        baseMapper.updateById(entity);
        
        log.info("标记换货完成: {}", entity.getReturnCode());
        return true;
    }

    @Override
    @Transactional
    public boolean complete(Long id, String completionNote) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setStatus("completed");
        entity.setActualCompleteDate(LocalDateTime.now());
        entity.setRemark(completionNote);
        baseMapper.updateById(entity);
        
        log.info("换货单完成: {}", entity.getReturnCode());
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(Long id, String cancellationReason) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        if ("completed".equals(entity.getStatus()) || "cancelled".equals(entity.getStatus())) {
            throw new RuntimeException("已完成或已取消的换货单不能取消");
        }
        
        entity.setStatus("cancelled");
        entity.setRemark(cancellationReason);
        baseMapper.updateById(entity);
        
        log.info("换货单取消: {}, 原因: {}", entity.getReturnCode(), cancellationReason);
        return true;
    }

    @Override
    public BigDecimal calculateReturnCost(Long id) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalCost = BigDecimal.ZERO;
        totalCost = totalCost.add(entity.getTotalAmount() != null ? entity.getTotalAmount() : BigDecimal.ZERO);
        totalCost = totalCost.add(entity.getShippingFee() != null ? entity.getShippingFee() : BigDecimal.ZERO);
        totalCost = totalCost.add(entity.getHandlingFee() != null ? entity.getHandlingFee() : BigDecimal.ZERO);
        totalCost = totalCost.add(entity.getOtherFee() != null ? entity.getOtherFee() : BigDecimal.ZERO);
        
        return totalCost;
    }

    @Override
    @Transactional
    public boolean updateClaim(Long id, BigDecimal claimAmount, String claimStatus, String claimNote) {
        PurchaseReturn entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new RuntimeException("换货单不存在: " + id);
        }
        
        entity.setClaimAmount(claimAmount);
        entity.setClaimStatus(claimStatus);
        entity.setRemark(claimNote);
        baseMapper.updateById(entity);
        
        log.info("更新索赔信息: {}, 索赔金额: {}", entity.getReturnCode(), claimAmount);
        return true;
    }

    @Override
    public List<PurchaseReturnDTO> getByPurchaseOrder(String purchaseOrderCode) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getPurchaseOrderCode, purchaseOrderCode);
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        wrapper.orderByDesc(PurchaseReturn::getCreatedAt);
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseReturnDTO> getBySupplier(Long supplierId, String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        
        if (startDate != null) {
            wrapper.ge(PurchaseReturn::getApplyDate, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(PurchaseReturn::getApplyDate, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        
        wrapper.orderByDesc(PurchaseReturn::getCreatedAt);
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseReturnDTO> getPendingApprovalList(Long userId, String role) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        wrapper.in(PurchaseReturn::getStatus, "submitted", "under_review");
        wrapper.orderByDesc(PurchaseReturn::getPriority);
        wrapper.orderByDesc(PurchaseReturn::getCreatedAt);
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseReturnDTO> getPendingSupplierConfirmation(Long supplierId) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        wrapper.eq(PurchaseReturn::getStatus, "approved");
        wrapper.eq(PurchaseReturn::getSupplierConfirmationStatus, "pending");
        wrapper.orderByDesc(PurchaseReturn::getCreatedAt);
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseReturnDTO> getOverdueReturns(int overdueDays) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        wrapper.in(PurchaseReturn::getStatus, "submitted", "approved", "supplier_confirmed", "returning", "replacing");
        
        LocalDateTime overdueDate = LocalDateTime.now().minusDays(overdueDays);
        wrapper.lt(PurchaseReturn::getExpectedCompleteDate, overdueDate);
        
        wrapper.orderByDesc(PurchaseReturn::getCreatedAt);
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public ReturnStatisticsDTO getStatistics(String startDate, String endDate, Long supplierId, String returnType) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturn::getDeleted, 0);
        
        if (startDate != null) {
            wrapper.ge(PurchaseReturn::getApplyDate, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(PurchaseReturn::getApplyDate, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        if (supplierId != null) {
            wrapper.eq(PurchaseReturn::getSupplierId, supplierId);
        }
        if (returnType != null) {
            wrapper.eq(PurchaseReturn::getReturnType, returnType);
        }
        
        List<PurchaseReturn> list = baseMapper.selectList(wrapper);
        
        ReturnStatisticsDTO stats = new ReturnStatisticsDTO();
        stats.setTotalCount(list.size());
        
        int completedCount = 0;
        int pendingCount = 0;
        int qualityCount = 0;
        int quantityCount = 0;
        int specificationCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalProcessingDays = 0;
        BigDecimal supplierCost = BigDecimal.ZERO;
        BigDecimal buyerCost = BigDecimal.ZERO;
        
        for (PurchaseReturn pr : list) {
            if ("completed".equals(pr.getStatus())) {
                completedCount++;
                if (pr.getActualCompleteDate() != null && pr.getApplyDate() != null) {
                    totalProcessingDays += ChronoUnit.DAYS.between(pr.getApplyDate(), pr.getActualCompleteDate());
                }
            } else {
                pendingCount++;
            }
            
            if ("quality".equals(pr.getReturnType())) qualityCount++;
            else if ("quantity".equals(pr.getReturnType())) quantityCount++;
            else if ("specification".equals(pr.getReturnType())) specificationCount++;
            
            if (pr.getTotalAmount() != null) {
                totalAmount = totalAmount.add(pr.getTotalAmount());
            }
            
            int ratio = pr.getSupplierShareRatio() != null ? pr.getSupplierShareRatio() : 100;
            BigDecimal cost = calculateReturnCost(pr.getId());
            supplierCost = supplierCost.add(cost.multiply(BigDecimal.valueOf(ratio)).divide(BigDecimal.valueOf(100)));
            buyerCost = buyerCost.add(cost.multiply(BigDecimal.valueOf(100 - ratio)).divide(BigDecimal.valueOf(100)));
        }
        
        stats.setCompletedCount(completedCount);
        stats.setPendingCount(pendingCount);
        stats.setQualityReturnCount(qualityCount);
        stats.setQuantityReturnCount(quantityCount);
        stats.setSpecificationReturnCount(specificationCount);
        stats.setTotalAmount(totalAmount);
        
        if (completedCount > 0) {
            stats.setAvgProcessingTime(BigDecimal.valueOf(totalProcessingDays / completedCount));
        } else {
            stats.setAvgProcessingTime(BigDecimal.ZERO);
        }
        
        stats.setSupplierCostShare(supplierCost);
        stats.setBuyerCostShare(buyerCost);
        
        return stats;
    }

    @Override
    public String exportReturns(String startDate, String endDate, String exportType) {
        log.info("导出换货单数据: {} - {}, 类型: {}", startDate, endDate, exportType);
        return "/exports/returns_" + startDate + "_" + endDate + "." + exportType;
    }

    private String generateReturnCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = baseMapper.selectCount(null);
        return "RTN" + dateStr + String.format("%08d", count + 1);
    }

    private PurchaseReturn convertToEntity(PurchaseReturnDTO dto) {
        PurchaseReturn entity = new PurchaseReturn();
        entity.setPurchaseOrderCode(dto.getPurchaseOrderCode());
        entity.setSupplierId(dto.getSupplierId());
        entity.setSupplierName(dto.getSupplierName());
        entity.setReturnType(dto.getReturnType());
        entity.setReturnReasonCode(dto.getReturnReasonCode());
        entity.setReturnReason(dto.getReturnReason());
        entity.setExpectedSolution(dto.getExpectedSolution());
        entity.setApplyDate(dto.getApplyDate());
        entity.setExpectedCompleteDate(dto.getExpectedCompleteDate());
        entity.setApplicantId(dto.getApplicantId());
        entity.setApplicantName(dto.getApplicantName());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setDepartmentName(dto.getDepartmentName());
        entity.setUrgencyLevel(dto.getUrgencyLevel());
        entity.setPriority(dto.getPriority());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setShippingFee(dto.getShippingFee());
        entity.setHandlingFee(dto.getHandlingFee());
        entity.setOtherFee(dto.getOtherFee());
        entity.setFeeAllocationMethod(dto.getFeeAllocationMethod());
        entity.setSupplierShareRatio(dto.getSupplierShareRatio());
        entity.setSupplierCommunicationSummary(dto.getSupplierCommunicationSummary());
        entity.setQualityReportPath(dto.getQualityReportPath());
        entity.setQualityReportName(dto.getQualityReportName());
        entity.setQualityIssueCode(dto.getQualityIssueCode());
        entity.setQualityIssueDescription(dto.getQualityIssueDescription());
        entity.setImpactLevel(dto.getImpactLevel());
        entity.setClaimAmount(dto.getClaimAmount());
        entity.setRemark(dto.getRemark());
        entity.setAttachments(dto.getAttachments());
        return entity;
    }

    private void updateEntityFromDTO(PurchaseReturn entity, PurchaseReturnDTO dto) {
        entity.setPurchaseOrderCode(dto.getPurchaseOrderCode());
        entity.setSupplierId(dto.getSupplierId());
        entity.setSupplierName(dto.getSupplierName());
        entity.setReturnType(dto.getReturnType());
        entity.setReturnReasonCode(dto.getReturnReasonCode());
        entity.setReturnReason(dto.getReturnReason());
        entity.setExpectedSolution(dto.getExpectedSolution());
        entity.setExpectedCompleteDate(dto.getExpectedCompleteDate());
        entity.setUrgencyLevel(dto.getUrgencyLevel());
        entity.setPriority(dto.getPriority());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setShippingFee(dto.getShippingFee());
        entity.setHandlingFee(dto.getHandlingFee());
        entity.setOtherFee(dto.getOtherFee());
        entity.setFeeAllocationMethod(dto.getFeeAllocationMethod());
        entity.setSupplierShareRatio(dto.getSupplierShareRatio());
        entity.setSupplierCommunicationSummary(dto.getSupplierCommunicationSummary());
        entity.setQualityReportPath(dto.getQualityReportPath());
        entity.setQualityReportName(dto.getQualityReportName());
        entity.setQualityIssueCode(dto.getQualityIssueCode());
        entity.setQualityIssueDescription(dto.getQualityIssueDescription());
        entity.setImpactLevel(dto.getImpactLevel());
        entity.setClaimAmount(dto.getClaimAmount());
        entity.setRemark(dto.getRemark());
        entity.setAttachments(dto.getAttachments());
    }

    private List<PurchaseReturnItem> convertToItems(Long returnOrderId, List<PurchaseReturnItemDTO> itemDTOs) {
        List<PurchaseReturnItem> items = new ArrayList<>();
        for (PurchaseReturnItemDTO dto : itemDTOs) {
            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setReturnOrderId(returnOrderId);
            item.setProductId(dto.getProductId());
            item.setProductCode(dto.getProductCode());
            item.setProductName(dto.getProductName());
            item.setProductSpec(dto.getProductSpec());
            item.setReturnQuantity(dto.getReturnQuantity());
            item.setReplacedQuantity(dto.getReplacedQuantity());
            item.setUnit(dto.getUnit());
            item.setUnitPrice(dto.getUnitPrice());
            item.setReturnAmount(dto.getReturnAmount());
            item.setReplacedAmount(dto.getReplacedAmount());
            item.setReturnReasonDetail(dto.getReturnReasonDetail());
            item.setRemark(dto.getRemark());
            items.add(item);
        }
        return items;
    }

    private PurchaseReturnDTO convertToDTO(PurchaseReturn entity) {
        PurchaseReturnDTO dto = new PurchaseReturnDTO();
        dto.setId(entity.getId());
        dto.setReturnCode(entity.getReturnCode());
        dto.setPurchaseOrderCode(entity.getPurchaseOrderCode());
        dto.setSupplierId(entity.getSupplierId());
        dto.setSupplierName(entity.getSupplierName());
        dto.setReturnType(entity.getReturnType());
        dto.setReturnReasonCode(entity.getReturnReasonCode());
        dto.setReturnReason(entity.getReturnReason());
        dto.setExpectedSolution(entity.getExpectedSolution());
        dto.setStatus(entity.getStatus());
        dto.setProcessInstanceId(entity.getProcessInstanceId());
        dto.setApplyDate(entity.getApplyDate());
        dto.setExpectedCompleteDate(entity.getExpectedCompleteDate());
        dto.setApplicantId(entity.getApplicantId());
        dto.setApplicantName(entity.getApplicantName());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(entity.getDepartmentName());
        dto.setUrgencyLevel(entity.getUrgencyLevel());
        dto.setPriority(entity.getPriority());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setShippingFee(entity.getShippingFee());
        dto.setHandlingFee(entity.getHandlingFee());
        dto.setOtherFee(entity.getOtherFee());
        dto.setFeeAllocationMethod(entity.getFeeAllocationMethod());
        dto.setSupplierShareRatio(entity.getSupplierShareRatio());
        dto.setSupplierCommunicationSummary(entity.getSupplierCommunicationSummary());
        dto.setSupplierConfirmationStatus(entity.getSupplierConfirmationStatus());
        dto.setSupplierConfirmedAt(entity.getSupplierConfirmedAt());
        dto.setSupplierConfirmationNote(entity.getSupplierConfirmationNote());
        dto.setReturnTrackingNumber(entity.getReturnTrackingNumber());
        dto.setReplacementTrackingNumber(entity.getReplacementTrackingNumber());
        dto.setLogisticsCompany(entity.getLogisticsCompany());
        dto.setQualityReportPath(entity.getQualityReportPath());
        dto.setQualityReportName(entity.getQualityReportName());
        dto.setQualityIssueCode(entity.getQualityIssueCode());
        dto.setQualityIssueDescription(entity.getQualityIssueDescription());
        dto.setImpactLevel(entity.getImpactLevel());
        dto.setClaimAmount(entity.getClaimAmount());
        dto.setClaimStatus(entity.getClaimStatus());
        dto.setRemark(entity.getRemark());
        dto.setAttachments(entity.getAttachments());
        
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturnItem::getReturnOrderId, entity.getId());
        List<PurchaseReturnItem> items = itemMapper.selectList(wrapper);
        if (items != null && !items.isEmpty()) {
            dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        }
        
        return dto;
    }

    private PurchaseReturnItemDTO convertItemToDTO(PurchaseReturnItem item) {
        PurchaseReturnItemDTO dto = new PurchaseReturnItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductCode(item.getProductCode());
        dto.setProductName(item.getProductName());
        dto.setProductSpec(item.getProductSpec());
        dto.setReturnQuantity(item.getReturnQuantity());
        dto.setReplacedQuantity(item.getReplacedQuantity());
        dto.setUnit(item.getUnit());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setReturnAmount(item.getReturnAmount());
        dto.setReplacedAmount(item.getReplacedAmount());
        dto.setReturnReasonDetail(item.getReturnReasonDetail());
        dto.setRemark(item.getRemark());
        return dto;
    }
}