package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import cn.aiedge.erp.marketing.service.LoyaltyCouponService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/api/erp/marketing/coupon")
@RequiredArgsConstructor
public class LoyaltyCouponController {

    private final LoyaltyCouponService loyaltyCouponService;

    @Operation(summary = "分页查询优惠券")
    @SaCheckPermission("marketing:coupon:list")
    @GetMapping("/page")
    public Result<IPage<LoyaltyCoupon>> page(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<LoyaltyCoupon> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoyaltyCoupon> wrapper = new LambdaQueryWrapper<>();
        if (partnerId != null) {
            wrapper.eq(LoyaltyCoupon::getPartnerId, partnerId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LoyaltyCoupon::getStatus, status);
        }
        wrapper.orderByDesc(LoyaltyCoupon::getCreateTime);
        return Result.ok(loyaltyCouponService.page(page, wrapper));
    }

    @Operation(summary = "查询会员可用优惠券")
    @SaCheckPermission("marketing:coupon:detail")
    @GetMapping("/available/{partnerId}")
    public Result<List<LoyaltyCoupon>> listAvailable(@PathVariable Long partnerId) {
        return Result.ok(loyaltyCouponService.listAvailable(partnerId));
    }

    @Operation(summary = "使用优惠券")
    @SaCheckPermission("marketing:coupon:create")
    @PostMapping("/{id}/use")
    public Result<Boolean> useCoupon(@PathVariable Long id) {
        loyaltyCouponService.useCoupon(id);
        return Result.ok(true);
    }

    @Operation(summary = "删除优惠券")
    @SaCheckPermission("marketing:coupon:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(loyaltyCouponService.removeById(id));
    }
}
