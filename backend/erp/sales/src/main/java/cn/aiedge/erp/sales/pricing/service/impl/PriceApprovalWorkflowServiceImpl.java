package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.erp.sales.pricing.dto.PriceApprovalRequestDTO;
import cn.aiedge.erp.sales.pricing.dto.PriceApprovalResponseDTO;
import cn.aiedge.erp.sales.pricing.dto.PriceApprovalSearchDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceApprovalRecord;
import cn.aiedge.erp.sales.pricing.entity.PriceSpecialApproval;
import cn.aiedge.erp.sales.pricing.enums.PriceApprovalStatus;
import cn.aiedge.erp.sales.pricing.repository.PriceApprovalRecordRepository;
import cn.aiedge.erp.sales.pricing.repository.PriceSpecialApprovalRepository;
import cn.aiedge.erp.sales.pricing.service.IPriceApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格审批工作流服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PriceApprovalWorkflowServiceImpl implements IPriceApprovalWorkflowService {
    
    private final PriceSpecialApprovalRepository approvalRepository;
    private final PriceApprovalRecordRepository recordRepository;
    
    // ========== 申请管理 ==========
    
    @Override
    public PriceSpecialApproval createApprovalRequest(PriceApprovalRequestDTO requestDTO) {
        log.info("创建价格特批申请: title={}, applicant={}", 
                requestDTO.getTitle(), requestDTO.getApplicantName());
        
        // 验证请求数据
        if (!requestDTO.isValid()) {
            throw new IllegalArgumentException("价格特批申请数据无效");
        }
        
        // 生成申请编号
        String approvalCode = generateApprovalCode();
        
        // 创建申请实体
        PriceSpecialApproval approval = PriceSpecialApproval.builder()
                .approvalCode(approvalCode)
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .applicantId(requestDTO.getApplicantId())
                .applicantName(requestDTO.getApplicantName())
                .applicantDepartmentId(requestDTO.getApplicantDepartmentId())
                .applicantDepartmentName(requestDTO.getApplicantDepartmentName())
                .productId(requestDTO.getProductId())
                .productName(requestDTO.getProductName())
                .productSku(requestDTO.getProductSku())
                .productCategoryId(requestDTO.getProductCategoryId())
                .productCategoryName(requestDTO.getProductCategoryName())
                .customerId(requestDTO.getCustomerId())
                .customerName(requestDTO.getCustomerName())
                .customerCode(requestDTO.getCustomerCode())
                .priceType(requestDTO.getPriceType())
                .standardPrice(requestDTO.getStandardPrice())
                .appliedPrice(requestDTO.getAppliedPrice())
                .discountRate(requestDTO.getDiscountRate())
                .validFrom(requestDTO.getValidFrom())
                .validTo(requestDTO.getValidTo())
                .purchaseQuantity(requestDTO.getPurchaseQuantity())
                .purchaseAmount(requestDTO.getPurchaseAmount())
                .urgencyLevel(requestDTO.getUrgencyLevel())
                .priorityLevel(requestDTO.getPriorityLevel())
                .status(PriceApprovalStatus.DRAFT)
                .currentApprovalLevel(0)
                .totalApprovalLevels(requestDTO.getTotalApprovalLevels())
                .level1ApproverId(requestDTO.getLevel1ApproverId())
                .level1ApproverName(requestDTO.getLevel1ApproverName())
                .level2ApproverId(requestDTO.getLevel2ApproverId())
                .level2ApproverName(requestDTO.getLevel2ApproverName())
                .level3ApproverId(requestDTO.getLevel3ApproverId())
                .level3ApproverName(requestDTO.getLevel3ApproverName())
                .finalApproverId(requestDTO.getFinalApproverId())
                .finalApproverName(requestDTO.getFinalApproverName())
                .expectedCompletionTime(requestDTO.getExpectedCompletionTime())
                .attachmentCount(requestDTO.getAttachments() != null ? requestDTO.getAttachments().size() : 0)
                .remark(requestDTO.getRemark())
                .tenantId(requestDTO.getTenantId())
                .deleted(0)
                .createTime(LocalDateTime.now())
                .createBy(requestDTO.getApplicantId())
                .updateTime(LocalDateTime.now())
                .updateBy(requestDTO.getApplicantId())
                .build();
        
        // 保存申请
        PriceSpecialApproval savedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(savedApproval, PriceApprovalRecord.ActionType.CREATE,
                "创建价格特批申请", requestDTO.getApplicantId(), requestDTO.getApplicantName(),
                PriceApprovalRecord.OperatorRole.APPLICANT, null);
        
        log.info("价格特批申请创建成功: id={}, code={}", savedApproval.getId(), savedApproval.getApprovalCode());
        return savedApproval;
    }
    
    @Override
    public PriceSpecialApproval submitApprovalRequest(Long approvalId, Long submitterId) {
        log.info("提交价格特批申请: id={}, submitter={}", approvalId, submitterId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证状态转换
        if (!approval.getStatus().canSubmit()) {
            throw new IllegalStateException("当前状态不能提交审批: " + approval.getStatus());
        }
        
        // 验证审批人信息
        validateApprovers(approval);
        
        // 更新状态
        approval.setStatus(PriceApprovalStatus.PENDING_APPROVAL);
        approval.setCurrentApprovalLevel(1);
        approval.setSubmittedAt(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(submitterId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.SUBMIT,
                "提交价格特批申请", submitterId, getOperatorName(submitterId),
                PriceApprovalRecord.OperatorRole.APPLICANT, null);
        
        // 发送通知给一级审批人
        sendApprovalNotification(approvalId, "SUBMISSION");
        
        log.info("价格特批申请提交成功: id={}, status={}", approvalId, updatedApproval.getStatus());
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval updateApprovalRequest(Long approvalId, PriceApprovalRequestDTO requestDTO) {
        log.info("更新价格特批申请: id={}", approvalId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 只能更新草稿状态的申请
        if (approval.getStatus() != PriceApprovalStatus.DRAFT) {
            throw new IllegalStateException("只能更新草稿状态的申请");
        }
        
        // 更新字段
        BeanUtils.copyProperties(requestDTO, approval, 
                "id", "approvalCode", "status", "currentApprovalLevel", "createTime", "createBy");
        
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(requestDTO.getApplicantId());
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.MODIFY,
                "更新价格特批申请", requestDTO.getApplicantId(), requestDTO.getApplicantName(),
                PriceApprovalRecord.OperatorRole.APPLICANT, null);
        
        log.info("价格特批申请更新成功: id={}", approvalId);
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval withdrawApprovalRequest(Long approvalId, Long operatorId, String reason) {
        log.info("撤回价格特批申请: id={}, operator={}", approvalId, operatorId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证状态转换
        if (!approval.getStatus().canWithdraw()) {
            throw new IllegalStateException("当前状态不能撤回申请: " + approval.getStatus());
        }
        
        // 更新状态
        approval.setStatus(PriceApprovalStatus.WITHDRAWN);
        approval.setWithdrawnAt(LocalDateTime.now());
        approval.setWithdrawReason(reason);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(operatorId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.WITHDRAW,
                "撤回价格特批申请: " + reason, operatorId, getOperatorName(operatorId),
                getOperatorRole(operatorId, approval), null);
        
        // 发送通知
        sendApprovalNotification(approvalId, "WITHDRAWAL");
        
        log.info("价格特批申请撤回成功: id={}", approvalId);
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval cancelApprovalRequest(Long approvalId, Long operatorId, String reason) {
        log.info("取消价格特批申请: id={}, operator={}", approvalId, operatorId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证状态转换
        if (!approval.getStatus().canCancel()) {
            throw new IllegalStateException("当前状态不能取消申请: " + approval.getStatus());
        }
        
        // 更新状态
        approval.setStatus(PriceApprovalStatus.CANCELLED);
        approval.setCancelledAt(LocalDateTime.now());
        approval.setCancelReason(reason);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(operatorId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.CANCEL,
                "取消价格特批申请: " + reason, operatorId, getOperatorName(operatorId),
                getOperatorRole(operatorId, approval), null);
        
        log.info("价格特批申请取消成功: id={}", approvalId);
        return updatedApproval;
    }
    
    // ========== 审批操作 ==========
    
    @Override
    public PriceSpecialApproval approve(Long approvalId, Long approverId, String comments) {
        log.info("审批通过: id={}, approver={}", approvalId, approverId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证审批权限
        validateApprovalPermission(approval, approverId);
        
        // 获取下一级状态
        PriceApprovalStatus nextStatus = approval.getStatus().getNextApprovalLevel();
        
        // 更新状态
        updateApprovalStatus(approval, nextStatus, approverId);
        
        // 设置审批时间
        setApprovalTime(approval, approval.getCurrentApprovalLevel());
        
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.APPROVE,
                "审批通过: " + comments, approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), PriceApprovalRecord.Decision.APPROVED);
        
        // 发送通知
        sendApprovalNotification(approvalId, "APPROVAL");
        
        // 如果是最终审批通过，执行后续操作
        if (updatedApproval.getStatus() == PriceApprovalStatus.APPROVED) {
            onFinalApproval(updatedApproval);
        }
        
        log.info("价格特批申请审批通过: id={}, level={}", approvalId, approval.getCurrentApprovalLevel());
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval reject(Long approvalId, Long approverId, String rejectReason, String suggestions) {
        log.info("审批拒绝: id={}, approver={}", approvalId, approverId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证审批权限
        validateApprovalPermission(approval, approverId);
        
        // 获取拒绝状态
        PriceApprovalStatus rejectStatus = approval.getStatus().getRejectionStatus(approval.getCurrentApprovalLevel());
        
        // 更新状态
        approval.setStatus(rejectStatus);
        approval.setRejectedAt(LocalDateTime.now());
        approval.setRejectReason(rejectReason);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.REJECT,
                "审批拒绝: " + rejectReason + (suggestions != null ? " 建议: " + suggestions : ""),
                approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), PriceApprovalRecord.Decision.REJECTED);
        
        // 发送通知
        sendApprovalNotification(approvalId, "REJECTION");
        
        log.info("价格特批申请审批拒绝: id={}, reason={}", approvalId, rejectReason);
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval approveWithConditions(Long approvalId, Long approverId, String conditions, String comments) {
        log.info("有条件批准: id={}, approver={}", approvalId, approverId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证审批权限
        validateApprovalPermission(approval, approverId);
        
        // 获取下一级状态
        PriceApprovalStatus nextStatus = approval.getStatus().getNextApprovalLevel();
        
        // 更新状态
        updateApprovalStatus(approval, nextStatus, approverId);
        
        // 设置审批时间
        setApprovalTime(approval, approval.getCurrentApprovalLevel());
        
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.APPROVE,
                "有条件批准: " + conditions + " | 备注: " + comments,
                approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), PriceApprovalRecord.Decision.CONDITIONAL);
        
        // 发送通知
        sendApprovalNotification(approvalId, "CONDITIONAL_APPROVAL");
        
        log.info("价格特批申请有条件批准: id={}, conditions={}", approvalId, conditions);
        return updatedApproval;
    }
    
    @Override
    public PriceSpecialApproval returnForModification(Long approvalId, Long approverId, String reason, String requiredChanges) {
        log.info("退回修改: id={}, approver={}", approvalId, approverId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证审批权限
        validateApprovalPermission(approval, approverId);
        
        // 更新状态
        approval.setStatus(PriceApprovalStatus.DRAFT);
        approval.setCurrentApprovalLevel(0);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.REJECT,
                "退回修改: " + reason + " | 需要修改: " + requiredChanges,
                approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), PriceApprovalRecord.Decision.RETURNED);
        
        // 发送通知
        sendApprovalNotification(approvalId, "RETURN_FOR_MODIFICATION");
        
        log.info("价格特批申请退回修改: id={}, reason={}", approvalId, reason);
        return updatedApproval;
    }
    
    // ========== 查询方法 ==========
    
    @Override
    public PriceSpecialApproval getApprovalById(Long approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("价格特批申请不存在: " + approvalId));
    }
    
    @Override
    public PriceSpecialApproval getApprovalByCode(String approvalCode) {
        return approvalRepository.findByApprovalCode(approvalCode)
                .orElseThrow(() -> new RuntimeException("价格特批申请不存在: " + approvalCode));
    }
    
    @Override
    public Page<PriceSpecialApproval> searchApprovals(PriceApprovalSearchDTO searchDTO, Pageable pageable) {
        log.debug("搜索价格特批申请: {}", searchDTO);
        
        Specification<PriceSpecialApproval> spec = buildSearchSpecification(searchDTO);
        return approvalRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<PriceSpecialApproval> getPendingApprovals(Long approverId, Pageable pageable) {
        log.debug("查询待我审批的申请: approver={}", approverId);
        
        // 构建查询条件
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态为待审批或审批中
            predicates.add(cb.or(
                cb.equal(root.get("status"), PriceApprovalStatus.PENDING_APPROVAL),
                cb.equal(root.get("status"), PriceApprovalStatus.UNDER_REVIEW)
            ));
            
            // 根据当前审批级别匹配审批人
            predicates.add(cb.or(
                cb.and(
                    cb.equal(root.get("currentApprovalLevel"), 1),
                    cb.equal(root.get("level1ApproverId"), approverId)
                ),
                cb.and(
                    cb.equal(root.get("currentApprovalLevel"), 2),
                    cb.equal(root.get("level2ApproverId"), approverId)
                ),
                cb.and(
                    cb.equal(root.get("currentApprovalLevel"), 3),
                    cb.equal(root.get("level3ApproverId"), approverId)
                ),
                cb.and(
                    cb.equal(root.get("currentApprovalLevel"), 4),
                    cb.equal(root.get("finalApproverId"), approverId)
                )
            ));
            
            // 未删除
            predicates.add(cb.equal(root.get("deleted"), 0));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return approvalRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<PriceSpecialApproval> getMySubmittedApprovals(Long applicantId, Pageable pageable) {
        log.debug("查询我提交的申请: applicant={}", applicantId);
        
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("applicantId"), applicantId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return approvalRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<PriceSpecialApproval> getCompletedApprovals(Long operatorId, Pageable pageable) {
        log.debug("查询已完成的审批申请: operator={}", operatorId);
        
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态为已完成（通过、拒绝、撤回、取消、过期）
            predicates.add(cb.or(
                cb.equal(root.get("status"), PriceApprovalStatus.APPROVED),
                cb.equal(root.get("status"), PriceApprovalStatus.REJECTED),
                cb.equal(root.get("status"), PriceApprovalStatus.WITHDRAWN),
                cb.equal(root.get("status"), PriceApprovalStatus.CANCELLED),
                cb.equal(root.get("status"), PriceApprovalStatus.EXPIRED)
            ));
            
            // 申请人或审批人相关
            predicates.add(cb.or(
                cb.equal(root.get("applicantId"), operatorId),
                cb.equal(root.get("level1ApproverId"), operatorId),
                cb.equal(root.get("level2ApproverId"), operatorId),
                cb.equal(root.get("level3ApproverId"), operatorId),
                cb.equal(root.get("finalApproverId"), operatorId)
            ));
            
            predicates.add(cb.equal(root.get("deleted"), 0));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return approvalRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<PriceSpecialApproval> getExpiredApprovals(Pageable pageable) {
        log.debug("查询过期的审批申请");
        
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态为待审批或审批中
            predicates.add(cb.or(
                cb.equal(root.get("status"), PriceApprovalStatus.PENDING_APPROVAL),
                cb.equal(root.get("status"), PriceApprovalStatus.UNDER_REVIEW)
            ));
            
            // 预计完成时间已过
            predicates.add(cb.lessThan(root.get("expectedCompletionTime"), LocalDateTime.now()));
            
            predicates.add(cb.equal(root.get("deleted"), 0));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return approvalRepository.findAll(spec, pageable);
    }
    
    @Override
    public Page<PriceSpecialApproval> getUrgentApprovals(Pageable pageable) {
        log.debug("查询紧急审批申请");
        
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 紧急程度为1或2
            predicates.add(cb.or(
                cb.equal(root.get("urgencyLevel"), 1),
                cb.equal(root.get("urgencyLevel"), 2)
            ));
            
            // 状态为待审批或审批中
            predicates.add(cb.or(
                cb.equal(root.get("status"), PriceApprovalStatus.PENDING_APPROVAL),
                cb.equal(root.get("status"), PriceApprovalStatus.UNDER_REVIEW)
            ));
            
            predicates.add(cb.equal(root.get("deleted"), 0));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return approvalRepository.findAll(spec, pageable);
    }
    
    // ========== 审批记录管理 ==========
    
    @Override
    public List<PriceApprovalRecord> getApprovalRecords(Long approvalId) {
        log.debug("获取审批记录: approvalId={}", approvalId);
        
        return recordRepository.findByApprovalIdOrderByCreateTimeAsc(approvalId);
    }
    
    @Override
    public PriceApprovalResponseDTO getApprovalHistory(Long approvalId) {
        log.debug("获取审批历史: approvalId={}", approvalId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        List<PriceApprovalRecord> records = getApprovalRecords(approvalId);
        
        // 构建响应DTO
        return PriceApprovalResponseDTO.builder()
                .approval(approval)
                .records(records)
                .totalRecords(records.size())
                .build();
    }
    
    @Override
    public PriceApprovalRecord addApprovalComment(Long approvalId, Long operatorId, String comments) {
        log.debug("添加审批备注: approvalId={}, operator={}", approvalId, operatorId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        return createApprovalRecord(approval, PriceApprovalRecord.ActionType.COMMENT,
                "添加备注: " + comments, operatorId, getOperatorName(operatorId),
                getOperatorRole(operatorId, approval), null);
    }
    
    // ========== 统计方法 ==========
    
    @Override
    public ApprovalStatisticsDTO getApprovalStatistics(PriceApprovalSearchDTO searchDTO) {
        log.debug("获取审批统计: {}", searchDTO);
        
        Specification<PriceSpecialApproval> spec = buildSearchSpecification(searchDTO);
        List<PriceSpecialApproval> approvals = approvalRepository.findAll(spec);
        
        ApprovalStatisticsDTO statistics = new ApprovalStatisticsDTO();
        statistics.setTotalCount((long) approvals.size());
        statistics.setPendingCount(approvals.stream()
                .filter(a -> a.getStatus().isInProgress())
                .count());
        statistics.setApprovedCount(approvals.stream()
                .filter(a -> a.getStatus().isApproved())
                .count());
        statistics.setRejectedCount(approvals.stream()
                .filter(a -> a.getStatus().isRejected())
                .count());
        statistics.setWithdrawnCount(approvals.stream()
                .filter(a -> a.getStatus() == PriceApprovalStatus.WITHDRAWN)
                .count());
        statistics.setExpiredCount(approvals.stream()
                .filter(a -> a.getStatus() == PriceApprovalStatus.EXPIRED)
                .count());
        statistics.setUrgentCount(approvals.stream()
                .filter(PriceSpecialApproval::isUrgent)
                .count());
        
        // 计算审批率
        long totalProcessed = statistics.getApprovedCount() + statistics.getRejectedCount();
        if (totalProcessed > 0) {
            statistics.setApprovalRate(BigDecimal.valueOf(statistics.getApprovedCount())
                    .divide(BigDecimal.valueOf(totalProcessed), 4, java.math.RoundingMode.HALF_UP));
        } else {
            statistics.setApprovalRate(BigDecimal.ZERO);
        }
        
        // 计算平均审批时间
        List<PriceSpecialApproval> completedApprovals = approvals.stream()
                .filter(a -> a.getStatus().isCompleted() && a.getSubmittedAt() != null)
                .collect(Collectors.toList());
        
        if (!completedApprovals.isEmpty()) {
            long totalMinutes = completedApprovals.stream()
                    .mapToLong(PriceSpecialApproval::getApprovalDurationMinutes)
                    .sum();
            statistics.setAverageApprovalTime(BigDecimal.valueOf(totalMinutes)
                    .divide(BigDecimal.valueOf(completedApprovals.size()), 2, java.math.RoundingMode.HALF_UP));
        } else {
            statistics.setAverageApprovalTime(BigDecimal.ZERO);
        }
        
        return statistics;
    }
    
    // ========== 工作流管理 ==========
    
    @Override
    public PriceSpecialApproval reassignApprover(Long approvalId, Long approverId, Long newApproverId, String reason) {
        log.info("重新分配审批人: id={}, from={}, to={}", approvalId, approverId, newApproverId);
        
        PriceSpecialApproval approval = getApprovalById(approvalId);
        
        // 验证当前审批人权限
        if (!approverId.equals(approval.getCurrentApproverId())) {
            throw new IllegalStateException("无权重新分配审批人");
        }
        
        // 根据当前审批级别更新审批人
        switch (approval.getCurrentApprovalLevel()) {
            case 1:
                approval.setLevel1ApproverId(newApproverId);
                approval.setLevel1ApproverName(getOperatorName(newApproverId));
                break;
            case 2:
                approval.setLevel2ApproverId(newApproverId);
                approval.setLevel2ApproverName(getOperatorName(newApproverId));
                break;
            case 3:
                approval.setLevel3ApproverId(newApproverId);
                approval.setLevel3ApproverName(getOperatorName(newApproverId));
                break;
            case 4:
                approval.setFinalApproverId(newApproverId);
                approval.setFinalApproverName(getOperatorName(newApproverId));
                break;
            default:
                throw new IllegalStateException("无效的审批级别: " + approval.getCurrentApprovalLevel());
        }
        
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
        
        // 保存更新
        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);
        
        // 创建审批记录
        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.REASSIGN,
                "重新分配审批人: " + reason, approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), null);
        
        // 发送通知给新审批人
        sendApprovalNotification(approvalId, "REASSIGNMENT");
        
        log.info("审批人重新分配成功: id={}, newApprover={}", approvalId, newApproverId);
        return updatedApproval;
    }
    
    // ========== 通知和提醒 ==========
    
    @Override
    public boolean sendApprovalNotification(Long approvalId, String notificationType) {
        log.debug("发送审批通知: id={}, type={}", approvalId, notificationType);
        
        try {
            PriceSpecialApproval approval = getApprovalById(approvalId);
            
            // 根据通知类型确定接收人和内容
            switch (notificationType) {
                case "SUBMISSION":
                    // 通知一级审批人
                    sendNotification(approval.getLevel1ApproverId(), "价格特批申请待审批", 
                            String.format("您有一个新的价格特批申请需要审批: %s", approval.getTitle()));
                    break;
                    
                case "APPROVAL":
                    // 通知下一级审批人或申请人
                    if (approval.getStatus() == PriceApprovalStatus.APPROVED) {
                        sendNotification(approval.getApplicantId(), "价格特批申请已批准",
                                String.format("您的价格特批申请已批准: %s", approval.getTitle()));
                    } else if (approval.getCurrentApprovalLevel() < approval.getTotalApprovalLevels()) {
                        // 通知下一级审批人
                        Long nextApproverId = getNextApproverId(approval);
                        if (nextApproverId != null) {
                            sendNotification(nextApproverId, "价格特批申请待审批",
                                    String.format("您有一个价格特批申请需要审批: %s", approval.getTitle()));
                        }
                    }
                    break;
                    
                case "REJECTION":
                    // 通知申请人
                    sendNotification(approval.getApplicantId(), "价格特批申请被拒绝",
                            String.format("您的价格特批申请被拒绝: %s", approval.getTitle()));
                    break;
                    
                case "WITHDRAWAL":
                    // 通知相关审批人
                    sendNotification(approval.getCurrentApproverId(), "价格特批申请已撤回",
                            String.format("价格特批申请已撤回: %s", approval.getTitle()));
                    break;
                    
                default:
                    log.warn("未知的通知类型: {}", notificationType);
                    return false;
            }
            
            // 更新通知状态
            if (notificationType.equals("SUBMISSION")) {
                approval.setNotifiedApprovers(true);
            } else if (notificationType.equals("APPROVAL") && approval.getStatus() == PriceApprovalStatus.APPROVED) {
                approval.setNotifiedApplicant(true);
            }
            approvalRepository.save(approval);
            
            return true;
            
        } catch (Exception e) {
            log.error("发送审批通知失败: id={}, type={}, error={}", approvalId, notificationType, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean sendApprovalReminder(Long approvalId, Long approverId) {
        log.debug("发送审批提醒: id={}, approver={}", approvalId, approverId);
        
        try {
            PriceSpecialApproval approval = getApprovalById(approvalId);
            
            // 验证审批人
            if (!approverId.equals(approval.getCurrentApproverId())) {
                log.warn("审批人ID不匹配: expected={}, actual={}", approval.getCurrentApproverId(), approverId);
                return false;
            }
            
            // 发送提醒
            sendNotification(approverId, "价格特批申请审批提醒",
                    String.format("您有一个价格特批申请需要尽快审批: %s (预计完成时间: %s)", 
                            approval.getTitle(), approval.getExpectedCompletionTime()));
            
            return true;
            
        } catch (Exception e) {
            log.error("发送审批提醒失败: id={}, approver={}, error={}", approvalId, approverId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public int sendExpirationReminders() {
        log.debug("批量发送过期提醒");
        
        int sentCount = 0;
        
        // 查询即将过期的申请
        LocalDateTime reminderTime = LocalDateTime.now().plusHours(24); // 24小时后过期
        Pageable pageable = Pageable.unpaged(); // 获取所有
        
        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态为待审批或审批中
            predicates.add(cb.or(
                cb.equal(root.get("status"), PriceApprovalStatus.PENDING_APPROVAL),
                cb.equal(root.get("status"), PriceApprovalStatus.UNDER_REVIEW)
            ));
            
            // 预计完成时间在24小时内
            predicates.add(cb.between(root.get("expectedCompletionTime"), 
                    LocalDateTime.now(), reminderTime));
            
            predicates.add(cb.equal(root.get("deleted"), 0));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        List<PriceSpecialApproval> expiringApprovals = approvalRepository.findAll(spec, pageable)
                .getContent();
        
        // 发送提醒
        for (PriceSpecialApproval approval : expiringApprovals) {
            Long approverId = approval.getCurrentApproverId();
            if (approverId != null && sendApprovalReminder(approval.getId(), approverId)) {
                sentCount++;
            }
        }
        
        log.info("发送过期提醒完成: total={}, sent={}", expiringApprovals.size(), sentCount);
        return sentCount;
    }
    
    // ========== 批量操作 ==========
    
    @Override
    public BatchApprovalResult batchApprove(List<Long> approvalIds, Long approverId, String comments) {
        log.info("批量审批通过: count={}, approver={}", approvalIds.size(), approverId);
        
        BatchApprovalResult result = new BatchApprovalResult();
        result.setTotalCount(approvalIds.size());
        result.setSuccessfulIds(new ArrayList<>());
        result.setFailedReasons(new HashMap<>());
        
        for (Long approvalId : approvalIds) {
            try {
                approve(approvalId, approverId, comments);
                result.getSuccessfulIds().add(approvalId);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailedReasons().put(approvalId, e.getMessage());
                log.error("批量审批失败: id={}, error={}", approvalId, e.getMessage(), e);
            }
        }
        
        log.info("批量审批完成: total={}, success={}, failure={}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        return result;
    }
    
    @Override
    public BatchApprovalResult batchReject(List<Long> approvalIds, Long approverId, String rejectReason) {
        log.info("批量审批拒绝: count={}, approver={}", approvalIds.size(), approverId);

        BatchApprovalResult result = new BatchApprovalResult();
        result.setTotalCount(approvalIds.size());
        result.setSuccessfulIds(new ArrayList<>());
        result.setFailedReasons(new HashMap<>());

        for (Long approvalId : approvalIds) {
            try {
                reject(approvalId, approverId, rejectReason, null);
                result.getSuccessfulIds().add(approvalId);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailedReasons().put(approvalId, e.getMessage());
                log.error("批量审批拒绝失败: id={}, error={}", approvalId, e.getMessage(), e);
            }
        }

        log.info("批量审批拒绝完成: total={}, success={}, failure={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    @Override
    public BatchApprovalResult batchWithdraw(List<Long> approvalIds, Long operatorId, String reason) {
        log.info("批量撤回申请: count={}, operator={}", approvalIds.size(), operatorId);

        BatchApprovalResult result = new BatchApprovalResult();
        result.setTotalCount(approvalIds.size());
        result.setSuccessfulIds(new ArrayList<>());
        result.setFailedReasons(new HashMap<>());

        for (Long approvalId : approvalIds) {
            try {
                withdrawApprovalRequest(approvalId, operatorId, reason);
                result.getSuccessfulIds().add(approvalId);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailureCount(result.getFailureCount() + 1);
                result.getFailedReasons().put(approvalId, e.getMessage());
                log.error("批量撤回失败: id={}, error={}", approvalId, e.getMessage(), e);
            }
        }

        log.info("批量撤回完成: total={}, success={}, failure={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    // ========== 工作流管理 ==========

    @Override
    public PriceSpecialApproval escalateApproval(Long approvalId, Long operatorId, String reason) {
        log.info("升级审批级别: id={}, operator={}", approvalId, operatorId);

        PriceSpecialApproval approval = getApprovalById(approvalId);

        // 验证是否可以升级
        if (approval.getCurrentApprovalLevel() >= approval.getTotalApprovalLevels()) {
            throw new IllegalStateException("已达到最高审批级别，无法升级");
        }

        // 提升审批级别
        approval.setCurrentApprovalLevel(approval.getCurrentApprovalLevel() + 1);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(operatorId);

        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);

        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.ESCALATE,
                "升级审批级别: " + reason, operatorId, getOperatorName(operatorId),
                PriceApprovalRecord.OperatorRole.SYSTEM_ADMIN, null);

        log.info("审批级别升级成功: id={}, level={}", approvalId, approval.getCurrentApprovalLevel());
        return updatedApproval;
    }

    @Override
    public PriceSpecialApproval delegateApproval(Long approvalId, Long approverId, Long delegateId, String reason) {
        log.info("委托审批: id={}, from={}, to={}", approvalId, approverId, delegateId);

        PriceSpecialApproval approval = getApprovalById(approvalId);

        // 验证当前审批人
        if (!approverId.equals(approval.getCurrentApproverId())) {
            throw new IllegalStateException("无权委托审批");
        }

        // 根据当前级别委托
        switch (approval.getCurrentApprovalLevel()) {
            case 1:
                approval.setLevel1ApproverId(delegateId);
                break;
            case 2:
                approval.setLevel2ApproverId(delegateId);
                break;
            case 3:
                approval.setLevel3ApproverId(delegateId);
                break;
            case 4:
                approval.setFinalApproverId(delegateId);
                break;
            default:
                throw new IllegalStateException("无效的审批级别");
        }

        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);

        PriceSpecialApproval updatedApproval = approvalRepository.save(approval);

        createApprovalRecord(updatedApproval, PriceApprovalRecord.ActionType.DELEGATE,
                "委托审批: " + reason, approverId, getOperatorName(approverId),
                getApproverRole(approverId, approval), null);

        log.info("审批委托成功: id={}, delegate={}", approvalId, delegateId);
        return updatedApproval;
    }

    @Override
    public WorkflowStatusDTO checkWorkflowStatus(Long approvalId) {
        PriceSpecialApproval approval = getApprovalById(approvalId);

        WorkflowStatusDTO status = new WorkflowStatusDTO();
        status.setCurrentStatus(approval.getStatus());
        status.setCurrentLevel(approval.getCurrentApprovalLevel());
        status.setTotalLevels(approval.getTotalApprovalLevels());
        status.setCurrentApproverId(approval.getCurrentApproverId());
        status.setCurrentApproverName(approval.getCurrentApproverName());
        status.setSubmittedAt(approval.getSubmittedAt());
        status.setLastUpdatedAt(approval.getUpdateTime());
        status.setExpectedCompletionTime(approval.getExpectedCompletionTime());
        status.setTimeout(approval.isApprovalTimeout());

        if (approval.getSubmittedAt() != null) {
            status.setDurationMinutes(java.time.Duration.between(approval.getSubmittedAt(), LocalDateTime.now()).toMinutes());
        } else {
            status.setDurationMinutes(0L);
        }

        // 确定下一步操作
        List<String> nextActions = new ArrayList<>();
        if (approval.getStatus().isInProgress()) {
            nextActions.add("APPROVE");
            nextActions.add("REJECT");
            if (approval.getCurrentApprovalLevel() < approval.getTotalApprovalLevels()) {
                nextActions.add("ESCALATE");
            }
        }
        status.setNextActions(nextActions);

        return status;
    }

    @Override
    public WorkflowValidationResult validateWorkflow(Long approvalId) {
        PriceSpecialApproval approval = getApprovalById(approvalId);

        WorkflowValidationResult result = new WorkflowValidationResult();
        result.setValid(true);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        result.setSuggestions(new ArrayList<>());

        // 验证审批人配置
        if (approval.getTotalApprovalLevels() >= 1 && approval.getLevel1ApproverId() == null) {
            result.getErrors().add("一级审批人未配置");
            result.setValid(false);
        }
        if (approval.getTotalApprovalLevels() >= 2 && approval.getLevel2ApproverId() == null) {
            result.getErrors().add("二级审批人未配置");
            result.setValid(false);
        }
        if (approval.getTotalApprovalLevels() >= 3 && approval.getLevel3ApproverId() == null) {
            result.getErrors().add("三级审批人未配置");
            result.setValid(false);
        }
        if (approval.getTotalApprovalLevels() >= 4 && approval.getFinalApproverId() == null) {
            result.getErrors().add("最终审批人未配置");
            result.setValid(false);
        }

        // 验证有效期
        if (approval.getValidFrom() != null && approval.getValidTo() != null
                && approval.getValidTo().isBefore(approval.getValidFrom())) {
            result.getWarnings().add("价格有效期结束时间早于开始时间");
        }

        // 建议
        if (approval.getTotalApprovalLevels() > 3) {
            result.getSuggestions().add("审批级别较多，建议简化审批流程");
        }
        if (approval.getStandardPrice() != null && approval.getAppliedPrice() != null
                && approval.getAppliedPrice().compareTo(approval.getStandardPrice()) > 0) {
            result.getSuggestions().add("申请价格高于标准价格，请确认合理性");
        }

        return result;
    }

    // ========== 统计方法 ==========

    @Override
    public ApprovalEfficiencyDTO getApprovalEfficiency(LocalDateTime startTime, LocalDateTime endTime) {
        ApprovalEfficiencyDTO efficiency = new ApprovalEfficiencyDTO();

        Specification<PriceSpecialApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("submittedAt"), startTime, endTime));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<PriceSpecialApproval> approvals = approvalRepository.findAll(spec);
        List<PriceSpecialApproval> completed = approvals.stream()
                .filter(a -> a.getStatus().isCompleted() && a.getSubmittedAt() != null)
                .collect(Collectors.toList());

        efficiency.setTotalApprovals((long) completed.size());

        if (!completed.isEmpty()) {
            long totalMinutes = completed.stream()
                    .mapToLong(PriceSpecialApproval::getApprovalDurationMinutes)
                    .sum();
            efficiency.setAverageApprovalTime(BigDecimal.valueOf(totalMinutes)
                    .divide(BigDecimal.valueOf(completed.size()), 2, java.math.RoundingMode.HALF_UP));
            efficiency.setMinApprovalTime(completed.stream()
                    .mapToLong(PriceSpecialApproval::getApprovalDurationMinutes)
                    .min().orElse(0));
            efficiency.setMaxApprovalTime(completed.stream()
                    .mapToLong(PriceSpecialApproval::getApprovalDurationMinutes)
                    .max().orElse(0));
        } else {
            efficiency.setAverageApprovalTime(BigDecimal.ZERO);
            efficiency.setMinApprovalTime(0L);
            efficiency.setMaxApprovalTime(0L);
        }

        long overdueCount = approvals.stream()
                .filter(PriceSpecialApproval::isApprovalTimeout)
                .count();
        efficiency.setOverdueCount(overdueCount);
        efficiency.setOverdueRate(approvals.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(overdueCount)
                        .divide(BigDecimal.valueOf(approvals.size()), 4, java.math.RoundingMode.HALF_UP));

        return efficiency;
    }

    @Override
    public ApproverStatisticsDTO getApproverStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        ApproverStatisticsDTO statistics = new ApproverStatisticsDTO();

        List<PriceApprovalRecord> records = recordRepository.findAll((Specification<PriceApprovalRecord>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("actionType"), PriceApprovalRecord.ActionType.APPROVE));
            predicates.add(cb.between(root.get("createTime"), startTime, endTime));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        Map<String, Long> countByApprover = records.stream()
                .collect(Collectors.groupingBy(PriceApprovalRecord::getOperatorName, Collectors.counting()));

        statistics.setTotalApprovers((long) countByApprover.size());
        statistics.setActiveApprovers(countByApprover.size() > 0 ? (long) countByApprover.size() : 0L);
        statistics.setApprovalCountByApprover(countByApprover);

        return statistics;
    }

    // ========== 辅助方法 ==========

    /**
     * 创建审批记录
     */
    private PriceApprovalRecord createApprovalRecord(PriceSpecialApproval approval, String actionType,
                                                      String description, Long operatorId, String operatorName,
                                                      String operatorRole, String decision) {
        PriceApprovalRecord record = PriceApprovalRecord.builder()
                .recordCode(generateRecordCode())
                .approvalId(approval.getId())
                .actionType(actionType)
                .actionDescription(description)
                .approvalLevel(approval.getCurrentApprovalLevel())
                .previousStatus(approval.getStatus() != null ? approval.getStatus().name() : null)
                .currentStatus(approval.getStatus() != null ? approval.getStatus().name() : null)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operatorRole(operatorRole)
                .decision(decision)
                .isSuccess(true)
                .tenantId(approval.getTenantId())
                .deleted(0)
                .createTime(LocalDateTime.now())
                .createBy(operatorId)
                .updateTime(LocalDateTime.now())
                .updateBy(operatorId)
                .build();

        return recordRepository.save(record);
    }

    /**
     * 生成申请编号
     */
    private String generateApprovalCode() {
        return "PA-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 生成记录编号
     */
    private String generateRecordCode() {
        return "PR-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 获取操作人姓名
     */
    private String getOperatorName(Long operatorId) {
        // TODO: 从用户服务获取用户名
        return String.valueOf(operatorId);
    }

    /**
     * 获取操作人角色
     */
    private String getOperatorRole(Long operatorId, PriceSpecialApproval approval) {
        if (operatorId.equals(approval.getApplicantId())) {
            return PriceApprovalRecord.OperatorRole.APPLICANT;
        }
        if (operatorId.equals(approval.getLevel1ApproverId()) ||
                operatorId.equals(approval.getLevel2ApproverId()) ||
                operatorId.equals(approval.getLevel3ApproverId()) ||
                operatorId.equals(approval.getFinalApproverId())) {
            return PriceApprovalRecord.OperatorRole.APPROVER;
        }
        return PriceApprovalRecord.OperatorRole.SYSTEM_ADMIN;
    }

    /**
     * 获取审批人角色
     */
    private String getApproverRole(Long approverId, PriceSpecialApproval approval) {
        if (approverId.equals(approval.getLevel1ApproverId())) {
            return PriceApprovalRecord.OperatorRole.APPROVER;
        }
        if (approverId.equals(approval.getLevel2ApproverId())) {
            return PriceApprovalRecord.OperatorRole.DEPARTMENT_MANAGER;
        }
        if (approverId.equals(approval.getLevel3ApproverId())) {
            return PriceApprovalRecord.OperatorRole.FINANCE_MANAGER;
        }
        if (approverId.equals(approval.getFinalApproverId())) {
            return PriceApprovalRecord.OperatorRole.GENERAL_MANAGER;
        }
        return PriceApprovalRecord.OperatorRole.APPROVER;
    }

    /**
     * 验证审批人配置
     */
    private void validateApprovers(PriceSpecialApproval approval) {
        if (approval.getTotalApprovalLevels() >= 1 && approval.getLevel1ApproverId() == null) {
            throw new IllegalStateException("一级审批人未配置");
        }
        if (approval.getTotalApprovalLevels() >= 2 && approval.getLevel2ApproverId() == null) {
            throw new IllegalStateException("二级审批人未配置");
        }
        if (approval.getTotalApprovalLevels() >= 3 && approval.getLevel3ApproverId() == null) {
            throw new IllegalStateException("三级审批人未配置");
        }
        if (approval.getTotalApprovalLevels() >= 4 && approval.getFinalApproverId() == null) {
            throw new IllegalStateException("最终审批人未配置");
        }
    }

    /**
     * 验证审批权限
     */
    private void validateApprovalPermission(PriceSpecialApproval approval, Long approverId) {
        if (!approverId.equals(approval.getCurrentApproverId())) {
            throw new IllegalStateException("当前用户无权进行此审批操作");
        }
        if (!approval.getStatus().isInProgress()) {
            throw new IllegalStateException("当前审批状态不允许审批操作");
        }
    }

    /**
     * 更新审批状态
     */
    private void updateApprovalStatus(PriceSpecialApproval approval, PriceApprovalStatus nextStatus, Long approverId) {
        approval.setStatus(nextStatus);
        approval.setCurrentApprovalLevel(approval.getCurrentApprovalLevel() != null
                ? approval.getCurrentApprovalLevel() + 1 : 1);
        approval.setUpdateTime(LocalDateTime.now());
        approval.setUpdateBy(approverId);
    }

    /**
     * 设置审批时间
     */
    private void setApprovalTime(PriceSpecialApproval approval, Integer level) {
        LocalDateTime now = LocalDateTime.now();
        if (level == null) return;
        switch (level) {
            case 1: approval.setLevel1ApprovedAt(now); break;
            case 2: approval.setLevel2ApprovedAt(now); break;
            case 3: approval.setLevel3ApprovedAt(now); break;
            case 4: approval.setFinalApprovedAt(now); break;
            default: break;
        }
    }

    /**
     * 最终审批通过后的处理
     */
    private void onFinalApproval(PriceSpecialApproval approval) {
        log.info("最终审批通过，执行后续操作: id={}", approval.getId());
        // TODO: 执行价格更新、通知等后续操作
        sendApprovalNotification(approval.getId(), "APPROVAL");
    }

    /**
     * 获取下一级审批人ID
     */
    private Long getNextApproverId(PriceSpecialApproval approval) {
        int nextLevel = approval.getCurrentApprovalLevel() + 1;
        switch (nextLevel) {
            case 2: return approval.getLevel2ApproverId();
            case 3: return approval.getLevel3ApproverId();
            case 4: return approval.getFinalApproverId();
            default: return null;
        }
    }

    /**
     * 发送通知（占位实现）
     */
    private void sendNotification(Long userId, String title, String content) {
        log.debug("发送通知: userId={}, title={}", userId, title);
        // TODO: 集成消息通知服务
    }

    /**
     * 构建搜索查询条件
     */
    private Specification<PriceSpecialApproval> buildSearchSpecification(PriceApprovalSearchDTO searchDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.getApprovalId() != null) {
                predicates.add(cb.equal(root.get("id"), searchDTO.getApprovalId()));
            }
            if (searchDTO.getApplicantId() != null) {
                predicates.add(cb.equal(root.get("applicantId"), searchDTO.getApplicantId()));
            }
            if (searchDTO.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), PriceApprovalStatus.valueOf(searchDTO.getStatus())));
            }
            if (searchDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), searchDTO.getStartTime()));
            }
            if (searchDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), searchDTO.getEndTime()));
            }

            predicates.add(cb.equal(root.get("deleted"), 0));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}