package cn.aiedge.finance.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.finance.entity.ProductCostStandard;
import cn.aiedge.finance.service.ICostService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/erp/finance/cost")
@Tag(name = "成本核算", description = "产品标准成本及成本核算相关接口")
@SaCheckLogin
@RequiredArgsConstructor
public class CostController {

    private final ICostService costService;

    @GetMapping("/page")
    @Operation(summary = "分页查询成本数据")
    public ApiResponse<Page<ProductCostStandard>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String batchNo) {
        Page<ProductCostStandard> page = costService.pageCost(pageNum, pageSize, productName, batchNo);
        return ApiResponse.success(page);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取成本统计数据")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(costService.getStats());
    }
}
