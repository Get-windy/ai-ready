package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsCheckResult;
import cn.aiedge.wms.entity.WmsCheckTask;
import cn.aiedge.wms.check.service.CheckService;
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
@Tag(name = "盘点管理")
@RestController
@RequestMapping("/api/wms/check")
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    @Operation(summary = "新增盘点任务")
    @PostMapping("/save")
    public Result<WmsCheckTask> save(@Valid @RequestBody WmsCheckTask task) {
        checkService.saveTask(task);
        log.info("新增盘点任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新盘点任务")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsCheckTask task) {
        boolean updated = checkService.updateTask(task);
        if (updated) log.info("更新盘点任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询盘点任务")
    @GetMapping("/{id}")
    public Result<WmsCheckTask> getById(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        WmsCheckTask task = checkService.getTaskById(id);
        if (task == null) return Result.fail("盘点任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询盘点任务")
    @GetMapping("/page")
    public Result<Page<WmsCheckTask>> page(@Valid Page<WmsCheckTask> page, WmsCheckTask query) {
        return Result.ok(checkService.pageTask(page, query));
    }

    @Operation(summary = "删除盘点任务")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        checkService.removeTask(id);
        log.info("删除盘点任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始盘点")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        checkService.startCheck(taskId, userId, userName);
        log.info("开始盘点: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始盘点成功");
    }

    @Operation(summary = "提交盘点结果")
    @PostMapping("/submit")
    public Result<String> submit(@RequestParam @NotNull Long taskId,
                                 @RequestParam @NotNull Long userId,
                                 @RequestParam @NotBlank String userName) {
        checkService.submitResult(taskId, userId, userName);
        log.info("提交盘点结果: taskId={}, userId={}", taskId, userId);
        return Result.ok("提交盘点结果成功");
    }

    @Operation(summary = "审核盘点")
    @PostMapping("/approve")
    public Result<String> approve(@RequestParam @NotNull Long taskId,
                                  @RequestParam @NotNull Long userId) {
        checkService.approveCheck(taskId, userId);
        log.info("审核盘点: taskId={}, userId={}", taskId, userId);
        return Result.ok("审核盘点成功");
    }

    @Operation(summary = "查询盘点结果列表")
    @GetMapping("/results/{taskId}")
    public Result<List<WmsCheckResult>> results(@PathVariable @NotNull Long taskId) {
        return Result.ok(checkService.listByTaskId(taskId));
    }
}
