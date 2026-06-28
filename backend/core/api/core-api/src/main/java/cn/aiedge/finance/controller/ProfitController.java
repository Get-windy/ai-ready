package cn.aiedge.finance.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.finance.entity.ProfitAnalysis;
import cn.aiedge.finance.service.IProfitService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/erp/finance/profit")
@Tag(name = "毛利分析", description = "毛利分析相关接口")
@SaCheckLogin
@RequiredArgsConstructor
public class ProfitController {

    private final IProfitService profitService;

    @GetMapping("/page")
    @Operation(summary = "分页查询毛利数据")
    public ApiResponse<Page<ProfitAnalysis>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<ProfitAnalysis> page = profitService.pageProfit(pageNum, pageSize, productName, startDate, endDate);
        return ApiResponse.success(page);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取毛利统计数据")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(profitService.getStats());
    }
}
