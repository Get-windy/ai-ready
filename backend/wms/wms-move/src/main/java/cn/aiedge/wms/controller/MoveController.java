package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.move.service.MoveService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@Tag(name = "移库管理")
@RestController
@RequestMapping("/api/wms/move")
@RequiredArgsConstructor
public class MoveController {

    private final MoveService moveService;

    @Operation(summary = "新增移库任务")
    @PostMapping("/save")
    public Result<WmsMoveTask> save(@Valid @RequestBody WmsMoveTask task) {
        moveService.saveTask(task);
        log.info("新增移库任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新移库任务")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsMoveTask task) {
        boolean updated = moveService.updateTask(task);
        if (updated) log.info("更新移库任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询移库任务")
    @GetMapping("/{id}")
    public Result<WmsMoveTask> getById(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        WmsMoveTask task = moveService.getTaskById(id);
        if (task == null) return Result.fail("移库任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询移库任务")
    @GetMapping("/page")
    public Result<Page<WmsMoveTask>> page(@Valid Page<WmsMoveTask> page, WmsMoveTask query) {
        return Result.ok(moveService.pageTask(page, query));
    }

    @Operation(summary = "删除移库任务")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        moveService.removeTask(id);
        log.info("删除移库任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始移库")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        moveService.startMove(taskId, userId, userName);
        log.info("开始移库: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始移库成功");
    }

    @Operation(summary = "执行移库")
    @PostMapping("/execute")
    public Result<String> execute(@RequestParam @NotNull Long taskId,
                                  @RequestParam @NotNull Long userId,
                                  @RequestParam @NotBlank String userName) {
        moveService.executeMove(taskId, userId, userName);
        log.info("执行移库: taskId={}, userId={}", taskId, userId);
        return Result.ok("执行移库成功");
    }

    @Operation(summary = "查询移库明细列表")
    @GetMapping("/details/{taskId}")
    public Result<List<WmsMoveDetail>> details(
            @PathVariable @NotNull(message = "任务ID不能为空") Long taskId) {
        return Result.ok(moveService.listByTaskId(taskId));
    }
}
