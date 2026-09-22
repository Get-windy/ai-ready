package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.expense.dto.ApiResponse;
import cn.aiedge.erp.expense.model.ExpenseReimbursement;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.repository.ExpenseReimbursementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/expense/reimbursement")
@Tag(name = "费用报销管理", description = "费用报销申请、提交、撤回等管理")
@RequiredArgsConstructor
public class ExpenseReimbursementController {

    private final ExpenseReimbursementRepository reimbursementRepository;

    @Operation(summary = "报销分页列表", description = "获取费用报销分页列表")
    @SaCheckPermission("erp:expense:reimbursement:list")
    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @Parameter(description = "申请人ID") @RequestParam(required = false) String applicantId,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) ExpenseStatus status,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Specification<ExpenseReimbursement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExpenseReimbursement> pageResult = reimbursementRepository.findAll(spec, pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", pageResult.getTotalPages());
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取报销详情", description = "根据ID获取报销详细信息")
    @SaCheckPermission("erp:expense:reimbursement:query")
    @GetMapping("/{id}")
    public ApiResponse<ExpenseReimbursement> getById(
            @Parameter(description = "报销ID") @PathVariable Long id) {
        ExpenseReimbursement reimbursement = reimbursementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("报销单不存在: " + id));
        if (reimbursement.isDeleted()) {
            throw BusinessException.badRequest("报销单已被删除: " + id);
        }
        return ApiResponse.success(reimbursement);
    }

    @Operation(summary = "创建报销单", description = "创建新的费用报销")
    @SaCheckPermission("erp:expense:reimbursement:create")
    @PostMapping
    public ApiResponse<ExpenseReimbursement> create(@RequestBody ExpenseReimbursement reimbursement) {
        reimbursement.setStatus(ExpenseStatus.DRAFT);
        if (reimbursement.getApplyDate() == null) {
            reimbursement.setApplyDate(LocalDate.now());
        }
        ExpenseReimbursement saved = reimbursementRepository.save(reimbursement);
        log.info("报销单创建成功: {}", saved.getId());
        return ApiResponse.success("报销单创建成功", saved);
    }

    @Operation(summary = "更新报销单", description = "更新费用报销信息")
    @SaCheckPermission("erp:expense:reimbursement:edit")
    @PutMapping("/{id}")
    public ApiResponse<ExpenseReimbursement> update(
            @Parameter(description = "报销ID") @PathVariable Long id,
            @RequestBody ExpenseReimbursement reimbursement) {
        ExpenseReimbursement existing = reimbursementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("报销单不存在: " + id));
        if (existing.isDeleted()) {
            throw BusinessException.badRequest("报销单已被删除: " + id);
        }

        if (reimbursement.getAmount() != null) existing.setAmount(reimbursement.getAmount());
        if (reimbursement.getReimbursementType() != null) existing.setReimbursementType(reimbursement.getReimbursementType());
        if (reimbursement.getBankAccount() != null) existing.setBankAccount(reimbursement.getBankAccount());
        if (reimbursement.getBankName() != null) existing.setBankName(reimbursement.getBankName());
        if (reimbursement.getApplicantId() != null) existing.setApplicantId(reimbursement.getApplicantId());
        if (reimbursement.getApplicantName() != null) existing.setApplicantName(reimbursement.getApplicantName());
        if (reimbursement.getDepartmentId() != null) existing.setDepartmentId(reimbursement.getDepartmentId());
        if (reimbursement.getDepartmentName() != null) existing.setDepartmentName(reimbursement.getDepartmentName());
        if (reimbursement.getApplyDate() != null) existing.setApplyDate(reimbursement.getApplyDate());
        if (reimbursement.getVoucherNo() != null) existing.setVoucherNo(reimbursement.getVoucherNo());
        if (reimbursement.getPaymentMethod() != null) existing.setPaymentMethod(reimbursement.getPaymentMethod());
        if (reimbursement.getRemark() != null) existing.setRemark(reimbursement.getRemark());

        ExpenseReimbursement saved = reimbursementRepository.save(existing);
        log.info("报销单更新成功: {}", saved.getId());
        return ApiResponse.success("报销单更新成功", saved);
    }

    @Operation(summary = "删除报销单", description = "删除指定的费用报销")
    @SaCheckPermission("erp:expense:reimbursement:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "报销ID") @PathVariable Long id) {
        ExpenseReimbursement reimbursement = reimbursementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("报销单不存在: " + id));
        reimbursement.markAsDeleted();
        reimbursementRepository.save(reimbursement);
        log.info("报销单删除成功: {}", id);
        return ApiResponse.success("报销单删除成功", null);
    }

    @Operation(summary = "提交报销审批", description = "提交报销单进入审批流程")
    @SaCheckPermission("erp:expense:reimbursement:submit")
    @PostMapping("/{id}/submit")
    public ApiResponse<ExpenseReimbursement> submit(
            @Parameter(description = "报销ID") @PathVariable Long id) {
        ExpenseReimbursement reimbursement = reimbursementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("报销单不存在: " + id));
        if (reimbursement.isDeleted()) {
            throw BusinessException.badRequest("报销单已被删除: " + id);
        }
        if (reimbursement.getStatus() != ExpenseStatus.DRAFT) {
            throw BusinessException.badRequest("只有草稿状态的报销单可以提交");
        }
        if (reimbursement.getAmount() == null || reimbursement.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("报销金额必须大于0");
        }
        reimbursement.setStatus(ExpenseStatus.SUBMITTED);
        ExpenseReimbursement saved = reimbursementRepository.save(reimbursement);
        log.info("报销单提交审批成功: {}", saved.getId());
        return ApiResponse.success("报销单已提交审批", saved);
    }

    @Operation(summary = "撤回报销申请", description = "撤回已提交的报销申请")
    @SaCheckPermission("erp:expense:reimbursement:create")
    @PostMapping("/{id}/withdraw")
    public ApiResponse<ExpenseReimbursement> withdraw(
            @Parameter(description = "报销ID") @PathVariable Long id) {
        ExpenseReimbursement reimbursement = reimbursementRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("报销单不存在: " + id));
        if (reimbursement.isDeleted()) {
            throw BusinessException.badRequest("报销单已被删除: " + id);
        }
        if (reimbursement.getStatus() != ExpenseStatus.SUBMITTED) {
            throw BusinessException.badRequest("只有已提交状态的报销单可以撤回");
        }
        reimbursement.setStatus(ExpenseStatus.CANCELLED);
        ExpenseReimbursement saved = reimbursementRepository.save(reimbursement);
        log.info("报销单撤回成功: {}", saved.getId());
        return ApiResponse.success("报销单已撤回", saved);
    }
}
