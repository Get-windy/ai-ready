package cn.aiedge.erp.sale.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.dto.CustomerActiveAnalysisDTO;
import cn.aiedge.erp.sale.dto.PromotionAnalysisDTO;
import cn.aiedge.erp.sale.service.SaleAnalysisService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 销售分析控制器
 */
@Tag(name = "销售分析")
@RestController
@RequestMapping("/api/erp/sale")
@RequiredArgsConstructor
public class SaleAnalysisController {

    private final SaleAnalysisService saleAnalysisService;

    @Operation(summary = "客户活跃分析分页",
            description = "每客户一行: 近N天订单数/金额、最近下单时间、历史订单总数/总额、跟进次数、活跃度分层(活跃/一般/沉默)")
    @SaCheckPermission("sale:analysis:list")
    @GetMapping("/analysis/customer-active/page")
    @SaCheckLogin
    public ApiResponse<Page<CustomerActiveAnalysisDTO>> customerActivePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(defaultValue = "30") Integer activeDays,
            @RequestParam(defaultValue = "90") Integer silentDays,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(saleAnalysisService.pageCustomerActive(page, size, days, activeDays, silentDays, keyword));
    }

    @Operation(summary = "促销效果分析",
            description = "促销活动按状态/类型计数+时间分布+区间活动列表+区间订单优惠概况(订单无promotion_id, 无法归因到具体活动)")
    @SaCheckPermission("sale:promotion:view")
    @GetMapping("/promotion/analysis")
    @SaCheckLogin
    public ApiResponse<PromotionAnalysisDTO> promotionAnalysis(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(saleAnalysisService.promotionAnalysis(startDate, endDate));
    }
}
