package cn.aiedge.dms.execution.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.execution.service.ExecutionService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 配送执行控制器
 *
 * 提供装车确认、取货确认、送达确认、催单和路线查询等配送执行 REST 接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "配送执行")
@RestController
@RequestMapping("/api/dms/execution")
@RequiredArgsConstructor
@SaCheckLogin
public class ExecutionController {

    private final ExecutionService executionService;

    @Operation(summary = "确认装车")
    @PostMapping("/{taskId}/load")
    @SaCheckPermission("dms:execution:operate")
    public ApiResponse<Void> confirmLoad(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        executionService.confirmLoad(taskId);
        return ApiResponse.success();
    }

    @Operation(summary = "确认取货")
    @PostMapping("/{taskId}/pickup")
    @SaCheckPermission("dms:execution:operate")
    public ApiResponse<Void> confirmPickup(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        executionService.confirmPickup(taskId);
        return ApiResponse.success();
    }

    @Operation(summary = "确认送达")
    @PostMapping("/{taskId}/arrive")
    @SaCheckPermission("dms:execution:operate")
    public ApiResponse<Void> confirmArrive(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        executionService.confirmArrive(taskId);
        return ApiResponse.success();
    }

    @Operation(summary = "催单")
    @PostMapping("/{taskId}/urge")
    @SaCheckPermission("dms:execution:operate")
    public ApiResponse<Void> urge(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        executionService.urge(taskId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取优化路线")
    @GetMapping("/{taskId}/route")
    public ApiResponse<?> getRoute(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Object route = executionService.getRoute(taskId);
        return ApiResponse.success(route);
    }
}
