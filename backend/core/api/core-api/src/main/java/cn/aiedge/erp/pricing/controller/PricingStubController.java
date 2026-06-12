package cn.aiedge.erp.pricing.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 定价审批占位 Controller
 * 全模块尚未完整实现，返回空数据避免前端 404
 */
@Tag(name = "定价审批（占位）")
@RestController
@RequestMapping("/api/erp/pricing")
public class PricingStubController {

    @Operation(summary = "审批统计（占位）")
    @GetMapping("/approval/statistics")
    public Result<Map<String, Object>> approvalStatistics() {
        return Result.ok(Map.of("totalCount", 0, "pendingCount", 0, "approvedCount", 0, "rejectedCount", 0));
    }

    @Operation(summary = "待审批列表（占位）")
    @GetMapping("/approval/pending")
    public Result<List<?>> pendingApprovals() {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "我的申请（占位）")
    @GetMapping("/approval/my/{applicantId}")
    public Result<List<?>> myApprovals(@PathVariable Long applicantId) {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "按状态列表（占位）")
    @GetMapping("/approval/list/{status}")
    public Result<List<?>> approvalsByStatus(@PathVariable String status) {
        return Result.ok(Collections.emptyList());
    }

    @Operation(summary = "价格层级列表（占位）")
    @GetMapping("/tiers/list")
    public Result<List<?>> tiersList() {
        return Result.ok(Collections.emptyList());
    }
}
