package cn.aiedge.erp.sale.analytics.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.analytics.dto.PromotionFunnelQueryDTO;
import cn.aiedge.erp.sale.analytics.service.PromotionFunnelReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 推广分析控制器（分析 → 采销分析 → 销售分析 → 推广分析，菜单 80418）
 *
 * <p>职员维度的商城分享推广漏斗：分享次数 / 浏览次数 / 下单笔数 / 下单金额 / 新客注册。
 * 取数自营销域 {@code mkt_share_record}（推广分享触达台账），只读消费，不新增埋点。</p>
 *
 * <p>与既有 {@code /erp/sale/promotion/analysis}（促销活动效果分析，按活动计数）互补：
 * 后者无分享归因能力，本端点改为「按分享人归因」。</p>
 */
@Tag(name = "推广分析（职员分享漏斗）")
@RestController
@RequestMapping("/api/erp/sale/analysis/promotion-funnel")
@RequiredArgsConstructor
public class PromotionFunnelController {

    private final PromotionFunnelReportService promotionFunnelReportService;

    @Operation(summary = "职员推广漏斗分页",
            description = "按分享人归集：分享次数/浏览次数/下单笔数/下单金额（新客注册无归因链路，返回 null），含合计行 summary")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Map<String, Object>> page(PromotionFunnelQueryDTO query) {
        return ApiResponse.ok(promotionFunnelReportService.page(query));
    }
}
