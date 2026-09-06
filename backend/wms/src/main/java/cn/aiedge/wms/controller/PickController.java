package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.controller.dto.DetailSaveRequest;
import cn.aiedge.wms.entity.WmsPickDetail;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.entity.WmsPickWave;
import cn.aiedge.wms.pick.dto.PickTaskDetailVO;
import cn.aiedge.wms.pick.dto.PickTaskQuery;
import cn.aiedge.wms.pick.service.PickService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Validated
@Tag(name = "拣货管理")
@RestController
@RequestMapping("/api/wms/pick")
@RequiredArgsConstructor
public class PickController {

    private final PickService pickService;

    // ==================== 波次管理 ====================

    @Operation(summary = "从销售订单创建波次")
    @PostMapping("/wave/create")
    public Result<WmsPickWave> createWave(@RequestBody List<Long> saleOrderIds) {
        WmsPickWave wave = pickService.createWave(saleOrderIds);
        log.info("从销售订单创建波次: waveId={}, orderCount={}", wave.getId(), saleOrderIds.size());
        return Result.ok(wave);
    }

    @Operation(summary = "新增波次")
    @PostMapping("/wave/save")
    public Result<WmsPickWave> saveWave(@Valid @RequestBody WmsPickWave wave) {
        pickService.saveWave(wave);
        log.info("新增波次: id={}, waveNo={}", wave.getId(), wave.getWaveNo());
        return Result.ok(wave);
    }

