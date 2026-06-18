package cn.aiedge.dms.task.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.service.TaskService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 配送任务管理控制器
 */
@Tag(name = "配送任务管理")
@RestController
@RequestMapping("/api/dms/task")
@RequiredArgsConstructor
@SaCheckLogin
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "分页查询配送任务")
    @GetMapping("/page")
    public ApiResponse<IPage<DmsTask>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            DmsTask query) {
        Page<DmsTask> page = new Page<>(current, size);
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        if (query.getTenantId() != null) {
            wrapper.eq(DmsTask::getTenantId, query.getTenantId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DmsTask::getStatus, query.getStatus());
        }
        if (query.getRiderId() != null) {
            wrapper.eq(DmsTask::getRiderId, query.getRiderId());
        }
        if (query.getOrderNo() != null) {
            wrapper.like(DmsTask::getOrderNo, query.getOrderNo());
        }
        wrapper.orderByDesc(DmsTask::getCreateTime);
        return ApiResponse.ok(taskService.page(page, wrapper));
    }

    @Operation(summary = "获取配送任务详情")
    @GetMapping("/{id}")
    public ApiResponse<DmsTask> getById(@Parameter(description = "任务ID") @PathVariable Long id) {
        return ApiResponse.ok(taskService.getById(id));
    }

    @Operation(summary = "创建配送任务")
    @PostMapping
    @SaCheckPermission("dms:task:create")
    public ApiResponse<DmsTask> create(@Parameter(description = "任务信息") @Valid @RequestBody DmsTask task) {
        return ApiResponse.ok(taskService.create(task));
    }

    @Operation(summary = "更新配送任务")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:task:update")
    public ApiResponse<Void> update(@Parameter(description = "任务ID") @PathVariable Long id, @Parameter(description = "任务信息") @RequestBody DmsTask task) {
        task.setId(id);
        taskService.update(task);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "更新任务状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:task:update")
    public ApiResponse<Void> updateStatus(@Parameter(description = "任务ID") @PathVariable Long id, @Parameter(description = "状态变更信息(fromStatus, toStatus)") @RequestBody Map<String, Integer> body) {
        Integer fromStatus = body.get("fromStatus");
        Integer toStatus = body.get("toStatus");
        taskService.updateStatus(id, fromStatus, toStatus);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("dms:task:cancel")
    public ApiResponse<Void> cancel(@Parameter(description = "任务ID") @PathVariable Long id) {
        taskService.cancel(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "标记任务异常")
    @PostMapping("/{id}/exception")
    @SaCheckPermission("dms:task:update")
    public ApiResponse<Void> markException(@Parameter(description = "任务ID") @PathVariable Long id) {
        taskService.markException(id);
        return ApiResponse.ok(null);
    }
}
