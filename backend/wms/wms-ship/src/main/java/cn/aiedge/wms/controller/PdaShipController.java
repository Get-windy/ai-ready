package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.ship.service.ShipService;
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
@RequestMapping("/api/v1/warehouse/ship")
@Tag(name = "PDA-发货")
@RequiredArgsConstructor
public class PdaShipController {

    private final ShipService shipService;

    @Operation(summary = "获取发货任务列表")
    @GetMapping
    public Result<List<WmsShipTask>> list() {
        Page<WmsShipTask> page = shipService.pageTask(
                new Page<>(1, 1000), new WmsShipTask());
        return Result.ok(page.getRecords());
    }

    @Operation(summary = "获取发货任务详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable @NotNull Long id) {
        WmsShipTask task = shipService.getTaskById(id);
        if (task == null) return Result.fail("发货任务不存在");
        List<WmsShipDetail> details = shipService.listByShipId(id);
        return Result.ok(Map.of("task", task, "details", details));
    }

    @Operation(summary = "扫描条码复核")
    @PostMapping("/{id}/scan")
    public Result<Map<String, Object>> scan(@PathVariable @NotNull Long id,
                                            @RequestBody Map<String, Object> body) {
        String barcode = (String) body.get("barcode");
        List<WmsShipDetail> details = shipService.listByShipId(id);
        WmsShipDetail matched = details.stream()
                .filter(d -> barcode.equals(d.getProductCode()))
                .findFirst()
                .orElse(null);
        if (matched != null) {
            BigDecimal qty = body.get("quantity") != null
                    ? new BigDecimal(body.get("quantity").toString())
                    : BigDecimal.ONE;
            shipService.scanItem(matched.getId(), qty);
        }
        log.info("PDA扫描复核: taskId={}, barcode={}, matched={}", id, barcode, matched != null);
        return Result.ok(Map.of("barcode", barcode, "scanned", true, "matched", matched != null));
    }

    @Operation(summary = "确认发货")
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable @NotNull Long id) {
        shipService.confirmShip(id);
        log.info("PDA确认发货: taskId={}", id);
        return Result.ok();
    }
}
