package cn.aiedge.permission.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.permission.service.PermissionSimulationService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 权限模拟控制器
 * <p>
 * 允许管理员模拟其他用户的权限上下文进行验证。
 * 也可通过请求头 {@code X-Simulate-User-Id} 自动触发。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "权限模拟", description = "管理员模拟其他用户权限")
@RestController
@RequestMapping("/api/simulate")
@RequiredArgsConstructor
public class PermissionSimulationController {

    private final PermissionSimulationService simulationService;

    @Operation(summary = "开始权限模拟")
    @PostMapping("/start")
    @SaCheckPermission("system:simulate")
    public Result<Void> startSimulation(@RequestBody Map<String, Object> params) {
        Long targetUserId = Long.valueOf(params.get("targetUserId").toString());
        String reason = (String) params.getOrDefault("reason", "");
        simulationService.startSimulation(targetUserId, reason);
        return Result.ok("开始模拟: targetUserId=" + targetUserId + ", reason=" + reason, null);
    }

    @Operation(summary = "结束权限模拟")
    @PostMapping("/stop")
    @SaCheckPermission("system:simulate")
    public Result<Void> stopSimulation() {
        simulationService.stopSimulation();
        return Result.ok("结束模拟", null);
    }

    @Operation(summary = "查询模拟状态")
    @GetMapping("/status")
    @SaCheckPermission("system:simulate")
    public Result<Map<String, Object>> getStatus() {
        return Result.ok(Map.of(
            "simulating", simulationService.isSimulating(),
            "targetUserId", simulationService.getSimulatedUserId(),
            "actualUserId", simulationService.getActualUserId(),
            "reason", simulationService.getSimulateReason()
        ));
    }
}
