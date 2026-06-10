package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.StockAlertConfig;
import cn.aiedge.erp.stock.service.StockAlertConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock-alert-config")
@RequiredArgsConstructor
@Tag(name = "库存预警配置管理", description = "库存预警阈值配置、预警检查等操作")
public class StockAlertConfigController {

    private final StockAlertConfigService stockAlertConfigService;

    @GetMapping("/page")
    @Operation(summary = "分页查询预警配置")
    public Result<Page<StockAlertConfig>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "是否激活") @RequestParam(required = false) Boolean active,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(stockAlertConfigService.pageList(keyword, warehouseId, active, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取预警配置详情")
    public Result<StockAlertConfig> getById(@PathVariable Long id) {
        return Result.ok(stockAlertConfigService.getById(id));
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "根据产品和仓库获取预警配置")
    public Result<StockAlertConfig> getByProductAndWarehouse(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return Result.ok(stockAlertConfigService.getByProductAndWarehouse(productId, warehouseId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "获取仓库预警配置列表")
    public Result<List<StockAlertConfig>> listByWarehouse(@PathVariable Long warehouseId) {
        return Result.ok(stockAlertConfigService.listByWarehouse(warehouseId));
    }

    @GetMapping("/active")
    @Operation(summary = "获取所有激活的预警配置")
    public Result<List<StockAlertConfig>> listAllActive() {
        return Result.ok(stockAlertConfigService.listAllActive());
    }

    @PostMapping
    @Operation(summary = "创建预警配置")
    public Result<StockAlertConfig> create(@RequestBody StockAlertConfig config) {
        config.setTenantId(1L);
        return Result.ok(stockAlertConfigService.createConfig(config));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新预警配置")
    public Result<StockAlertConfig> update(@PathVariable Long id, @RequestBody StockAlertConfig config) {
        return Result.ok(stockAlertConfigService.updateConfig(id, config));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活预警配置")
    public Result<Void> activate(@PathVariable Long id) {
        stockAlertConfigService.activateConfig(id);
        return Result.ok();
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用预警配置")
    public Result<Void> deactivate(@PathVariable Long id) {
        stockAlertConfigService.deactivateConfig(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除预警配置")
    public Result<Void> delete(@PathVariable Long id) {
        stockAlertConfigService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/check")
    @Operation(summary = "检查库存预警")
    public Result<List<StockAlertConfig>> checkAlerts() {
        return Result.ok(stockAlertConfigService.checkAlerts());
    }

    @GetMapping("/statistics")
    @Operation(summary = "预警配置统计")
    public Result<java.util.Map<String, Object>> statistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalConfigs", stockAlertConfigService.lambdaQuery()
                .eq(StockAlertConfig::getDeleted, 0)
                .count());
        stats.put("activeConfigs", stockAlertConfigService.countActive(1L));
        return Result.ok(stats);
    }
}