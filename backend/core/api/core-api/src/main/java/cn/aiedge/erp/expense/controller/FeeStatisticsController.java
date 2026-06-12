package cn.aiedge.erp.expense.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.FeeStatisticsQueryRequest;
import cn.aiedge.erp.expense.dto.FeeStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 费用统计占位 Controller
 * 数据库表尚未创建，返回空数据避免前端报错
 */
@Tag(name = "费用统计管理（占位）")
@RestController
@RequestMapping("/api/erp/expense/statistics")
public class FeeStatisticsController {

    @Operation(summary = "分页查询统计台账（占位）")
    @GetMapping("/page")
    public ApiResponse<PageResult<FeeStatisticsVO>> pageList(FeeStatisticsQueryRequest request) {
        return ApiResponse.ok(PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize()));
    }

    @Operation(summary = "获取汇总统计数据（占位）")
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok(Map.of("totalAmount", 0, "pendingAmount", 0, "approvedAmount", 0, "paidAmount", 0));
    }

    @Operation(summary = "按部门统计（占位）")
    @GetMapping("/by-department")
    public ApiResponse<List<FeeStatisticsVO>> getByDepartment(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.ok(Collections.emptyList());
    }

    @Operation(summary = "按费用类型统计（占位）")
    @GetMapping("/by-type")
    public ApiResponse<List<FeeStatisticsVO>> getByExpenseType(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long departmentId) {
        return ApiResponse.ok(Collections.emptyList());
    }

    @Operation(summary = "手动刷新统计数据（占位）")
    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(@RequestParam Integer year, @RequestParam Integer month) {
        return ApiResponse.success();
    }
}
