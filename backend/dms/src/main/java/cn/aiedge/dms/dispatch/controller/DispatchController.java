package cn.aiedge.dms.dispatch.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.dispatch.dto.DispatchCandidateVO;
import cn.aiedge.dms.dispatch.dto.DispatchPreviewVO;
import cn.aiedge.dms.dispatch.dto.DispatchStatVO;
import cn.aiedge.dms.dispatch.dto.DispatchStrategyDTO;
import cn.aiedge.dms.dispatch.service.DispatchService;
import cn.aiedge.dms.task.dto.BatchResultVO;
import cn.aiedge.dms.task.dto.DmsTaskBatchDTO;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 智能调度控制器（配送 → 调度管理 → 智能调度，菜单 80850 / `dms:dispatch`）
 *
 * <p>本页定位为《智能调度开发文档》§3.1 的「**调度策略与执行台**」：策略配置 / 调度预览与执行 / 效果复盘；
 * 任务列表与手工指派入口复用《调度任务》，**不新增 `/dispatch/page`**（避免与 `/task/page` 重复造列表）。</p>
 *
 * <p>参数口径统一为 <b>JSON body</b>（原 `assign`/`reassign` 用 `@RequestParam`，前端发 body 会 400）；
 * 为兼容既有调用方，body 缺省时回落同名 query 参数。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "智能调度", description = "派单策略配置 / 调度预览与执行 / 候选配送员 / 效果复盘")
@RestController
@RequestMapping("/api/dms/dispatch")
@RequiredArgsConstructor
@SaCheckLogin
public class DispatchController {

    private final DispatchService dispatchService;

    // ==================== 策略配置 ====================

    @Operation(summary = "读取派单策略与约束")
    @GetMapping("/strategy")
    @SaCheckPermission("dms:dispatch:view")
    public ApiResponse<DispatchStrategyDTO> strategy() {
        return ApiResponse.ok(dispatchService.strategy());
    }

    @Operation(summary = "保存派单策略与约束（配置中心，保存即热生效）")
    @PutMapping("/strategy")
    @SaCheckPermission("dms:dispatch:config")
    public ApiResponse<DispatchStrategyDTO> saveStrategy(@RequestBody DispatchStrategyDTO dto) {
        return ApiResponse.ok("策略已保存", dispatchService.saveStrategy(dto));
    }

    // ==================== 调度执行 ====================

    @Operation(summary = "自动调度预览（不落库，返回逐单命中规则与理由）")
    @PostMapping("/auto/preview")
    @SaCheckPermission("dms:dispatch:auto")
    public ApiResponse<DispatchPreviewVO> preview(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(dispatchService.preview(limitOf(body)));
    }

    @Operation(summary = "执行自动调度（dryRun=true 只预览；返回逐单结果与失败原因）")
    @PostMapping("/auto")
    @SaCheckPermission("dms:dispatch:auto")
    public ApiResponse<DispatchPreviewVO> autoDispatch(@RequestBody(required = false) Map<String, Object> body) {
        boolean dryRun = body != null && Boolean.parseBoolean(String.valueOf(body.getOrDefault("dryRun", "false")));
        return ApiResponse.ok(dryRun ? "预览完成" : "自动调度完成", dispatchService.autoDispatchResult(dryRun, limitOf(body)));
    }

    @Operation(summary = "手动指派配送员（body 优先，兼容旧 query 参数）")
    @PostMapping("/{taskId}/assign")
    @SaCheckPermission("dms:dispatch:assign")
    public ApiResponse<Void> assignRider(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> body,
            @Parameter(description = "配送员ID（兼容旧参数）") @RequestParam(required = false) Long riderId) {
        Long target = body != null && body.get("riderId") != null
                ? Long.valueOf(String.valueOf(body.get("riderId"))) : riderId;
        if (target == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("配送员ID不能为空");
        }
        dispatchService.assignRider(taskId, target, textOf(body, "reason"));
        return ApiResponse.ok("指派成功", null);
    }

    @Operation(summary = "改派任务（body 优先，兼容旧 query 参数）")
    @PostMapping("/{taskId}/reassign")
    @SaCheckPermission("dms:dispatch:reassign")
    public ApiResponse<Void> reassign(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long fromRiderId,
            @RequestParam(required = false) Long toRiderId) {
        Long from = body != null && body.get("fromRiderId") != null
                ? Long.valueOf(String.valueOf(body.get("fromRiderId"))) : fromRiderId;
        Long to = body != null && body.get("toRiderId") != null
                ? Long.valueOf(String.valueOf(body.get("toRiderId"))) : toRiderId;
        if (from == null || to == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("原配送员与新配送员不能为空");
        }
        dispatchService.reassign(taskId, from, to, textOf(body, "reason"));
        return ApiResponse.ok("改派成功", null);
    }

    @Operation(summary = "批量指派配送员（逐单结果反馈）")
    @PostMapping("/batch-assign")
    @SaCheckPermission("dms:dispatch:assign")
    public ApiResponse<BatchResultVO> batchAssign(@RequestBody DmsTaskBatchDTO dto) {
        return ApiResponse.ok("批量指派完成",
                dispatchService.batchAssign(dto.getTaskIds(), dto.getRiderId(), dto.getReason()));
    }

    @Operation(summary = "超时升级扫描（超时未接单自动重派；已超时在途告警）")
    @PostMapping("/escalate-overdue")
    @SaCheckPermission("dms:dispatch:auto")
    public ApiResponse<BatchResultVO> escalateOverdue() {
        BatchResultVO result = dispatchService.escalateOverdue();
        return ApiResponse.ok("超时升级扫描完成", result);
    }

    @Operation(summary = "候选配送员（含评分、在途负载与约束说明）")
    @GetMapping("/candidates")
    @SaCheckPermission("dms:dispatch:candidates")
    public ApiResponse<List<DispatchCandidateVO>> getCandidates(
            @Parameter(description = "任务ID") @RequestParam Long taskId) {
        return ApiResponse.ok(dispatchService.candidateVO(taskId));
    }

    @Operation(summary = "围栏校验")
    @PostMapping("/{taskId}/fence-check")
    @SaCheckPermission("dms:dispatch:fence")
    public ApiResponse<Boolean> fenceCheck(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) Long riderId) {
        Long target = body != null && body.get("riderId") != null
                ? Long.valueOf(String.valueOf(body.get("riderId"))) : riderId;
        if (target == null) {
            throw new cn.aiedge.dms.common.exception.DmsBusinessException("配送员ID不能为空");
        }
        return ApiResponse.ok(dispatchService.fenceCheck(taskId, target));
    }

    // ==================== 效果复盘 ====================

    @Operation(summary = "调度效果统计（自动占比 / 平均派单耗时 / 超时率）")
    @GetMapping("/stat")
    @SaCheckPermission("dms:dispatch:view")
    public ApiResponse<DispatchStatVO> stat() {
        return ApiResponse.ok(dispatchService.stat());
    }

    private Integer limitOf(Map<String, Object> body) {
        if (body == null || body.get("maxTasks") == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(body.get("maxTasks")));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取 body 内文本参数（缺失/空串返回 null） */
    private String textOf(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }
}
