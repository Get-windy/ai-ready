package cn.aiedge.erp.pricing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.pricing.entity.PriceApproval;
import cn.aiedge.erp.pricing.mapper.PriceApprovalMapper;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 价格审批 Controller。
 *
 * <p><b>为什么要新建它</b>：前端 `views/erp/pricing/approval/index.vue` 与其 API 层
 * `api/pricing-approval.ts` 早已定义完整契约（8 个端点 + PriceApproval 实体），
 * 但后端零实现——该页所有请求 404。本类按**前端既有契约**补齐，不重新设计协议。</p>
 *
 * <p><b>租户隔离</b>：{@code erp_price_approval} 表名含下划线，会被
 * {@code AiReadyTenantLineInnerInterceptor} 自动注入 {@code tenant_id} 条件，
 * 业务代码无需手写租户过滤。</p>
 *
 * <p><b>权限</b>：前端该页未使用任何 {@code v-permission} 码，故此处不加
 * {@code @SaCheckPermission}，仅依赖全局登录校验（口径与页面对齐）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "价格审批")
@RestController
@RequestMapping("/api/erp/pricing/approval")
@RequiredArgsConstructor
public class PriceApprovalController {

    private final PriceApprovalMapper approvalMapper;
    private final ProductMapper productMapper;

    /** 申请入参：字段与前端 `api/pricing-approval.ts` 的 ApplyPriceChangeDTO 一一对应 */
    @Data
    public static class ApplyPriceChangeDTO {
        private Long productId;
        private Long customerId;
        private BigDecimal newPrice;
        private String approvalType;
        private String approvalReason;
        private LocalDate effectiveStart;
        private LocalDate effectiveEnd;
    }

    @Operation(summary = "审批统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalCount", approvalMapper.selectCount(null));
        m.put("pendingCount", countByStatus("pending"));
        m.put("approvedCount", countByStatus("approved"));
        m.put("rejectedCount", countByStatus("rejected"));
        return Result.ok(m);
    }

    @Operation(summary = "待审批列表")
    @GetMapping("/pending")
    public Result<List<PriceApproval>> pending() {
        return Result.ok(listByStatus("pending"));
    }

    @Operation(summary = "按状态查询审批列表")
    @GetMapping("/list/{status}")
    public Result<List<PriceApproval>> listByStatusApi(@PathVariable String status) {
        return Result.ok(listByStatus(status));
    }

    @Operation(summary = "我的申请")
    @GetMapping("/my/{applicantId}")
    public Result<List<PriceApproval>> my(@PathVariable Long applicantId) {
        return Result.ok(approvalMapper.selectList(
                new LambdaQueryWrapper<PriceApproval>()
                        .eq(PriceApproval::getApplicantId, applicantId)
                        .orderByDesc(PriceApproval::getApplyTime)));
    }

    /**
     * 审批详情。
     *
     * <p>⚠️ 路径必须约束为 {@code {id:\d+}}：否则 {@code /statistics}、{@code /pending}
     * 这类字面量路径会被本方法抢先匹配，再因无法转成 Long 而抛 400。
     * 项目内先例见 {@code CustomerController} 的 {@code /{id:\d+}/status}。</p>
     */
    @Operation(summary = "审批详情")
    @GetMapping("/{id:\\d+}")
    public Result<PriceApproval> detail(@PathVariable Long id) {
        PriceApproval a = approvalMapper.selectById(id);
        return a == null ? Result.fail("审批单不存在") : Result.ok(a);
    }

    @Operation(summary = "申请价格变更")
    @PostMapping("/apply")
    public Result<PriceApproval> apply(@RequestBody ApplyPriceChangeDTO dto) {
        if (dto.getProductId() == null) {
            return Result.fail("productId 不能为空");
        }
        if (dto.getNewPrice() == null) {
            return Result.fail("newPrice 不能为空");
        }

        // 原价快照：优先零售价，回退标准价；两者都为空则置 null（前端按空值展示为 "-"）
        Product p = productMapper.selectById(dto.getProductId());
        BigDecimal oldPrice = p == null ? null
                : (p.getRetailPrice() != null ? p.getRetailPrice() : p.getStandardPrice());

        BigDecimal change = (oldPrice == null || dto.getNewPrice() == null)
                ? null
                : dto.getNewPrice().subtract(oldPrice);

        PriceApproval a = new PriceApproval()
                .setProductId(dto.getProductId())
                .setProductName(p == null ? null : p.getProductName())
                .setProductCode(p == null ? null : p.getProductCode())
                .setCustomerId(dto.getCustomerId())
                .setOldPrice(oldPrice)
                .setNewPrice(dto.getNewPrice())
                .setPriceChange(change)
                .setPriceChangeType(change == null ? null : (change.signum() >= 0 ? "increase" : "decrease"))
                .setApprovalType(dto.getApprovalType())
                .setApprovalReason(dto.getApprovalReason())
                .setApplicantId(currentUserId())
                .setApplyTime(LocalDateTime.now())
                .setStatus("pending")
                .setEffectiveStart(dto.getEffectiveStart())
                .setEffectiveEnd(dto.getEffectiveEnd());

        approvalMapper.insert(a);
        return Result.ok(a);
    }

    @Operation(summary = "审批通过")
    @PutMapping("/{id:\\d+}/approve")
    public Result<PriceApproval> approve(@PathVariable Long id,
                                         @RequestParam(required = false) Long approverId,
                                         @RequestParam(required = false) String remark) {
        return doAudit(id, approverId, remark, "approved");
    }

    @Operation(summary = "审批拒绝")
    @PutMapping("/{id:\\d+}/reject")
    public Result<PriceApproval> reject(@PathVariable Long id,
                                        @RequestParam(required = false) Long approverId,
                                        @RequestParam(required = false) String remark) {
        return doAudit(id, approverId, remark, "rejected");
    }

    // ────────────────────────────── 内部方法 ──────────────────────────────

    private Long countByStatus(String status) {
        return approvalMapper.selectCount(
                new LambdaQueryWrapper<PriceApproval>().eq(PriceApproval::getStatus, status));
    }

    private List<PriceApproval> listByStatus(String status) {
        return approvalMapper.selectList(
                new LambdaQueryWrapper<PriceApproval>()
                        .eq(PriceApproval::getStatus, status)
                        .orderByDesc(PriceApproval::getApplyTime));
    }

    /**
     * 审批状态流转：pending → approved | rejected，单向不可逆。
     *
     * <p>注意：本方法只推进审批单状态，**不直接改写商品价格**。价格生效涉及
     * "改哪张价表"（基础价 {@code erp_product} / 客户价 {@code erp_customer_product_price} /
     * 等级价）的选择，属业务口径，需另行裁定后接线。前端该页当前也没有"审批后自动改价"的诉求。</p>
     */
    private Result<PriceApproval> doAudit(Long id, Long approverId, String remark, String target) {
        PriceApproval a = approvalMapper.selectById(id);
        if (a == null) {
            return Result.fail("审批单不存在");
        }
        if (!"pending".equals(a.getStatus())) {
            return Result.fail("该审批单已处理，当前状态：" + a.getStatus());
        }

        a.setStatus(target)
                .setApproverId(approverId != null ? approverId : currentUserId())
                .setApproveTime(LocalDateTime.now())
                .setApproveRemark(remark);
        approvalMapper.updateById(a);
        return Result.ok(a);
    }

    /** 当前登录用户 ID；取不到时返回 null（如定时任务等无会话场景） */
    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.debug("价格审批：无法获取当前登录用户，applicantId 置空");
            return null;
        }
    }
}
