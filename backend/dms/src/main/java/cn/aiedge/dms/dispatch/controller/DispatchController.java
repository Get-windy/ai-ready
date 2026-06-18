package cn.aiedge.dms.dispatch.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.dispatch.service.DispatchService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能调度控制器
 *
 * 提供自动分配、手动指派、改派、候选骑手查询和围栏校验等调度 REST 接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "智能调度")
@RestController
@RequestMapping("/api/dms/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @Operation(summary = "自动调度")
    @SaCheckPermission("dms:dispatch:auto")
    @PostMapping("/auto")
    public ApiResponse<Void> autoDispatch() {
        dispatchService.autoDispatch();
        return ApiResponse.success();
    }

    @Operation(summary = "手动指派骑手")
    @SaCheckPermission("dms:dispatch:assign")
    @PostMapping("/{taskId}/assign")
    public ApiResponse<Void> assignRider(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "骑手ID") @RequestParam Long riderId) {
        dispatchService.assignRider(taskId, riderId);
        return ApiResponse.success();
    }

    @Operation(summary = "改派任务")
    @SaCheckPermission("dms:dispatch:reassign")
    @PostMapping("/{taskId}/reassign")
    public ApiResponse<Void> reassign(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "原骑手ID") @RequestParam Long fromRiderId,
            @Parameter(description = "新骑手ID") @RequestParam Long toRiderId) {
        dispatchService.reassign(taskId, fromRiderId, toRiderId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取候选骑手列表")
    @SaCheckPermission("dms:dispatch:candidates")
    @GetMapping("/candidates")
    public ApiResponse<List<DmsRider>> getCandidates(
            @Parameter(description = "任务ID") @RequestParam Long taskId) {
        List<DmsRider> candidates = dispatchService.getCandidates(taskId);
        return ApiResponse.success(candidates);
    }

    @Operation(summary = "围栏校验")
    @SaCheckPermission("dms:dispatch:fence")
    @PostMapping("/{taskId}/fence-check")
    public ApiResponse<Boolean> fenceCheck(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "骑手ID") @RequestParam Long riderId) {
        boolean result = dispatchService.fenceCheck(taskId, riderId);
        return ApiResponse.success(result);
    }
}
