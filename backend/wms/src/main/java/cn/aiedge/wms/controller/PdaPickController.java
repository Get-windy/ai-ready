package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsPickTask;
import cn.aiedge.wms.pick.service.PickService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/pick")
@Tag(name = "PDA-拣货")
@RequiredArgsConstructor
public class PdaPickController {

    private final PickService pickService;

    @Operation(summary = "获取拣货任务列表")
    @GetMapping
    public Result<List<WmsPickTask>> list() {
        Page<WmsPickTask> page = pickService.pageTask(
                new Page<>(1, 1000), new WmsPickTask());
        return Result.ok(page.getRecords());
    }

    @Operation(summary = "获取拣货任务详情")
    @GetMapping("/{id}")
    public Result<WmsPickTask> detail(@PathVariable @NotNull Long id) {
        WmsPickTask task = pickService.getTaskById(id);
        if (task == null) return Result.fail("拣货任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "扫描商品验证")
    @PostMapping("/{id}/verify")
    public Result<Map<String, Object>> verify(@PathVariable @NotNull Long id,
                                              @RequestBody Map<String, Object> body) {
        String barcode = (String) body.get("barcode");
        log.info("PDA扫描验证: taskId={}, barcode={}", id, barcode);
        return Result.ok(Map.of("barcode", barcode, "verified", true, "taskId", id));
    }

    @Operation(summary = "确认拣货数量")
    @PutMapping("/item/{itemId}")
    public Result<Void> confirmItem(@PathVariable @NotNull Long itemId,
                                    @RequestBody Map<String, Object> body) {
        BigDecimal quantity = new BigDecimal(body.getOrDefault("quantity", "0").toString());
        pickService.confirmPickItem(itemId, quantity);
        log.info("PDA确认拣货: itemId={}, qty={}", itemId, quantity);
        return Result.ok();
    }

    @Operation(summary = "完成拣货")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable @NotNull Long id) {
        pickService.completePick(id);
        log.info("PDA完成拣货: taskId={}", id);
        return Result.ok();
    }

    @Operation(summary = "标记拣货短缺")
    @PostMapping("/{id}/shortage")
    public Result<Void> shortage(@PathVariable @NotNull Long id,
                                 @RequestBody Map<String, Object> body) {
        Long detailId = Long.valueOf(body.get("detailId").toString());
        BigDecimal quantity = new BigDecimal(body.getOrDefault("quantity", "0").toString());
        pickService.markShortage(detailId, quantity);
        log.info("PDA标记缺货: detailId={}, qty={}", detailId, quantity);
        return Result.ok();
    }
}
