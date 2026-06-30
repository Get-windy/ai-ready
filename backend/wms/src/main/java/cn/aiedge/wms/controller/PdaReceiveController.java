package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.receipt.service.ReceiptService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/receive")
@Tag(name = "PDA-收货")
@RequiredArgsConstructor
public class PdaReceiveController {

    /**
     * 扫码请求 DTO.
     */
    public record ScanRequest(String barcode) {}

    private final ReceiptService receiptService;

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "PDA操作员";

    @Operation(summary = "获取收货任务列表")
    @GetMapping
    public Result<List<WmsReceiptTask>> list() {
        Page<WmsReceiptTask> page = receiptService.pageTask(
                new Page<>(1, 1000), new WmsReceiptTask());
        return Result.ok(page.getRecords());
    }

    @Operation(summary = "获取收货任务详情")
    @GetMapping("/{id}")
    public Result<WmsReceiptTask> detail(@PathVariable @NotNull Long id) {
        WmsReceiptTask task = receiptService.getTaskById(id);
        if (task == null) {
            return Result.fail("收货任务不存在");
        }
        return Result.ok(task);
    }

    @Operation(summary = "扫描商品条码进行收货")
    @PostMapping("/{id}/scan")
    public Result<Map<String, Object>> scan(@PathVariable @NotNull Long id,
                                            @RequestBody @Validated ScanRequest request) {
        if (request.barcode() == null || request.barcode().isBlank()) {
            return Result.fail("条码不能为空");
        }
        log.info("PDA扫码收货: taskId={}, barcode={}", id, request.barcode());

        // 根据条码查找收货明细
        WmsReceiptTask task = receiptService.getTaskById(id);
        if (task == null) {
            return Result.fail("收货任务不存在");
        }

        java.util.List<WmsReceiptDetail> details = receiptService.listByTaskId(id);
        WmsReceiptDetail matchedDetail = null;
        for (WmsReceiptDetail detail : details) {
            if (request.barcode().equals(detail.getProductCode())
                    || request.barcode().equals(detail.getBatchNo())) {
                matchedDetail = detail;
                break;
            }
        }

        if (matchedDetail == null) {
            return Result.fail("未找到匹配的商品条码: " + request.barcode());
        }

        // 记录扫描数量（每次扫码+1）
        java.math.BigDecimal currentReceived = matchedDetail.getReceivedQuantity() != null
                ? matchedDetail.getReceivedQuantity() : java.math.BigDecimal.ZERO;
        matchedDetail.setReceivedQuantity(currentReceived.add(java.math.BigDecimal.ONE));

        // 如果实收数量达到应收数量，标记为已收货
        if (matchedDetail.getReceivedQuantity().compareTo(matchedDetail.getExpectedQuantity()) >= 0) {
            matchedDetail.setStatus(1); // 已收货
        }
        receiptService.updateDetail(matchedDetail);

        // 更新任务总已收数量
        java.math.BigDecimal totalReceived = task.getReceivedQuantity() != null
                ? task.getReceivedQuantity() : java.math.BigDecimal.ZERO;
        task.setReceivedQuantity(totalReceived.add(java.math.BigDecimal.ONE));
        if (task.getStatus() == 0) {
            task.setStatus(1); // 收货中
        }
        receiptService.updateTask(task);

        return Result.ok(java.util.Map.of(
                "barcode", request.barcode(),
                "scanned", true,
                "taskId", id,
                "detailId", matchedDetail.getId(),
                "productName", matchedDetail.getProductName() != null ? matchedDetail.getProductName() : "",
                "receivedQuantity", matchedDetail.getReceivedQuantity(),
                "expectedQuantity", matchedDetail.getExpectedQuantity()
        ));
    }

    @Operation(summary = "确认收货完成")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable @NotNull Long id) {
        receiptService.confirmReceipt(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
        log.info("PDA确认收货: taskId={}", id);
        return Result.ok();
    }

    @Operation(summary = "报告收货异常")
    @PostMapping("/{id}/exception")
    public Result<Void> reportException(@PathVariable @NotNull Long id,
                                        @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "PDA报告异常");
        receiptService.cancelReceipt(id, reason);
        log.info("PDA收货异常: taskId={}, reason={}", id, reason);
        return Result.ok();
    }
}
