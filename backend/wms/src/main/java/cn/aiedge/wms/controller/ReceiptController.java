package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.controller.dto.DetailSaveRequest;
import cn.aiedge.wms.controller.dto.WmsReceiptDetailVO;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Validated
@Tag(name = "收货管理")
@RestController
@RequestMapping("/api/wms/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "新增收货任务")
    @SaCheckPermission("wms:receipt:create")
    @PostMapping("/save")
    public Result<WmsReceiptTask> save(@Valid @RequestBody WmsReceiptTask task) {
        receiptService.saveTask(task);
        log.info("新增收货任务: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return Result.ok(task);
    }

    @Operation(summary = "更新收货任务")
    @SaCheckPermission("wms:receipt:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsReceiptTask task) {
        boolean updated = receiptService.updateTask(task);
        if (updated) {
            log.info("更新收货任务: id={}", task.getId());
        }
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询收货任务")
    @SaCheckPermission("wms:receipt:detail")
    @GetMapping("/{id}")
    public Result<WmsReceiptTask> getById(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        WmsReceiptTask task = receiptService.getTaskById(id);
        if (task == null) {
            return Result.fail("收货任务不存在");
        }
        return Result.ok(task);
    }

    @Operation(summary = "分页查询收货任务")
    @SaCheckPermission("wms:receipt:list")
    @GetMapping("/page")
    public Result<Page<WmsReceiptTask>> page(@Valid Page<WmsReceiptTask> page, WmsReceiptTask query,
                                             @RequestParam(required = false) String keyword) {
        // 前端「关键字」为单号/来源单号模糊，注入到 taskNo 与 sourceOrderNo 供 Service 做 OR LIKE
        if (StringUtils.hasText(keyword)) {
            query.setTaskNo(keyword);
            query.setSourceOrderNo(keyword);
        }
        return Result.ok(receiptService.pageTask(page, query));
    }

    @Operation(summary = "生成下一收货单号")
    @SaCheckPermission("wms:receipt:list")
    @GetMapping("/next-no")
    public Result<String> nextNo() {
        return Result.ok(receiptService.generateNo());
    }

    @Operation(summary = "分页查询收货明细(按明细)")
    @SaCheckPermission("wms:receipt:view")
    @GetMapping("/page-detail")
    public Result<Page<WmsReceiptDetailVO>> pageDetail(@Valid Page<WmsReceiptDetailVO> page,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String sourceOrderNo,
                                                       @RequestParam(required = false) Integer sourceType,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) Long warehouseId,
                                                       @RequestParam(required = false) String warehouseName,
                                                       @RequestParam(required = false) String productName,
                                                       @RequestParam(required = false) String batchNo,
                                                       @RequestParam(required = false) LocalDate dateStart,
                                                       @RequestParam(required = false) LocalDate dateEnd) {
        return Result.ok(receiptService.pageDetail(page, keyword, sourceOrderNo, sourceType, status,
                warehouseId, warehouseName, productName, batchNo, dateStart, dateEnd));
    }

    @Operation(summary = "删除收货任务")
    @SaCheckPermission("wms:receipt:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "任务ID不能为空") Long id) {
        receiptService.removeTask(id);
        log.info("删除收货任务: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "开始收货")
    @SaCheckPermission("wms:receipt:execute")
    @PostMapping("/start")
    public Result<String> start(@RequestParam @NotNull Long taskId,
                                @RequestParam @NotNull Long userId,
                                @RequestParam @NotBlank String userName) {
        receiptService.startReceipt(taskId, userId, userName);
        log.info("开始收货: taskId={}, userId={}", taskId, userId);
        return Result.ok("开始收货成功");
    }

    @Operation(summary = "确认收货")
    @SaCheckPermission("wms:receipt:confirm")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam @NotNull Long taskId,
                                  @RequestParam @NotNull Long userId,
                                  @RequestParam @NotBlank String userName) {
        receiptService.confirmReceipt(taskId, userId, userName);
        log.info("确认收货: taskId={}, userId={}", taskId, userId);
        return Result.ok("确认收货成功");
    }

    @Operation(summary = "取消收货")
    @SaCheckPermission("wms:receipt:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long taskId,
                                 @RequestParam @NotBlank String reason) {
        receiptService.cancelReceipt(taskId, reason);
        log.info("取消收货: taskId={}, reason={}", taskId, reason);
        return Result.ok("取消收货成功");
    }

    @Operation(summary = "查询收货明细列表")
    @SaCheckPermission("wms:receipt:detail")
    @GetMapping("/details/{taskId}")
    public Result<List<WmsReceiptDetail>> details(
            @PathVariable @NotNull(message = "任务ID不能为空") Long taskId) {
        return Result.ok(receiptService.listByTaskId(taskId));
    }

    @Operation(summary = "保存收货明细（整体替换，先删后插）")
    @SaCheckPermission("wms:receipt:create")
    @PostMapping("/detail/save")
    public Result<String> saveDetails(@Valid @RequestBody DetailSaveRequest<WmsReceiptDetail> request) {
        receiptService.saveDetails(request.getTaskId(), request.getDetails());
        log.info("保存收货明细: taskId={}, items={}", request.getTaskId(), request.getDetails().size());
        return Result.ok("保存成功");
    }
}
