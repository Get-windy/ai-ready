package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.ship.service.ShipService;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Validated
@Tag(name = "发货管理")
@RestController
@RequestMapping("/api/wms/ship")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService shipService;

    @Operation(summary = "新增发货任务")
    @PostMapping("/task/save")
    public Result<WmsShipTask> saveTask(@Valid @RequestBody WmsShipTask task) {
        shipService.saveTask(task);
        log.info("新增发货任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新发货任务")
    @PostMapping("/task/update")
    public Result<Boolean> updateTask(@Valid @RequestBody WmsShipTask task) {
        boolean updated = shipService.updateTask(task);
        if (updated) log.info("更新发货任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询发货任务")
    @GetMapping("/task/{id}")
    public Result<WmsShipTask> getTaskById(@PathVariable @NotNull Long id) {
        WmsShipTask task = shipService.getTaskById(id);
        if (task == null) return Result.fail("发货任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询发货任务")
    @GetMapping("/task/page")
    public Result<Page<WmsShipTask>> taskPage(@Valid Page<WmsShipTask> page, WmsShipTask query) {
        return Result.ok(shipService.pageTask(page, query));
    }

    @Operation(summary = "删除发货任务")
    @DeleteMapping("/task/{id}")
    public Result<String> deleteTask(@PathVariable @NotNull Long id) {
        shipService.removeTask(id);
        log.info("删除发货任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始发货")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        shipService.startShip(taskId, userId, userName);
        log.info("开始发货: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始发货成功");
    }

    @Operation(summary = "扫描发货商品")
    @PostMapping("/scan")
    public Result<String> scan(@RequestParam @NotNull Long detailId,
                               @RequestParam @Positive BigDecimal scannedQuantity) {
        shipService.scanItem(detailId, scannedQuantity);
        log.info("扫描发货: detailId={}, qty={}", detailId, scannedQuantity);
        return Result.ok("扫描成功");
    }

    @Operation(summary = "确认发货")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam @NotNull Long taskId) {
        shipService.confirmShip(taskId);
        log.info("确认发货: taskId={}", taskId);
        return Result.ok("确认发货成功");
    }

    @Operation(summary = "查询发货明细列表")
    @GetMapping("/details/{shipId}")
    public Result<List<WmsShipDetail>> details(@PathVariable @NotNull Long shipId) {
        return Result.ok(shipService.listByShipId(shipId));
    }
}
