package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.promotion.PromotionEngine;
import cn.aiedge.erp.marketing.promotion.PromotionRequest;
import cn.aiedge.erp.marketing.promotion.PromotionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 促销引擎试算（营销 → 营销活动 → 商品促销/整单促销/特价 的共用计算入口）。
 *
 * <p>开单侧（销售订单/零售单）保存时同样调用 {@link PromotionEngine}，且**以服务端结果为准**；
 * 本端点只做"开单前预览"，不产生任何写操作（券不核销、次数不累加）。</p>
 */
@Slf4j
@Tag(name = "促销引擎试算")
@RestController
@RequestMapping("/api/erp/marketing/promotion")
@RequiredArgsConstructor
public class PromotionEngineController {

    private final PromotionEngine promotionEngine;

    @Operation(summary = "试算订单可享受的促销与优惠券优惠（只读，不核销不累加次数）")
    @SaCheckPermission("marketing:promotion:create")
    @PostMapping("/calc")
    public Result<PromotionResult> calc(@RequestBody PromotionRequest req) {
        if (req.getTenantId() == null) {
            Long tid = SecurityUtils.getCurrentTenantId();
            req.setTenantId(tid == null ? 1L : tid);
        }
        if (req.getChannel() == null || req.getChannel().isBlank()) {
            req.setChannel("OFFLINE");
        }
        PromotionResult r = promotionEngine.preview(req);
        log.debug("[促销引擎] 试算 promo={} coupon={} 命中活动={}",
                r.getPromoDiscount(), r.getCouponDiscount(), r.getAppliedPromotionIds().size());
        return Result.ok(r);
    }
}
