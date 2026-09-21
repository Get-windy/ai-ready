package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.CouponRecordRow;
import cn.aiedge.erp.marketing.entity.CouponCustomer;
import cn.aiedge.erp.marketing.entity.CouponTemplate;
import cn.aiedge.erp.marketing.service.CouponTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 优惠券（营销 → 营销活动 → 优惠券，菜单 80311）
 * 双视图：`优惠券设置`（制券，15 列）/ `领用明细`（领用核销流水，13 列）
 */
@Slf4j
@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/api/erp/marketing/coupon-template")
@RequiredArgsConstructor
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;
    private final cn.aiedge.erp.marketing.promotion.CouponRedemptionService couponRedemptionService;

    // ══════ Tab1 优惠券设置 ══════

    @Operation(summary = "分页查询优惠券（券模板）")
    @SaCheckPermission("marketing:coupon-template:list")
    @GetMapping("/page")
    public Result<IPage<CouponTemplate>> page(
            @RequestParam(required = false) String couponName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerScope,
            @RequestParam(required = false) Integer openReceive,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(couponTemplateService.pageWithCounts(couponName, status, customerScope, openReceive, pageNum, pageSize));
    }

    @Operation(summary = "优惠券详情")
    @SaCheckPermission("marketing:coupon-template:detail")
    @GetMapping("/{id}")
    public Result<CouponTemplate> getById(@PathVariable Long id) {
        CouponTemplate tpl = couponTemplateService.getById(id);
        if (tpl == null) return Result.fail("优惠券不存在");
        tpl.setRemainingCount(intOf(tpl.getTotalCount()) - intOf(tpl.getReceivedCount()) - intOf(tpl.getUsedCount()));
        return Result.ok(tpl);
    }

    @Operation(summary = "新增优惠券")
    @SaCheckPermission("marketing:coupon-template:create")
    @PostMapping
    public Result<CouponTemplate> create(@RequestBody CouponTemplate tpl) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        tpl.setId(null);
        tpl.setTenantId(tenantId == null ? 1L : tenantId);
        applyDefaults(tpl);
        tpl.setCreateTime(LocalDateTime.now());
        tpl.setUpdateTime(LocalDateTime.now());
        couponTemplateService.save(tpl);
        tpl.setRemainingCount(intOf(tpl.getTotalCount()));
        return Result.ok(tpl);
    }

    @Operation(summary = "修改优惠券")
    @SaCheckPermission("marketing:coupon-template:update")
    @PutMapping("/{id}")
    public Result<CouponTemplate> update(@PathVariable Long id, @RequestBody CouponTemplate tpl) {
        tpl.setId(id);
        tpl.setTenantId(null);
        tpl.setUpdateTime(LocalDateTime.now());
        couponTemplateService.updateById(tpl);
        return Result.ok(couponTemplateService.getById(id));
    }

    @Operation(summary = "删除优惠券")
    @SaCheckPermission("marketing:coupon-template:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(couponTemplateService.removeById(id));
    }

    @Operation(summary = "作废优惠券（同步作废其未使用的券）")
    @SaCheckPermission("marketing:coupon-template:create")
    @PostMapping("/{id}/void")
    public Result<Boolean> voidTemplate(@PathVariable Long id) {
        couponTemplateService.voidTemplate(id);
        return Result.ok(true);
    }

    @Operation(summary = "优惠券数量统计（总数/已领取（未使用）/已使用/未领取）")
    @SaCheckPermission("marketing:coupon-template:view")
    @GetMapping("/{id}/stat")
    public Result<Map<String, Object>> stat(@PathVariable Long id) {
        return Result.ok(couponTemplateService.stat(id));
    }

    @Operation(summary = "查询券模板指定客户")
    @SaCheckPermission("marketing:coupon-template:view")
    @GetMapping("/{id}/customers")
    public Result<List<CouponCustomer>> customers(@PathVariable Long id) {
        return Result.ok(couponTemplateService.listCustomers(id));
    }

    @Operation(summary = "保存券模板指定客户")
    @SaCheckPermission("marketing:coupon-template:update")
    @PutMapping("/{id}/customers")
    public Result<Boolean> saveCustomers(@PathVariable Long id, @RequestBody List<CouponCustomer> customers) {
        couponTemplateService.saveCustomers(id, customers);
        return Result.ok(true);
    }

    @Operation(summary = "发优惠券（对指定客户发放 N 张）")
    @SaCheckPermission("marketing:coupon-template:create")
    @PostMapping("/{id}/issue")
    public Result<Integer> issue(@PathVariable Long id, @RequestBody IssueRequest req) {
        int n = couponTemplateService.issue(id, req.getPartnerIds(),
                req.getQuantityPerPartner(), req.getSourceBillNo());
        return Result.ok(n);
    }

    // ══════ Tab2 领用明细 ══════

    @Operation(summary = "分页查询优惠券领用明细")
    @SaCheckPermission("marketing:coupon-template:list")
    @GetMapping("/record/page")
    public Result<IPage<CouponRecordRow>> recordPage(
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String billNo,
            @RequestParam(required = false) String couponName,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(couponTemplateService.pageRecords(templateId, status, billNo, couponName, partnerId, pageNum, pageSize));
    }

    @Operation(summary = "核销优惠券（后台补录：把券置为已使用并记录单据号）")
    @SaCheckPermission("marketing:coupon-template:create")
    @PostMapping("/record/{couponId}/redeem")
    public Result<Boolean> redeem(@PathVariable Long couponId,
                                  @RequestParam(required = false) Long orderId,
                                  @RequestParam(required = false) String orderNo) {
        couponRedemptionService.redeem(couponId, orderId, orderNo == null || orderNo.isBlank() ? "后台补录" : orderNo);
        return Result.ok(true);
    }

    @Operation(summary = "作废单张券（领用明细行级）")
    @SaCheckPermission("marketing:coupon-template:create")
    @PostMapping("/record/{couponId}/void")
    public Result<Boolean> voidCoupon(@PathVariable Long couponId) {
        couponTemplateService.voidCoupon(couponId);
        return Result.ok(true);
    }

    // ── 内部 ──

    @Data
    public static class IssueRequest {
        private List<Long> partnerIds;
        private Integer quantityPerPartner;
        private String sourceBillNo;
    }

    private static int intOf(Integer v) {
        return v == null ? 0 : v;
    }

    /** 默认值：类型现金券 / 规则无限制 / 全部客户 / 状态正常 / 双通道允许 */
    private void applyDefaults(CouponTemplate tpl) {
        if (tpl.getCouponType() == null) tpl.setCouponType("CASH");
        if (tpl.getUseRule() == null) tpl.setUseRule("UNLIMITED");
        if (tpl.getCustomerScope() == null) tpl.setCustomerScope("ALL");
        if (tpl.getStatus() == null) tpl.setStatus("NORMAL");
        if (tpl.getOpenReceive() == null) tpl.setOpenReceive(0);
        if (tpl.getReceivedCount() == null) tpl.setReceivedCount(0);
        if (tpl.getUsedCount() == null) tpl.setUsedCount(0);
        if (tpl.getFaceValue() == null) tpl.setFaceValue(BigDecimal.ZERO);
        if (tpl.getTotalCount() == null) tpl.setTotalCount(0);
        if (tpl.getMallEnabled() == null) tpl.setMallEnabled(1);
        if (tpl.getOfflineEnabled() == null) tpl.setOfflineEnabled(0);
    }
}
