package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeStatisticsQueryRequest;
import cn.aiedge.erp.expense.dto.FeeStatisticsVO;
import cn.aiedge.erp.expense.service.FeeStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 费用统计控制器
 */
@Tag(name = "费用统计管理", description = "费用统计台账接口")
@RestController
@RequestMapping("/api/erp/expense/statistics")
@RequiredArgsConstructor
public class FeeStatisticsController {

    private final FeeStatisticsService feeStatisticsService;

    @Operation(summary = "分页查询统计台账")
    @GetMapping("/page")
    @RequiresPermission("erp:expense:statistics:list")
    public ApiResponse<PageResult<FeeStatisticsVO>> pageList(FeeStatisticsQueryRequest request) {
        return ApiResponse.ok(feeStatisticsService.pageList(request));
    }

    @Operation(summary = "获取汇总统计数据")
    @GetMapping("/summary")
    @RequiresPermission("erp:expense:statistics:list")
    public ApiResponse<Map<String, Object>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok(feeStatisticsService.getSummaryData(year, month, departmentId));
    }

    @Operation(summary = "按部门统计")
    @GetMapping("/by-department")
    @RequiresPermission("erp:expense:statistics:list")
    public ApiResponse<List<FeeStatisticsVO>> getByDepartment(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.ok(feeStatisticsService.getByDepartment(year, month));
    }

    @Operation(summary = "按费用类型统计")
    @GetMapping("/by-type")
    @RequiresPermission("erp:expense:statistics:list")
    public ApiResponse<List<FeeStatisticsVO>> getByExpenseType(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok(feeStatisticsService.getByExpenseType(year, month, departmentId));
    }

    @Operation(summary = "手动刷新统计数据")
    @PostMapping("/refresh")
    @RequiresPermission("erp:expense:statistics:refresh")
    public ApiResponse<Void> refresh(@RequestParam Integer year, @RequestParam Integer month) {
        feeStatisticsService.refreshStatistics(year, month);
        return ApiResponse.success();
    }
}
