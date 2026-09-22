package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.controller.dto.DetailSaveRequest;
import cn.aiedge.wms.controller.dto.WmsPutawayDetailQuery;
import cn.aiedge.wms.controller.dto.WmsPutawayDetailVO;
import cn.aiedge.wms.entity.WmsPutawayDetail;
import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.putaway.service.PutawayService;
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
@Tag(name = "上架管理")
@RestController
@RequestMapping("/api/wms/putaway")
@RequiredArgsConstructor
public class PutawayController {

    private final PutawayService putawayService;

    @Operation(summary = "新增上架任务")
    @SaCheckPermission("wms:putaway:create")
    @PostMapping("/save")
    public Result<WmsPutawayTask> save(@Valid @RequestBody WmsPutawayTask task) {
        putawayService.saveTask(task);
        log.info("新增上架任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新上架任务")
    @SaCheckPermission("wms:putaway:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsPutawayTask task) {
        boolean updated = putawayService.updateTask(task);
        if (updated) {
            log.info("更新上架任务: id={}", task.getId());
        }
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询上架任务")
    @SaCheckPermission("wms:putaway:detail")
    @GetMapping("/{id}")
    public Result<WmsPutawayTask> getById(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        WmsPutawayTask task = putawayService.getTaskById(id);
        if (task == null) return Result.fail("上架任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询上架任务")
    @SaCheckPermission("wms:putaway:list")
    @GetMapping("/page")
    public Result<Page<WmsPutawayTask>> page(@Valid Page<WmsPutawayTask> page, WmsPutawayTask query) {
        return Result.ok(putawayService.pageTask(page, query));
    }

    @Operation(summary = "按明细分页查询上架任务")
    @SaCheckPermission("wms:putaway:view")
    @GetMapping("/page-detail")
    public Result<Page<WmsPutawayDetailVO>> pageDetail(WmsPutawayDetailQuery query) {
        if (query == null) query = new WmsPutawayDetailQuery();
        Page<WmsPutawayDetail> page = new Page<>(
                query.getPageNum() == null ? 1 : query.getPageNum(),
                query.getPageSize() == null ? 20 : query.getPageSize());
        return Result.ok(putawayService.pageDetail(page, query));
    }

    @Operation(summary = "删除上架任务")
    @SaCheckPermission("wms:putaway:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        putawayService.removeTask(id);
        log.info("删除上架任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始上架")
    @SaCheckPermission("wms:putaway:execute")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        putawayService.startPutaway(taskId, userId, userName);
        log.info("开始上架: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始上架成功");
    }

    @Operation(summary = "确认上架")
    @SaCheckPermission("wms:putaway:confirm")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam @NotNull Long taskId,
                                  @RequestParam @NotNull Long userId,
                                  @RequestParam @NotBlank String userName) {
        putawayService.confirmPutaway(taskId, userId, userName);
        log.info("确认上架: taskId={}, userId={}", taskId, userId);
        return Result.ok("确认上架成功");
    }

    @Operation(summary = "取消上架")
    @SaCheckPermission("wms:putaway:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long taskId,
                                 @RequestParam(required = false) String reason) {
        putawayService.cancelPutaway(taskId, reason);
        log.info("取消上架: taskId={}, reason={}", taskId, reason);
        return Result.ok("取消成功");
    }

    @Operation(summary = "查询上架明细列表")
    @SaCheckPermission("wms:putaway:detail")
    @GetMapping("/details/{taskId}")
    public Result<List<WmsPutawayDetail>> details(
            @PathVariable @NotNull(message = "任务ID不能为空") Long taskId) {
        return Result.ok(putawayService.listByTaskId(taskId));
    }

    @Operation(summary = "保存上架明细（整体替换，先删后插）")
    @SaCheckPermission("wms:putaway:create")
    @PostMapping("/detail/save")
    public Result<String> saveDetails(@Valid @RequestBody DetailSaveRequest<WmsPutawayDetail> request) {
        putawayService.saveDetails(request.getTaskId(), request.getDetails());
        log.info("保存上架明细: taskId={}, items={}", request.getTaskId(), request.getDetails().size());
        return Result.ok("保存成功");
    }
}
