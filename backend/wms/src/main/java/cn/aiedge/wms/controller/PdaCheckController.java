package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.check.service.CheckService;
import cn.aiedge.wms.entity.WmsCheckResult;
import cn.aiedge.wms.entity.WmsCheckTask;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/check")
@Tag(name = "PDA-盘点")
@RequiredArgsConstructor
public class PdaCheckController {

    private final CheckService checkService;

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "PDA操作员";

    @Operation(summary = "获取盘点任务列表")
    @GetMapping
    public Result<List<WmsCheckTask>> list() {
        Page<WmsCheckTask> page = checkService.pageTask(
                new Page<>(1, 1000), new WmsCheckTask());
        return Result.ok(page.getRecords());
    }

    @Operation(summary = "获取盘点任务详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable @NotNull Long id) {
        WmsCheckTask task = checkService.getTaskById(id);
        if (task == null) return Result.fail("盘点任务不存在");
        List<WmsCheckResult> results = checkService.listByTaskId(id);
        Map<String, Object> detail = new HashMap<>();
        detail.put("task", task);
        detail.put("results", results);
        return Result.ok(detail);
    }

    @Operation(summary = "扫描库位条码")
    @PostMapping("/{id}/scan-location")
    public Result<Map<String, Object>> scanLocation(@PathVariable @NotNull Long id,
                                                    @RequestBody Map<String, Object> body) {
        String locationCode = (String) body.get("barcode");
        log.info("PDA扫描库位: taskId={}, locationCode={}", id, locationCode);
        return Result.ok(Map.of("locationCode", locationCode, "scanned", true, "taskId", id));
    }

    @Operation(summary = "扫描商品并录入实际数量")
    @PostMapping("/{id}/scan-product")
    public Result<Map<String, Object>> scanProduct(@PathVariable @NotNull Long id,
                                                   @RequestBody Map<String, Object> body) {
        WmsCheckResult result = new WmsCheckResult();
        result.setTaskId(id);
        result.setProductCode((String) body.get("barcode"));
        if (body.get("quantity") != null) {
            result.setActualQuantity(new BigDecimal(body.get("quantity").toString()));
        }
        if (body.get("locationCode") != null) {
            result.setLocationCode((String) body.get("locationCode"));
        }
        checkService.saveResult(result);
        log.info("PDA盘点录入: taskId={}, barcode={}", id, body.get("barcode"));
        return Result.ok(Map.of("barcode", body.get("barcode"), "recorded", true));
    }

    @Operation(summary = "提交盘点结果")
    @PutMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable @NotNull Long id) {
        checkService.submitResult(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
        log.info("PDA提交盘点: taskId={}", id);
        return Result.ok();
    }
}
