package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.service.SysConfigService;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.enums.InventoryMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存管理模式控制器
 * <p>
 * 支持租户级配置：BATCH（批次）/ SERIAL（序列号）/ SKU
 */
@Slf4j
@Tag(name = "库存管理模式")
@RestController
@RequestMapping("/api/erp/inventory-mode")
@RequiredArgsConstructor
public class InventoryModeController {

    private static final String CONFIG_KEY = "inventory.mode";

    private final SysConfigService configService;

    @Operation(summary = "获取当前租户的库存管理模式")
    @GetMapping
    public Result<InventoryMode> getMode() {
        String value = configService.getValue(CONFIG_KEY, "BATCH");
        return Result.ok(InventoryMode.fromString(value));
    }

    @Operation(summary = "设置库存管理模式")
    @PutMapping
    public Result<Void> setMode(@RequestBody InventoryModeRequest request) {
        configService.setValue(CONFIG_KEY, request.getMode().name(), "string", "inventory", "库存管理模式");
        return Result.ok();
    }

    @Operation(summary = "获取所有支持的库存管理模式")
    @GetMapping("/options")
    public Result<List<ModeOption>> getOptions() {
        return Result.ok(List.of(
                new ModeOption("BATCH", "批次管理", "按批次追踪，含生产日期和有效期"),
                new ModeOption("SERIAL", "序列号管理", "一物一码，每个产品独立序列号追踪"),
                new ModeOption("SKU", "SKU管理", "按库存单位编码管理，最简单模式")
        ));
    }

    @lombok.Data
    public static class InventoryModeRequest {
        private InventoryMode mode;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ModeOption {
        private String value;
        private String label;
        private String description;
    }
}
