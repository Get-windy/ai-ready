package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.controller.dto.DetailSaveRequest;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.ship.dto.ShipQuery;
import cn.aiedge.wms.ship.dto.WmsShipDetailPageVO;
import cn.aiedge.wms.ship.service.ShipService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Validated
@Tag(name = "发货管理")
@RestController
@RequestMapping("/api/wms/ship")
@RequiredArgsConstructor
public class ShipController {

    private final ShipService shipService;

    @Operation(summary = "新增发货任务")
    @SaCheckPermission("wms:ship:create")
    @PostMapping("/task/save")
    public Result<WmsShipTask> saveTask(@Valid @RequestBody WmsShipTask task) {
        shipService.saveTask(task);
        log.info("新增发货任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新发货任务")
    @SaCheckPermission("wms:ship:create")
    @PostMapping("/task/update")
    public Result<Boolean> updateTask(@Valid @RequestBody WmsShipTask task) {
        boolean updated = shipService.updateTask(task);
        if (updated) log.info("更新发货任务: id={}", task.getId());
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询发货任务")
    @SaCheckPermission("wms:ship:detail")
    @GetMapping("/task/{id}")
    public Result<WmsShipTask> getTaskById(@PathVariable @NotNull Long id) {
        WmsShipTask task = shipService.getTaskById(id);
        if (task == null) return Result.fail("发货任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "分页查询发货任务")
    @SaCheckPermission("wms:ship:list")
    @GetMapping("/task/page")
    public Result<Page<WmsShipTask>> taskPage(@Valid Page<WmsShipTask> page, WmsShipTask query,
                                              @RequestParam(required = false) String keyword) {
        // 前端「关键字段」是单号/来源单号模糊，注入到 taskNo 与 sourceOrderNo 供 Service 做 OR LIKE
        if (StringUtils.hasText(keyword)) {
            query.setTaskNo(keyword);
            query.setSourceOrderNo(keyword);
        }
        return Result.ok(shipService.pageTask(page, query));
    }

    @Operation(summary = "分页查询发货单(按单据，多条件)")
    @SaCheckPermission("wms:ship:view")
    @GetMapping("/task/query")
    public Result<Page<WmsShipTask>> taskQuery(@ModelAttribute ShipQuery query) {
        Page<WmsShipTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        return Result.ok(shipService.queryPage(page, query));
    }

    @Operation(summary = "分页查询发货明细(按明细)")
    @SaCheckPermission("wms:ship:view")
    @GetMapping("/task/page-detail")
    public Result<IPage<WmsShipDetailPageVO>> pageDetail(@ModelAttribute ShipQuery query) {
        IPage<WmsShipDetailPageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return Result.ok(shipService.pageDetail(page, query));
    }

    @Operation(summary = "删除发货任务")
    @SaCheckPermission("wms:ship:delete")
    @DeleteMapping("/task/{id}")
    public Result<String> deleteTask(@PathVariable @NotNull Long id) {
        shipService.removeTask(id);
        log.info("删除发货任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始发货")
    @SaCheckPermission("wms:ship:execute")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        shipService.startShip(taskId, userId, userName);
        log.info("开始发货: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始发货成功");
    }

    @Operation(summary = "扫描发货商品")
    @SaCheckPermission("wms:ship:create")
    @PostMapping("/scan")
    public Result<String> scan(@RequestParam @NotNull Long detailId,
                               @RequestParam @Positive BigDecimal scannedQuantity) {
        shipService.scanItem(detailId, scannedQuantity);
        log.info("扫描发货: detailId={}, qty={}", detailId, scannedQuantity);
        return Result.ok("扫描成功");
    }

    @Operation(summary = "确认发货")
    @SaCheckPermission("wms:ship:confirm")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam @NotNull Long taskId) {
        shipService.confirmShip(taskId);
        log.info("确认发货: taskId={}", taskId);
        return Result.ok("确认发货成功");
    }

    @Operation(summary = "取消发货")
    @SaCheckPermission("wms:ship:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long taskId,
                                 @RequestParam @NotBlank String reason) {
        shipService.cancelShip(taskId, reason);
        log.info("取消发货: taskId={}, reason={}", taskId, reason);
        return Result.ok("取消发货成功");
    }

    @Operation(summary = "查询发货明细列表")
    @SaCheckPermission("wms:ship:detail")
    @GetMapping("/details/{shipId}")
    public Result<List<WmsShipDetail>> details(@PathVariable @NotNull Long shipId) {
        return Result.ok(shipService.listByShipId(shipId));
    }

    @Operation(summary = "保存发货明细（整体替换，先删后插）")
    @SaCheckPermission("wms:ship:create")
    @PostMapping("/detail/save")
    public Result<String> saveDetails(@Valid @RequestBody DetailSaveRequest<WmsShipDetail> request) {
        shipService.saveDetails(request.getTaskId(), request.getDetails());
        log.info("保存发货明细: shipId={}, items={}", request.getTaskId(), request.getDetails().size());
        return Result.ok("保存成功");
    }
}
