package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.check.service.CheckService;
import cn.aiedge.wms.entity.WmsCheckTask;
import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.move.service.MoveService;
import cn.aiedge.wms.pick.service.PickService;
import cn.aiedge.wms.putaway.service.PutawayService;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/tasks")
@Tag(name = "PDA-任务聚合")
@RequiredArgsConstructor
public class PdaTaskController {

    private final ReceiptService receiptService;
    private final PutawayService putawayService;
    private final PickService pickService;
    private final MoveService moveService;
    private final CheckService checkService;

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "PDA操作员";

    public record TaskDTO(Long taskId, String taskNo, String taskType,
                          Integer status, String warehouseName,
                          LocalDateTime createTime) {}

    @Operation(summary = "获取所有任务列表（聚合所有作业类型）")
    @GetMapping
    public Result<List<TaskDTO>> listTasks() {
        List<TaskDTO> tasks = new ArrayList<>();

        for (var t : receiptService.pageTask(new Page<>(1, 1000), new WmsReceiptTask()).getRecords()) {
            tasks.add(new TaskDTO(t.getId(), t.getTaskNo(), "RECEIPT",
                    t.getStatus(), t.getWarehouseName(), t.getCreateTime()));
        }
        for (var t : putawayService.pageTask(new Page<>(1, 1000), new WmsPutawayTask()).getRecords()) {
            tasks.add(new TaskDTO(t.getId(), t.getTaskNo(), "PUTAWAY",
                    t.getStatus(), t.getWarehouseName(), t.getCreateTime()));
        }
        for (var t : pickService.pageTask(new Page<>(1, 1000), new WmsPickTask()).getRecords()) {
            tasks.add(new TaskDTO(t.getId(), t.getTaskNo(), "PICK",
                    t.getStatus(), t.getWarehouseName(), t.getCreateTime()));
        }
        for (var t : moveService.pageTask(new Page<>(1, 1000), new WmsMoveTask()).getRecords()) {
            tasks.add(new TaskDTO(t.getId(), t.getTaskNo(), "MOVE",
                    t.getStatus(), t.getWarehouseName(), t.getCreateTime()));
        }
        for (var t : checkService.pageTask(new Page<>(1, 1000), new WmsCheckTask()).getRecords()) {
            tasks.add(new TaskDTO(t.getId(), t.getTaskNo(), "CHECK",
                    t.getStatus(), t.getWarehouseName(), t.getCreateTime()));
        }
        return Result.ok(tasks);
    }

    @Operation(summary = "获取任务详情（需指定 taskType）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTask(@PathVariable @NotNull Long id,
                                               @RequestParam @NotBlank String taskType) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("taskId", id);
        detail.put("taskType", taskType);

        switch (taskType.toUpperCase()) {
            case "RECEIPT" -> {
                var task = receiptService.getTaskById(id);
                if (task == null) return Result.fail("收货任务不存在");
                detail.put("task", task);
            }
            case "PUTAWAY" -> {
                var task = putawayService.getTaskById(id);
                if (task == null) return Result.fail("上架任务不存在");
                detail.put("task", task);
            }
            case "PICK" -> {
                var task = pickService.getTaskById(id);
                if (task == null) return Result.fail("拣货任务不存在");
                detail.put("task", task);
            }
            case "MOVE" -> {
                var task = moveService.getTaskById(id);
                if (task == null) return Result.fail("移库任务不存在");
                detail.put("task", task);
            }
            case "CHECK" -> {
                var task = checkService.getTaskById(id);
                if (task == null) return Result.fail("盘点任务不存在");
                detail.put("task", task);
            }
            default -> {
                return Result.fail("无效的任务类型: " + taskType);
            }
        }
        return Result.ok(detail);
    }

    @Operation(summary = "领取/开始任务")
    @PutMapping("/{id}/start")
    public Result<Void> startTask(@PathVariable @NotNull Long id,
                                  @RequestBody Map<String, String> body) {
        String taskType = body.get("taskType");
        if (taskType == null) return Result.fail("缺少 taskType 参数");

        switch (taskType.toUpperCase()) {
            case "RECEIPT" -> receiptService.startReceipt(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "PUTAWAY" -> putawayService.startPutaway(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "PICK" -> pickService.startPick(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "MOVE" -> moveService.startMove(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "CHECK" -> checkService.startCheck(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            default -> { return Result.fail("无效的任务类型: " + taskType); }
        }
        log.info("PDA开始任务: taskId={}, type={}", id, taskType);
        return Result.ok();
    }

    @Operation(summary = "完成任务")
    @PutMapping("/{id}/complete")
    public Result<Void> completeTask(@PathVariable @NotNull Long id,
                                     @RequestBody Map<String, String> body) {
        String taskType = body.get("taskType");
        if (taskType == null) return Result.fail("缺少 taskType 参数");

        switch (taskType.toUpperCase()) {
            case "RECEIPT" -> receiptService.confirmReceipt(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "PUTAWAY" -> putawayService.confirmPutaway(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "PICK" -> pickService.completePick(id);
            case "MOVE" -> moveService.executeMove(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            case "CHECK" -> checkService.submitResult(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
            default -> { return Result.fail("无效的任务类型: " + taskType); }
        }
        log.info("PDA完成任务: taskId={}, type={}", id, taskType);
        return Result.ok();
    }
}
