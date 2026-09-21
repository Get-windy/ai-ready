package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.expense.dto.ApiResponse;
import cn.aiedge.erp.expense.model.ExpensePayment;
import cn.aiedge.erp.expense.model.enumeration.ExpenseStatus;
import cn.aiedge.erp.expense.model.ExpenseApplication;
import cn.aiedge.erp.expense.repository.ExpensePaymentRepository;
import cn.aiedge.erp.expense.repository.ExpenseApplicationRepository;
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
@RequestMapping("/api/erp/expense/payment")
@Tag(name = "费用支付管理", description = "费用支付记录、确认、取消")
@RequiredArgsConstructor
public class ExpensePaymentController {

    private final ExpensePaymentRepository paymentRepository;
    private final ExpenseApplicationRepository applicationRepository;

    @Operation(summary = "支付分页列表", description = "获取费用支付分页列表")
    @SaCheckPermission("erp:expense:payment:list")
    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @Parameter(description = "付款人ID") @RequestParam(required = false) String payerId,
            @Parameter(description = "支付状态") @RequestParam(required = false) String paymentStatus,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        Specification<ExpensePayment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (payerId != null && !payerId.isEmpty()) {
                predicates.add(cb.equal(root.get("payerId"), payerId));
            }
            if (paymentStatus != null && !paymentStatus.isEmpty()) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExpensePayment> pageResult = paymentRepository.findAll(spec, pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getContent());
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", pageResult.getTotalPages());
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取支付详情", description = "根据ID获取支付详细信息")
    @SaCheckPermission("erp:expense:payment:query")
    @GetMapping("/{id}")
    public ApiResponse<ExpensePayment> getById(
            @Parameter(description = "支付ID") @PathVariable Long id) {
        ExpensePayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("支付记录不存在: " + id));
        if (payment.isDeleted()) {
            throw BusinessException.badRequest("支付记录已被删除: " + id);
        }
        return ApiResponse.success(payment);
    }

    @Operation(summary = "创建支付记录", description = "为已审批通过的费用单创建支付记录")
    @SaCheckPermission("erp:expense:payment:create")
    @PostMapping
    public ApiResponse<ExpensePayment> create(@RequestBody ExpensePayment payment) {
        if (payment.getApplicationId() != null) {
            ExpenseApplication application = applicationRepository.findById(payment.getApplicationId())
                    .orElseThrow(() -> BusinessException.notFound("费用单不存在: " + payment.getApplicationId()));
            if (application.isDeleted()) {
                throw BusinessException.badRequest("费用单已被删除");
            }
            if (!application.canPay()) {
                throw BusinessException.badRequest("费用单状态不允许支付: " + application.getStatus().getDescription());
            }
            if (payment.getAmount() == null) {
                payment.setAmount(application.getTotalAmount());
            }
        }

        payment.setPaymentStatus("PENDING");
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        ExpensePayment saved = paymentRepository.save(payment);
        log.info("支付记录创建成功: {}", saved.getId());
        return ApiResponse.success("支付记录创建成功", saved);
    }

    @Operation(summary = "确认支付", description = "确认费用支付完成")
    @SaCheckPermission("erp:expense:payment:confirm")
    @PostMapping("/{id}/confirm")
    public ApiResponse<ExpensePayment> confirm(
            @Parameter(description = "支付ID") @PathVariable Long id) {
        ExpensePayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("支付记录不存在: " + id));
        if (payment.isDeleted()) {
            throw BusinessException.badRequest("支付记录已被删除: " + id);
        }
        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw BusinessException.badRequest("只有待确认的支付记录可以确认");
        }

        payment.setPaymentStatus("CONFIRMED");
        payment.setPaymentDate(LocalDate.now());
        ExpensePayment saved = paymentRepository.save(payment);

        // 更新关联的费用单状态为已支付
        if (payment.getApplicationId() != null) {
            applicationRepository.findById(payment.getApplicationId()).ifPresent(app -> {
                if (app.getStatus() == ExpenseStatus.APPROVED) {
                    app.setStatus(ExpenseStatus.PAID);
                    app.updateStatusDesc();
                    app.setPaymentDate(payment.getPaymentDate());
                    applicationRepository.save(app);
                }
            });
        }

        log.info("支付确认成功: {}", saved.getId());
        return ApiResponse.success("支付确认成功", saved);
    }

    @Operation(summary = "取消支付", description = "取消费用支付")
    @SaCheckPermission("erp:expense:payment:cancel")
    @PostMapping("/{id}/cancel")
    public ApiResponse<ExpensePayment> cancel(
            @Parameter(description = "支付ID") @PathVariable Long id) {
        ExpensePayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("支付记录不存在: " + id));
        if (payment.isDeleted()) {
            throw BusinessException.badRequest("支付记录已被删除: " + id);
        }
        if ("CONFIRMED".equals(payment.getPaymentStatus())) {
            throw BusinessException.badRequest("已确认的支付记录不能取消");
        }

        payment.setPaymentStatus("CANCELLED");
        ExpensePayment saved = paymentRepository.save(payment);
        log.info("支付取消成功: {}", saved.getId());
        return ApiResponse.success("支付取消成功", saved);
    }
}
