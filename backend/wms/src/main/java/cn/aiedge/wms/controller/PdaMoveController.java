package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.move.service.MoveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/move")
@Tag(name = "PDA-移库")
@RequiredArgsConstructor
public class PdaMoveController {

    private final MoveService moveService;

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "PDA操作员";

    @Operation(summary = "创建移库任务")
    @PostMapping
    public Result<Map<String, Object>> create(@Valid @RequestBody WmsMoveTask task) {
        moveService.saveTask(task);
        log.info("PDA创建移库任务: id={}", task.getId());
        return Result.ok(Map.of("id", task.getId()));
    }

    @Operation(summary = "获取移库任务详情")
    @GetMapping("/{id}")
    public Result<WmsMoveTask> detail(@PathVariable @NotNull Long id) {
        WmsMoveTask task = moveService.getTaskById(id);
        if (task == null) return Result.fail("移库任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "执行移库")
    @PostMapping("/{id}/execute")
    public Result<Void> execute(@PathVariable @NotNull Long id) {
        moveService.executeMove(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
        log.info("PDA执行移库: taskId={}", id);
        return Result.ok();
    }
}
