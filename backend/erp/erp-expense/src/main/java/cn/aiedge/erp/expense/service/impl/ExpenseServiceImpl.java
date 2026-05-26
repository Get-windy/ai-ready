package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.erp.expense.dto.ExpenseApplicationDTO;
import cn.aiedge.erp.expense.dto.ExpenseRequest;
import cn.aiedge.erp.expense.model.ExpenseApplication;
import cn.aiedge.erp.expense.model.ExpenseApproval;
import cn.aiedge.erp.expense.model.ExpenseItem;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import cn.aiedge.erp.expense.repository.ExpenseApplicationRepository;
import cn.aiedge.erp.expense.repository.ExpenseApprovalRepository;
import cn.aiedge.erp.expense.repository.ExpenseItemRepository;
import cn.aiedge.erp.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseApplicationRepository expenseApplicationRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final ExpenseApprovalRepository expenseApprovalRepository;

    @Override
    @Transactional
    public ExpenseApplicationDTO applyExpense(ExpenseRequest request) {
        ExpenseApplication application = new ExpenseApplication();
        application.setApplicationCode(generateApplicationCode());
        application.setApplicantId(request.getApplicantId());
        application.setApplicantName(request.getApplicantName());
        application.setDepartmentId(request.getDepartmentId());
        application.setDepartmentName(request.getDepartmentName());
        application.setApplyDate(request.getApplyDate());
        application.setExpenseType(request.getExpenseType());
        application.setTotalAmount(request.getTotalAmount());
        application.setCurrency(request.getCurrency());
        application.setBudgetSubjectId(request.getBudgetSubjectId());
        application.setBudgetSubjectName(request.getBudgetSubjectName());
        application.setBudgetAmount(request.getBudgetAmount());
        application.setPurpose(request.getPurpose());
        application.setDescription(request.getDescription());
        application.setPaymentMethod(request.getPaymentMethod());
        application.setPaymentAccount(request.getPaymentAccount());
        application.setIsUrgent(request.getIsUrgent());
        application.setUrgentReason(request.getUrgentReason());
        application.setExpectedCompletionDate(request.getExpectedCompletionDate());
        application.setStatus(ExpenseStatus.DRAFT);
        application.updateStatusDesc();
        application.checkBudgetExceed();
        
        ExpenseApplication saved = expenseApplicationRepository.save(application);
        
        if (request.getExpenseItems() != null && !request.getExpenseItems().isEmpty()) {
            List<ExpenseItem> items = createExpenseItems(saved, request.getExpenseItems());
            expenseItemRepository.saveAll(items);
            saved.setExpenseItems(items);
            saved.calculateTotalAmount();
            expenseApplicationRepository.save(saved);
        }
        
        log.info("费用单申请成功: {}", saved.getApplicationCode());
        return convertToDTO(saved);
    }

    @Override
    public ExpenseApplicationDTO getExpenseDetail(Long id) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (application.isDeleted()) {
            throw new RuntimeException("费用单已被删除: " + id);
        }
        
        List<ExpenseItem> items = expenseItemRepository.findByExpenseApplicationId(id);
        application.setExpenseItems(items);
        
        return convertToDTO(application);
    }

    @Override
    @Transactional
    public ExpenseApplicationDTO updateExpense(Long id, ExpenseRequest request) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (application.isDeleted()) {
            throw new RuntimeException("费用单已被删除: " + id);
        }
        
        if (!application.canSubmit()) {
            throw new RuntimeException("费用单状态不允许修改: " + application.getStatus().getDescription());
        }
        
        application.setApplicantId(request.getApplicantId());
        application.setApplicantName(request.getApplicantName());
        application.setDepartmentId(request.getDepartmentId());
        application.setDepartmentName(request.getDepartmentName());
        application.setApplyDate(request.getApplyDate());
        application.setExpenseType(request.getExpenseType());
        application.setTotalAmount(request.getTotalAmount());
        application.setCurrency(request.getCurrency());
        application.setBudgetSubjectId(request.getBudgetSubjectId());
        application.setBudgetSubjectName(request.getBudgetSubjectName());
        application.setBudgetAmount(request.getBudgetAmount());
        application.setPurpose(request.getPurpose());
        application.setDescription(request.getDescription());
        application.setPaymentMethod(request.getPaymentMethod());
        application.setPaymentAccount(request.getPaymentAccount());
        application.setIsUrgent(request.getIsUrgent());
        application.setUrgentReason(request.getUrgentReason());
        application.setExpectedCompletionDate(request.getExpectedCompletionDate());
        application.checkBudgetExceed();
        
        expenseItemRepository.deleteByExpenseApplicationId(id);
        
        if (request.getExpenseItems() != null && !request.getExpenseItems().isEmpty()) {
            List<ExpenseItem> items = createExpenseItems(application, request.getExpenseItems());
            expenseItemRepository.saveAll(items);
            application.setExpenseItems(items);
            application.calculateTotalAmount();
        }
        
        ExpenseApplication saved = expenseApplicationRepository.save(application);
        log.info("费用单更新成功: {}", saved.getApplicationCode());
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (!application.canCancel()) {
            throw new RuntimeException("费用单状态不允许删除: " + application.getStatus().getDescription());
        }
        
        application.markAsDeleted();
        application.setCancelReason("用户主动删除");
        expenseApplicationRepository.save(application);
        log.info("费用单删除成功: {}", application.getApplicationCode());
    }

    @Override
    @Transactional
    public ExpenseApplicationDTO submitForApproval(Long id) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (application.isDeleted()) {
            throw new RuntimeException("费用单已被删除: " + id);
        }
        
        if (!application.canSubmit()) {
            throw new RuntimeException("费用单状态不允许提交审批: " + application.getStatus().getDescription());
        }
        
        if (application.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("费用单金额必须大于0");
        }
        
        int approvalLevel = determineApprovalLevel(application);
        application.setStatus(ExpenseStatus.SUBMITTED);
        application.updateStatusDesc();
        application.setCurrentApprovalLevel(1);
        application.setTotalApprovalLevel(approvalLevel);
        application.setCurrentApproverId(getApproverId(1, application.getDepartmentId()));
        application.setCurrentApproverName(getApproverName(1, application.getDepartmentId()));
        
        ExpenseApplication saved = expenseApplicationRepository.save(application);
        
        ExpenseApproval approval = new ExpenseApproval();
        approval.setApplicationId(saved.getApplicationCode());
        approval.setApprovalLevel(0);
        approval.setApproverId(saved.getApplicantId());
        approval.setApproverName(saved.getApplicantName());
        approval.setApproverDepartmentId(saved.getDepartmentId());
        approval.setApproverDepartmentName(saved.getDepartmentName());
        approval.setApprovalAction("SUBMIT");
        approval.setApprovalComment("提交审批");
        approval.setApprovalTime(LocalDateTime.now());
        approval.setPreviousStatus(ExpenseStatus.DRAFT.name());
        approval.setCurrentStatus(ExpenseStatus.SUBMITTED.name());
        expenseApprovalRepository.save(approval);
        
        log.info("费用单提交审批成功: {}", saved.getApplicationCode());
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ExpenseApplicationDTO approveExpense(Long id, String comment) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (application.isDeleted()) {
            throw new RuntimeException("费用单已被删除: " + id);
        }
        
        if (!application.canApprove()) {
            throw new RuntimeException("费用单状态不允许审批: " + application.getStatus().getDescription());
        }
        
        ExpenseStatus previousStatus = application.getStatus();
        int currentLevel = application.getCurrentApprovalLevel();
        int totalLevel = application.getTotalApprovalLevel();
        
        ExpenseApproval approval = new ExpenseApproval();
        approval.setApplicationId(application.getApplicationCode());
        approval.setApprovalLevel(currentLevel);
        approval.setApproverId(application.getCurrentApproverId());
        approval.setApproverName(application.getCurrentApproverName());
        approval.setApprovalAction("APPROVE");
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setPreviousStatus(previousStatus.name());
        expenseApprovalRepository.save(approval);
        
        if (currentLevel >= totalLevel) {
            application.setStatus(ExpenseStatus.APPROVED);
            application.updateStatusDesc();
            application.setCurrentApproverId(null);
            application.setCurrentApproverName(null);
            application.setApprovalComment(comment);
            approval.setCurrentStatus(ExpenseStatus.APPROVED.name());
        } else {
            int nextLevel = currentLevel + 1;
            application.setCurrentApprovalLevel(nextLevel);
            application.setCurrentApproverId(getApproverId(nextLevel, application.getDepartmentId()));
            application.setCurrentApproverName(getApproverName(nextLevel, application.getDepartmentId()));
            
            ExpenseStatus nextStatus = getApprovalStatusByLevel(nextLevel);
            application.setStatus(nextStatus);
            application.updateStatusDesc();
            approval.setCurrentStatus(nextStatus.name());
        }
        
        ExpenseApplication saved = expenseApplicationRepository.save(application);
        log.info("费用单审批通过: {}, 状态: {}", saved.getApplicationCode(), saved.getStatus().getDescription());
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ExpenseApplicationDTO rejectExpense(Long id, String reason) {
        ExpenseApplication application = expenseApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("费用单不存在: " + id));
        
        if (application.isDeleted()) {
            throw new RuntimeException("费用单已被删除: " + id);
        }
        
        if (!application.canApprove()) {
            throw new RuntimeException("费用单状态不允许审批: " + application.getStatus().getDescription());
        }
        
        ExpenseStatus previousStatus = application.getStatus();
        
        ExpenseApproval approval = new ExpenseApproval();
        approval.setApplicationId(application.getApplicationCode());
        approval.setApprovalLevel(application.getCurrentApprovalLevel());
        approval.setApproverId(application.getCurrentApproverId());
        approval.setApproverName(application.getCurrentApproverName());
        approval.setApprovalAction("REJECT");
        approval.setApprovalComment(reason);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setPreviousStatus(previousStatus.name());
        approval.setCurrentStatus(ExpenseStatus.REJECTED.name());
        expenseApprovalRepository.save(approval);
        
        application.setStatus(ExpenseStatus.REJECTED);
        application.updateStatusDesc();
        application.setRejectReason(reason);
        application.setCurrentApproverId(null);
        application.setCurrentApproverName(null);
        
        ExpenseApplication saved = expenseApplicationRepository.save(application);
        log.info("费用单审批拒绝: {}, 原因: {}", saved.getApplicationCode(), reason);
        return convertToDTO(saved);
    }

    @Override
    public List<ExpenseApplicationDTO> getExpenseList(String applicantId, String departmentId, 
                                                       ExpenseStatus status, LocalDate startDate, 
                                                       LocalDate endDate, int page, int size) {
        Specification<ExpenseApplication> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("deleted"), false));
            
            if (applicantId != null && !applicantId.isEmpty()) {
                predicates.add(cb.equal(root.get("applicantId"), applicantId));
            }
            
            if (departmentId != null && !departmentId.isEmpty()) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }
            
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("applyDate"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("applyDate"), endDate));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExpenseApplication> pageResult = expenseApplicationRepository.findAll(spec, pageRequest);
        
        return pageResult.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getExpenseStatistics(LocalDate startDate, LocalDate endDate, 
                                                    String departmentId, ExpenseType expenseType) {
        List<ExpenseStatus> approvedStatuses = Arrays.asList(
                ExpenseStatus.APPROVED, ExpenseStatus.PAID, ExpenseStatus.REIMBURSED
        );
        
        List<ExpenseStatus> pendingStatuses = Arrays.asList(
                ExpenseStatus.SUBMITTED, ExpenseStatus.DEPARTMENT_APPROVING,
                ExpenseStatus.FINANCE_APPROVING, ExpenseStatus.GENERAL_MANAGER_APPROVING
        );
        
        List<ExpenseApplication> allApplications = expenseApplicationRepository.findAll();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal approvedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal rejectedAmount = BigDecimal.ZERO;
        int expenseCount = 0;
        
        Map<String, BigDecimal> byDepartment = new HashMap<>();
        Map<String, BigDecimal> byType = new HashMap<>();
        
        for (ExpenseApplication app : allApplications) {
            if (app.isDeleted()) continue;
            
            if (startDate != null && app.getApplyDate().isBefore(startDate)) continue;
            if (endDate != null && app.getApplyDate().isAfter(endDate)) continue;
            if (departmentId != null && !departmentId.equals(app.getDepartmentId())) continue;
            if (expenseType != null && expenseType != app.getExpenseType()) continue;
            
            totalAmount = totalAmount.add(app.getTotalAmount());
            expenseCount++;
            
            if (approvedStatuses.contains(app.getStatus())) {
                approvedAmount = approvedAmount.add(app.getTotalAmount());
            } else if (pendingStatuses.contains(app.getStatus())) {
                pendingAmount = pendingAmount.add(app.getTotalAmount());
            } else if (app.getStatus() == ExpenseStatus.REJECTED) {
                rejectedAmount = rejectedAmount.add(app.getTotalAmount());
            }
            
            String deptName = app.getDepartmentName() != null ? app.getDepartmentName() : "未知部门";
            byDepartment.merge(deptName, app.getTotalAmount(), BigDecimal::add);
            
            String typeName = app.getExpenseType() != null ? app.getExpenseType().getDescription() : "其他";
            byType.merge(typeName, app.getTotalAmount(), BigDecimal::add);
        }
        
        BigDecimal averageAmount = expenseCount > 0 
                ? totalAmount.divide(BigDecimal.valueOf(expenseCount), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAmount", totalAmount);
        statistics.put("approvedAmount", approvedAmount);
        statistics.put("pendingAmount", pendingAmount);
        statistics.put("rejectedAmount", rejectedAmount);
        statistics.put("expenseCount", expenseCount);
        statistics.put("averageAmount", averageAmount);
        statistics.put("byDepartment", byDepartment);
        statistics.put("byType", byType);
        
        return statistics;
    }

    private String generateApplicationCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = expenseApplicationRepository.count();
        return "EXP-" + dateStr + String.format("%04d", count + 1);
    }

    private List<ExpenseItem> createExpenseItems(ExpenseApplication application, 
                                                  List<ExpenseRequest.ExpenseItemRequest> itemRequests) {
        List<ExpenseItem> items = new ArrayList<>();
        int sequence = 0;
        
        for (ExpenseRequest.ExpenseItemRequest request : itemRequests) {
            ExpenseItem item = new ExpenseItem();
            item.setExpenseApplication(application);
            item.setItemName(request.getItemName());
            item.setDescription(request.getDescription());
            item.setExpenseDate(request.getExpenseDate());
            item.setAmount(request.getAmount());
            item.setQuantity(request.getQuantity());
            item.setUnit(request.getUnit());
            item.setVendorName(request.getVendorName());
            item.setTaxRate(request.getTaxRate());
            item.setHasInvoice(request.getHasInvoice());
            item.setInvoiceNumber(request.getInvoiceNumber());
            item.setInvoiceDate(request.getInvoiceDate());
            item.setPaymentMethod(request.getPaymentMethod());
            item.setAccountCode(request.getAccountCode());
            item.setBudgetCode(request.getBudgetCode());
            item.setProjectCode(request.getProjectCode());
            item.setCostCenter(request.getCostCenter());
            item.setIsPersonal(request.getIsPersonal());
            item.setIsReimbursable(request.getIsReimbursable());
            item.setReceiptRequired(request.getReceiptRequired());
            item.setReceiptAttached(request.getReceiptAttached());
            item.setSequenceNumber(sequence++);
            items.add(item);
        }
        
        return items;
    }

    private int determineApprovalLevel(ExpenseApplication application) {
        BigDecimal amount = application.getTotalAmount();
        ExpenseType type = application.getExpenseType();
        
        int typeLevel = type.getDefaultApprovalLevel();
        
        int amountLevel;
        if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            amountLevel = 3;
        } else if (amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            amountLevel = 2;
        } else {
            amountLevel = 1;
        }
        
        return Math.max(typeLevel, amountLevel);
    }

    private ExpenseStatus getApprovalStatusByLevel(int level) {
        switch (level) {
            case 1:
                return ExpenseStatus.DEPARTMENT_APPROVING;
            case 2:
                return ExpenseStatus.FINANCE_APPROVING;
            case 3:
                return ExpenseStatus.GENERAL_MANAGER_APPROVING;
            default:
                return ExpenseStatus.DEPARTMENT_APPROVING;
        }
    }

    private String getApproverId(int level, String departmentId) {
        switch (level) {
            case 1:
                return "dept_manager_" + (departmentId != null ? departmentId : "default");
            case 2:
                return "finance_manager";
            case 3:
                return "general_manager";
            default:
                return "approver_default";
        }
    }

    private String getApproverName(int level, String departmentId) {
        switch (level) {
            case 1:
                return "部门经理";
            case 2:
                return "财务经理";
            case 3:
                return "总经理";
            default:
                return "审批人";
        }
    }

    private ExpenseApplicationDTO convertToDTO(ExpenseApplication application) {
        ExpenseApplicationDTO dto = new ExpenseApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationCode(application.getApplicationCode());
        dto.setApplicantId(application.getApplicantId());
        dto.setApplicantName(application.getApplicantName());
        dto.setDepartmentId(application.getDepartmentId());
        dto.setDepartmentName(application.getDepartmentName());
        dto.setApplyDate(application.getApplyDate());
        dto.setExpenseType(application.getExpenseType());
        dto.setExpenseTypeDesc(application.getExpenseTypeDesc());
        dto.setTotalAmount(application.getTotalAmount());
        dto.setCurrency(application.getCurrency());
        dto.setBudgetSubjectId(application.getBudgetSubjectId());
        dto.setBudgetSubjectName(application.getBudgetSubjectName());
        dto.setBudgetAmount(application.getBudgetAmount());
        dto.setUsedBudgetAmount(application.getUsedBudgetAmount());
        dto.setBudgetUsageRate(application.getBudgetUsageRate());
        dto.setPurpose(application.getPurpose());
        dto.setDescription(application.getDescription());
        dto.setStatus(application.getStatus());
        dto.setStatusDesc(application.getStatusDesc());
        dto.setCurrentApproverId(application.getCurrentApproverId());
        dto.setCurrentApproverName(application.getCurrentApproverName());
        dto.setCurrentApprovalLevel(application.getCurrentApprovalLevel());
        dto.setTotalApprovalLevel(application.getTotalApprovalLevel());
        dto.setProcessInstanceId(application.getProcessInstanceId());
        dto.setProcessDefinitionId(application.getProcessDefinitionId());
        dto.setTaskId(application.getTaskId());
        dto.setPaymentMethod(application.getPaymentMethod());
        dto.setPaymentAccount(application.getPaymentAccount());
        dto.setPaymentDate(application.getPaymentDate());
        dto.setPaymentVoucherNo(application.getPaymentVoucherNo());
        dto.setReimbursementDate(application.getReimbursementDate());
        dto.setReimbursementVoucherNo(application.getReimbursementVoucherNo());
        dto.setAttachmentCount(application.getAttachmentCount());
        dto.setIsUrgent(application.getIsUrgent());
        dto.setUrgentReason(application.getUrgentReason());
        dto.setExpectedCompletionDate(application.getExpectedCompletionDate());
        dto.setActualCompletionDate(application.getActualCompletionDate());
        dto.setExceedBudget(application.getExceedBudget());
        dto.setExceedAmount(application.getExceedAmount());
        dto.setExceedReason(application.getExceedReason());
        dto.setApprovalComment(application.getApprovalComment());
        dto.setRejectReason(application.getRejectReason());
        dto.setCancelReason(application.getCancelReason());
        dto.setRemark(application.getRemark());
        
        if (application.getExpenseItems() != null && !application.getExpenseItems().isEmpty()) {
            List<ExpenseApplicationDTO.ExpenseItemDTO> itemDTOs = application.getExpenseItems().stream()
                    .map(this::convertItemToDTO)
                    .collect(Collectors.toList());
            dto.setExpenseItems(itemDTOs);
        }
        
        return dto;
    }

    private ExpenseApplicationDTO.ExpenseItemDTO convertItemToDTO(ExpenseItem item) {
        ExpenseApplicationDTO.ExpenseItemDTO dto = new ExpenseApplicationDTO.ExpenseItemDTO();
        dto.setId(item.getId());
        dto.setItemName(item.getItemName());
        dto.setDescription(item.getDescription());
        dto.setExpenseDate(item.getExpenseDate());
        dto.setAmount(item.getAmount());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setUnit(item.getUnit());
        dto.setVendorName(item.getVendorName());
        dto.setTaxRate(item.getTaxRate());
        dto.setTaxAmount(item.getTaxAmount());
        dto.setTotalAmountWithTax(item.getTotalAmountWithTax());
        dto.setHasInvoice(item.getHasInvoice());
        dto.setInvoiceNumber(item.getInvoiceNumber());
        dto.setInvoiceDate(item.getInvoiceDate());
        dto.setPaymentMethod(item.getPaymentMethod());
        dto.setAccountCode(item.getAccountCode());
        dto.setBudgetCode(item.getBudgetCode());
        dto.setProjectCode(item.getProjectCode());
        dto.setCostCenter(item.getCostCenter());
        dto.setIsPersonal(item.getIsPersonal());
        dto.setIsReimbursable(item.getIsReimbursable());
        dto.setReceiptRequired(item.getReceiptRequired());
        dto.setReceiptAttached(item.getReceiptAttached());
        dto.setAttachmentId(item.getAttachmentId());
        dto.setIsVerified(item.getIsVerified());
        dto.setVerifiedBy(item.getVerifiedBy());
        dto.setVerifiedDate(item.getVerifiedDate());
        dto.setVerificationComment(item.getVerificationComment());
        dto.setSequenceNumber(item.getSequenceNumber());
        return dto;
    }
}