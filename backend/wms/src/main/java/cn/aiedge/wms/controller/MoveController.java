package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.controller.dto.DetailSaveRequest;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.move.dto.MoveTaskQuery;
import cn.aiedge.wms.move.dto.MoveDetailVO;
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
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Validated
@Tag(name = "移库管理")
@RestController
@RequestMapping("/api/wms/move")
@RequiredArgsConstructor
public class MoveController {

    private final MoveService moveService;

    @Operation(summary = "新增移库任务")
    @SaCheckPermission("wms:move:create")
    @PostMapping("/save")
    public Result<WmsMoveTask> save(@Valid @RequestBody WmsMoveTask task) {
        moveService.saveTask(task);
        log.info("新增移库任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新移库任务")
    @SaCheckPermission("wms:move:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsMoveTask task) {
        boolean updated = moveService.updateTask(task);
        if (updated) log.info("更新移库任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询移库任务")
    @SaCheckPermission("wms:move:detail")
    @GetMapping("/{id}")
    public Result<WmsMoveTask> getById(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        WmsMoveTask task = moveService.getTaskById(id);
        if (task == null) return Result.fail("移库任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询移库任务（按单据）")
    @SaCheckPermission("wms:move:list")
    @GetMapping("/page")
    public Result<Page<WmsMoveTask>> page(MoveTaskQuery query) {
        Page<WmsMoveTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        return Result.ok(moveService.pageTask(page, query));
    }

    @Operation(summary = "分页查询移库明细（按明细）")
    @SaCheckPermission("wms:move:view")
    @GetMapping("/page-detail")
    public Result<Page<MoveDetailVO>> pageDetail(MoveTaskQuery query) {
        Page<MoveDetailVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return Result.ok(moveService.pageDetail(page, query));
    }

    @Operation(summary = "生成移库单号")
    @SaCheckPermission("wms:move:list")
    @GetMapping("/next-no")
    public Result<String> nextNo() {
        return Result.ok(moveService.nextNo());
    }

    @Operation(summary = "批量删除移库任务")
    @SaCheckPermission("wms:move:delete")
    @DeleteMapping("/batch")
    public Result<String> batchDelete(@RequestBody java.util.List<Long> ids) {
        for (Long id : ids) {
            moveService.removeTask(id);
        }
        log.info("批量删除移库任务: ids={}", ids);
        return Result.ok("批量删除成功");
    }

    @Operation(summary = "删除移库任务")
    @SaCheckPermission("wms:move:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        moveService.removeTask(id);
        log.info("删除移库任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始移库")
    @SaCheckPermission("wms:move:execute")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        moveService.startMove(taskId, userId, userName);
        log.info("开始移库: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始移库成功");
    }

    @Operation(summary = "执行移库")
    @SaCheckPermission("wms:move:execute")
    @PostMapping("/execute")
    public Result<String> execute(@RequestParam @NotNull Long taskId,
                                  @RequestParam @NotNull Long userId,
                                  @RequestParam @NotBlank String userName) {
        moveService.executeMove(taskId, userId, userName);
        log.info("执行移库: taskId={}, userId={}", taskId, userId);
        return Result.ok("执行移库成功");
    }

    @Operation(summary = "取消移库")
    @SaCheckPermission("wms:move:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long taskId,
                                 @RequestParam(required = false) String reason) {
        moveService.cancelMove(taskId, reason);
        log.info("取消移库: taskId={}, reason={}", taskId, reason);
        return Result.ok("取消移库成功");
    }

    @Operation(summary = "查询移库明细列表")
    @SaCheckPermission("wms:move:detail")
    @GetMapping("/details/{taskId}")
    public Result<List<WmsMoveDetail>> details(
            @PathVariable @NotNull(message = "任务ID不能为空") Long taskId) {
        return Result.ok(moveService.listByTaskId(taskId));
    }

    @Operation(summary = "保存移库明细（整体替换，先删后插）")
    @SaCheckPermission("wms:move:create")
    @PostMapping("/detail/save")
    public Result<String> saveDetails(@Valid @RequestBody DetailSaveRequest<WmsMoveDetail> request) {
        moveService.saveDetails(request.getTaskId(), request.getDetails());
        log.info("保存移库明细: taskId={}, items={}", request.getTaskId(), request.getDetails().size());
        return Result.ok("保存成功");
    }
}