    @Operation(summary = "更新波次")
    @PostMapping("/wave/update")
    public Result<Boolean> updateWave(@Valid @RequestBody WmsPickWave wave) {
        boolean updated = pickService.updateWave(wave);
        if (updated) log.info("更新波次: id={}", wave.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询波次")
    @GetMapping("/wave/{id}")
    public Result<WmsPickWave> getWaveById(@PathVariable @NotNull Long id) {
        WmsPickWave wave = pickService.getWaveById(id);
        if (wave == null) return Result.fail("波次不存在");
        return Result.ok(wave);
    }

    @Operation(summary = "分页查询波次")
    @GetMapping("/wave/page")
    public Result<Page<WmsPickWave>> wavePage(@Valid Page<WmsPickWave> page, WmsPickWave query) {
        return Result.ok(pickService.pageWave(page, query));
    }

    @Operation(summary = "删除波次")
    @DeleteMapping("/wave/{id}")
    public Result<String> deleteWave(@PathVariable @NotNull Long id) {
        pickService.removeWave(id);
        log.info("删除波次: id={}", id);
        return Result.ok("删除成功");
    }

    // ==================== 任务管理 ====================

    @Operation(summary = "新增拣货任务")
    @PostMapping("/task/save")
    public Result<WmsPickTask> saveTask(@Valid @RequestBody WmsPickTask task) {
        pickService.saveTask(task);
        log.info("新增拣货任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新拣货任务")
    @PostMapping("/task/update")
    public Result<Boolean> updateTask(@Valid @RequestBody WmsPickTask task) {
        boolean updated = pickService.updateTask(task);
        if (updated) log.info("更新拣货任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询拣货任务")
    @GetMapping("/task/{id}")
    public Result<WmsPickTask> getTaskById(@PathVariable @NotNull Long id) {
        WmsPickTask task = pickService.getTaskById(id);
        if (task == null) return Result.fail("拣货任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询拣货任务")
    @GetMapping("/task/page")
    public Result<Page<WmsPickTask>> taskPage(@Valid Page<WmsPickTask> page, WmsPickTask query,
                                              @RequestParam(required = false) String keyword) {
        // 前端「关键字」是单号/来源单号模糊，注入到 taskNo 与 sourceOrderNo 供 Service 做 OR LIKE
        if (StringUtils.hasText(keyword)) {
            query.setTaskNo(keyword);
            query.setSourceOrderNo(keyword);
        }
        return Result.ok(pickService.pageTask(page, query));
    }

    @Operation(summary = "删除拣货任务")
    @DeleteMapping("/task/{id}")
    public Result<String> deleteTask(@PathVariable @NotNull Long id) {
        pickService.removeTask(id);
        log.info("删除拣货任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始拣货")
    @PostMapping("/task/start")
    public Result<String> startTask(@RequestParam @NotNull Long taskId,
                                    @RequestParam @NotNull Long userId,
                                    @RequestParam @NotBlank String userName) {
        pickService.startPick(taskId, userId, userName);
        log.info("开始拣货: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始拣货成功");
    }

    @Operation(summary = "完成拣货")
    @PostMapping("/task/complete")
    public Result<String> completeTask(@RequestParam @NotNull Long taskId) {
        pickService.completePick(taskId);
        log.info("完成拣货: taskId={}", taskId);
        return Result.ok("完成拣货成功");
    }

    @Operation(summary = "取消拣货任务")
    @PostMapping("/task/cancel")
    public Result<String> cancelTask(@RequestParam @NotNull Long taskId,
                                     @RequestParam(required = false) String reason) {
        pickService.cancelTask(taskId, reason);
        log.info("取消拣货任务: taskId={}, reason={}", taskId, reason);
        return Result.ok("取消成功");
    }

    @Operation(summary = "根据波次查询拣货任务列表")
    @GetMapping("/task/list-by-wave/{waveId}")
    public Result<List<WmsPickTask>> listTasksByWave(@PathVariable @NotNull Long waveId) {
        return Result.ok(pickService.listByWaveId(waveId));
    }

    // ==================== 明细管理 ====================

    @Operation(summary = "确认拣货明细")
    @PostMapping("/detail/confirm")
    public Result<String> confirmDetail(@RequestParam @NotNull Long detailId,
                                        @RequestParam @Positive BigDecimal pickedQuantity) {
        pickService.confirmPickItem(detailId, pickedQuantity);
        log.info("确认拣货: detailId={}, quantity={}", detailId, pickedQuantity);
        return Result.ok("确认拣货成功");
    }

    @Operation(summary = "标记缺货")
    @PostMapping("/detail/shortage")
    public Result<String> shortage(@RequestParam @NotNull Long detailId,
                                   @RequestParam @Positive BigDecimal shortageQuantity) {
        pickService.markShortage(detailId, shortageQuantity);
        log.info("标记缺货: detailId={}, shortage={}", detailId, shortageQuantity);
        return Result.ok("标记缺货成功");
    }

    @Operation(summary = "查询拣货明细列表")
    @GetMapping("/detail/list/{taskId}")
    public Result<List<WmsPickDetail>> listDetails(@PathVariable @NotNull Long taskId) {
        return Result.ok(pickService.listByTaskId(taskId));
    }

    @Operation(summary = "保存拣货明细（整体替换，先删后插）")
    @PostMapping("/detail/save")
    public Result<String> saveDetails(@Valid @RequestBody DetailSaveRequest<WmsPickDetail> request) {
        pickService.saveDetails(request.getTaskId(), request.getDetails());
        log.info("保存拣货明细: taskId={}, items={}", request.getTaskId(), request.getDetails().size());
        return Result.ok("保存成功");
    }

    // ==================== 金标准查询 / 编号 ====================

    @Operation(summary = "生成下一拣货单号")
    @GetMapping("/next-no")
    public Result<String> nextNo() {
        return Result.ok(pickService.generateNo());
    }

    @Operation(summary = "多条件分页查询拣货单(按单据)")
    @GetMapping("/doc-query")
    public Result<Page<WmsPickTask>> docQuery(PickTaskQuery query) {
        return Result.ok(pickService.pageOrderByQuery(query));
    }

    @Operation(summary = "分页查询拣货明细(按明细)")
    @GetMapping("/page-detail")
    public Result<Page<PickTaskDetailVO>> pageDetail(PickTaskQuery query) {
        return Result.ok(pickService.pageDetail(query));
    }
}
