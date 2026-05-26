package cn.aiedge.erp.sales.pricing.controller;

import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest;
import cn.aiedge.erp.sales.pricing.dto.PriceCalculationResult;
import cn.aiedge.erp.sales.pricing.dto.PriceStrategyDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.service.IPriceCalculationService;
import cn.aiedge.erp.sales.pricing.service.IPriceStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 价格策略管理控制器
 */
@Tag(name = "价格策略管理", description = "销售价格策略管理API")
@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Validated
public class PricingController {
    
    private final IPriceStrategyService priceStrategyService;
    private final IPriceCalculationService priceCalculationService;
    
    @Operation(summary = "创建价格策略", description = "创建一个新的价格策略")
    @PostMapping("/strategies")
    public ResponseEntity<PriceStrategy> createStrategy(@Valid @RequestBody PriceStrategyDTO strategyDTO) {
        PriceStrategy strategy = priceStrategyService.createStrategy(strategyDTO);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "更新价格策略", description = "更新指定的价格策略")
    @PutMapping("/strategies/{id}")
    public ResponseEntity<PriceStrategy> updateStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id,
            @Valid @RequestBody PriceStrategyDTO strategyDTO) {
        PriceStrategy strategy = priceStrategyService.updateStrategy(id, strategyDTO);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "删除价格策略", description = "逻辑删除指定的价格策略")
    @DeleteMapping("/strategies/{id}")
    public ResponseEntity<Void> deleteStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        priceStrategyService.deleteStrategy(id);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "获取价格策略详情", description = "根据ID获取价格策略详情")
    @GetMapping("/strategies/{id}")
    public ResponseEntity<PriceStrategy> getStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        PriceStrategy strategy = priceStrategyService.getStrategy(id);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "分页查询价格策略", description = "分页查询价格策略列表")
    @GetMapping("/strategies")
    public ResponseEntity<Page<PriceStrategy>> listStrategies(Pageable pageable) {
        Page<PriceStrategy> strategies = priceStrategyService.listStrategies(pageable);
        return ResponseEntity.ok(strategies);
    }
    
    @Operation(summary = "查询租户价格策略", description = "查询指定租户的所有价格策略")
    @GetMapping("/strategies/tenant/{tenantId}")
    public ResponseEntity<List<PriceStrategy>> listStrategiesByTenant(
            @Parameter(description = "租户ID") @PathVariable Long tenantId) {
        List<PriceStrategy> strategies = priceStrategyService.listStrategiesByTenant(tenantId);
        return ResponseEntity.ok(strategies);
    }
    
    @Operation(summary = "查询客户等级价格策略", description = "查询适用于指定客户等级的价格策略")
    @GetMapping("/strategies/customer-level/{customerLevel}")
    public ResponseEntity<List<PriceStrategy>> listStrategiesByCustomerLevel(
            @Parameter(description = "客户等级") @PathVariable String customerLevel) {
        List<PriceStrategy> strategies = priceStrategyService.listStrategiesByCustomerLevel(customerLevel);
        return ResponseEntity.ok(strategies);
    }
    
    @Operation(summary = "查询产品类别价格策略", description = "查询适用于指定产品类别的价格策略")
    @GetMapping("/strategies/product-category/{categoryId}")
    public ResponseEntity<List<PriceStrategy>> listStrategiesByProductCategory(
            @Parameter(description = "产品类别ID") @PathVariable Long categoryId) {
        List<PriceStrategy> strategies = priceStrategyService.listStrategiesByProductCategory(categoryId);
        return ResponseEntity.ok(strategies);
    }
    
    @Operation(summary = "激活价格策略", description = "激活指定的价格策略")
    @PostMapping("/strategies/{id}/activate")
    public ResponseEntity<PriceStrategy> activateStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        PriceStrategy strategy = priceStrategyService.activateStrategy(id);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "停用价格策略", description = "停用指定的价格策略")
    @PostMapping("/strategies/{id}/deactivate")
    public ResponseEntity<PriceStrategy> deactivateStrategy(
            @Parameter(description = "策略ID") @PathVariable Long id) {
        PriceStrategy strategy = priceStrategyService.deactivateStrategy(id);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "复制价格策略", description = "复制指定的价格策略")
    @PostMapping("/strategies/{id}/copy")
    public ResponseEntity<PriceStrategy> copyStrategy(
            @Parameter(description = "源策略ID") @PathVariable Long id,
            @RequestParam String newStrategyName) {
        PriceStrategy strategy = priceStrategyService.copyStrategy(id, newStrategyName);
        return ResponseEntity.ok(strategy);
    }
    
    @Operation(summary = "计算商品价格", description = "根据价格策略计算商品价格")
    @PostMapping("/calculate")
    public ResponseEntity<PriceCalculationResult> calculatePrice(@Valid @RequestBody PriceCalculationRequest request) {
        PriceCalculationResult result = priceCalculationService.calculatePrice(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "批量计算商品价格", description = "批量计算商品价格")
    @PostMapping("/calculate/batch")
    public ResponseEntity<PriceCalculationResult> calculateBatchPrice(@Valid @RequestBody PriceCalculationRequest request) {
        PriceCalculationResult result = priceCalculationService.calculateBatchPrice(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "验证策略适用性", description = "验证价格策略是否适用于指定场景")
    @PostMapping("/strategies/{strategyId}/validate")
    public ResponseEntity<Boolean> validateStrategyApplicability(
            @Parameter(description = "策略ID") @PathVariable Long strategyId,
            @Valid @RequestBody PriceCalculationRequest request) {
        boolean applicable = priceCalculationService.validateStrategyApplicability(request, strategyId);
        return ResponseEntity.ok(applicable);
    }
    
    @Operation(summary = "获取计算历史", description = "根据计算ID获取价格计算历史")
    @GetMapping("/calculations/{calculationId}")
    public ResponseEntity<PriceCalculationResult> getCalculationHistory(
            @Parameter(description = "计算ID") @PathVariable String calculationId) {
        PriceCalculationResult result = priceCalculationService.getCalculationHistory(calculationId);
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "模拟价格计算", description = "模拟价格计算，不保存历史")
    @PostMapping("/calculate/simulate")
    public ResponseEntity<PriceCalculationResult> simulatePriceCalculation(@Valid @RequestBody PriceCalculationRequest request) {
        PriceCalculationResult result = priceCalculationService.simulatePriceCalculation(request);
        return ResponseEntity.ok(result);
    }
    
    @Operation(summary = "清除价格缓存", description = "清除价格计算缓存")
    @PostMapping("/cache/clear")
    public ResponseEntity<Void> clearPriceCache() {
        priceCalculationService.clearPriceCache();
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "刷新规则缓存", description = "刷新Drools规则引擎缓存")
    @PostMapping("/rules/refresh")
    public ResponseEntity<Void> refreshRulesCache() {
        priceCalculationService.refreshRulesCache();
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "获取计算统计", description = "获取价格计算统计信息")
    @GetMapping("/statistics")
    public ResponseEntity<IPriceCalculationService.PriceCalculationStats> getCalculationStats() {
        IPriceCalculationService.PriceCalculationStats stats = priceCalculationService.getCalculationStats();
        return ResponseEntity.ok(stats);
    }
}