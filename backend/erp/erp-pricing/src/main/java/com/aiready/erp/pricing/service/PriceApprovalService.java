package com.aiready.erp.pricing.service;

import com.aiready.erp.pricing.entity.PriceApproval;
import com.aiready.erp.pricing.entity.PriceRule;
import com.aiready.erp.product.entity.ProductEntity;
import com.aiready.crm.customer.entity.CustomerEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceApprovalService {

    private final PriceApprovalMapper approvalMapper;
    private final ProductService productService;
    private final CustomerService customerService;
    private final PriceRuleService priceRuleService;
    private final NotificationService notificationService;

    public PriceApproval applyPriceChange(PriceApprovalApplyDTO dto) {
        ProductEntity product = productService.getById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("产品不存在");
        }

        CustomerEntity customer = null;
        if (dto.getCustomerId() != null) {
            customer = customerService.getById(dto.getCustomerId());
            if (customer == null) {
                throw new BusinessException("客户不存在");
            }
        }

        BigDecimal oldPrice = getProductCurrentPrice(product.getId(), dto.getCustomerId());
        BigDecimal newPrice = dto.getNewPrice();
        BigDecimal priceChange = newPrice.subtract(oldPrice);
        String priceChangeType = priceChange.compareTo(BigDecimal.ZERO) >= 0 ? "increase" : "decrease";

        PriceApproval approval = new PriceApproval();
        approval.setProductId(product.getId());
        approval.setProductCode(product.getCode());
        approval.setProductName(product.getName());
        approval.setCustomerId(dto.getCustomerId());
        approval.setCustomerName(customer != null ? customer.getName() : null);
        approval.setOldPrice(oldPrice);
        approval.setNewPrice(newPrice);
        approval.setPriceChange(priceChange.abs());
        approval.setPriceChangeType(priceChangeType);
        approval.setApprovalType(dto.getApprovalType());
        approval.setApprovalReason(dto.getApprovalReason());
        approval.setStatus("pending");
        approval.setApplicantId(dto.getApplicantId());
        approval.setApplyTime(LocalDateTime.now());

        if (dto.getEffectiveStartTime() != null) {
            approval.setEffectiveStartTime(dto.getEffectiveStartTime());
        }
        if (dto.getEffectiveEndTime() != null) {
            approval.setEffectiveEndTime(dto.getEffectiveEndTime());
        }

        approvalMapper.insert(approval);

        notificationService.sendApprovalNotification(approval);

        return approval;
    }

    @Transactional
    public PriceApproval approvePriceChange(Long approvalId, Long approverId, String remark) {
        PriceApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }

        if (!"pending".equals(approval.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        approval.setStatus("approved");
        approval.setApproverId(approverId);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);

        approvalMapper.updateById(approval);

        applyPriceChangeToRule(approval);

        notificationService.sendApproveResultNotification(approval, true);

        return approval;
    }

    @Transactional
    public PriceApproval rejectPriceChange(Long approvalId, Long approverId, String remark) {
        PriceApproval approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }

        if (!"pending".equals(approval.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        approval.setStatus("rejected");
        approval.setApproverId(approverId);
        approval.setApproveTime(LocalDateTime.now());
        approval.setApproveRemark(remark);

        approvalMapper.updateById(approval);

        notificationService.sendApproveResultNotification(approval, false);

        return approval;
    }

    private void applyPriceChangeToRule(PriceApproval approval) {
        PriceRule rule = new PriceRule();
        rule.setProductId(approval.getProductId());
        rule.setCustomerId(approval.getCustomerId());
        rule.setPrice(approval.getNewPrice());
        rule.setRuleType(approval.getCustomerId() != null ? "customer_specific" : "product_base");
        rule.setEffectiveStartTime(approval.getEffectiveStartTime() != null ? approval.getEffectiveStartTime() : System.currentTimeMillis());
        rule.setEffectiveEndTime(approval.getEffectiveEndTime());
        rule.setSourceApprovalId(approval.getId());
        rule.setStatus("active");

        priceRuleService.saveOrUpdateRule(rule);
    }

    private BigDecimal getProductCurrentPrice(Long productId, Long customerId) {
        if (customerId != null) {
            PriceRule customerRule = priceRuleService.getEffectiveRule(productId, customerId);
            if (customerRule != null) {
                return customerRule.getPrice();
            }
        }

        PriceRule productRule = priceRuleService.getEffectiveRule(productId, null);
        if (productRule != null) {
            return productRule.getPrice();
        }

        ProductEntity product = productService.getById(productId);
        return product != null && product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO;
    }

    public List<PriceApproval> getPendingApprovals() {
        return approvalMapper.selectList(
            new LambdaQueryWrapper<PriceApproval>()
                .eq(PriceApproval::getStatus, "pending")
                .orderByDesc(PriceApproval::getApplyTime)
        );
    }

    public List<PriceApproval> getApprovalsByStatus(String status) {
        return approvalMapper.selectList(
            new LambdaQueryWrapper<PriceApproval>()
                .eq(PriceApproval::getStatus, status)
                .orderByDesc(PriceApproval::getApplyTime)
        );
    }

    public List<PriceApproval> getApprovalsByApplicant(Long applicantId) {
        return approvalMapper.selectList(
            new LambdaQueryWrapper<PriceApproval>()
                .eq(PriceApproval::getApplicantId, applicantId)
                .orderByDesc(PriceApproval::getApplyTime)
        );
    }

    public PriceApproval getById(Long id) {
        return approvalMapper.selectById(id);
    }

    public PriceApprovalStatistics getStatistics() {
        List<PriceApproval> allApprovals = approvalMapper.selectList(null);

        PriceApprovalStatistics stats = new PriceApprovalStatistics();
        stats.setTotalCount(allApprovals.size());
        stats.setPendingCount(allApprovals.stream().filter(a -> "pending".equals(a.getStatus())).count());
        stats.setApprovedCount(allApprovals.stream().filter(a -> "approved".equals(a.getStatus())).count());
        stats.setRejectedCount(allApprovals.stream().filter(a -> "rejected".equals(a.getStatus())).count());

        stats.setTotalPriceIncrease(allApprovals.stream()
            .filter(a -> "approved".equals(a.getStatus()) && "increase".equals(a.getPriceChangeType()))
            .map(PriceApproval::getPriceChange)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        stats.setTotalPriceDecrease(allApprovals.stream()
            .filter(a -> "approved".equals(a.getStatus()) && "decrease".equals(a.getPriceChangeType()))
            .map(PriceApproval::getPriceChange)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        return stats;
    }
}