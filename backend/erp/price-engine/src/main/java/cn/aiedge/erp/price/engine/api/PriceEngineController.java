package cn.aiedge.erp.price.engine.api;

import cn.aiedge.erp.price.engine.service.PriceEngineService;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/price-engine")
@Tag(name = "价格引擎", description = "价格计算、优化和管理接口")
public class PriceEngineController {
    
    private final PriceEngineService priceEngineService;
    
    public PriceEngineController(PriceEngineService priceEngineService) {
        this.priceEngineService = priceEngineService;
    }
    
    @PostMapping("/calculate")
    @Operation(summary = "计算价格", description = "根据请求参数计算产品价格")
    public ApiResponse<PriceCalculationResult> calculatePrice(
            @Parameter(description = "价格计算请求", required = true)
            @RequestBody PriceCalculationRequest request) {
        try {
            PriceCalculationResult result = priceEngineService.calculatePrice(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("价格计算失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/calculate/batch")
    @Operation(summary = "批量计算价格", description = "批量计算多个产品的价格")
    public ApiResponse<List<PriceCalculationResult>> calculateBatchPrices(
            @Parameter(description = "价格计算请求列表", required = true)
            @RequestBody List<PriceCalculationRequest> requests) {
        try {
            List<PriceCalculationResult> results = priceEngineService.calculateBatchPrices(requests);
            return ApiResponse.success(results);
        } catch (Exception e) {
            return ApiResponse.error("批量计算失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/simulate")
    @Operation(summary = "模拟价格", description = "模拟价格策略效果")
    public ApiResponse<PriceCalculationResult> simulatePrice(
            @Parameter(description = "价格计算请求", required = true)
            @RequestBody PriceCalculationRequest request,
            @Parameter(description = "策略ID")
            @RequestParam(required = false) String strategyId) {
        try {
            PriceCalculationResult result = priceEngineService.simulatePrice(request, strategyId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("模拟计算失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/optimize")
    @Operation(summary = "获取最优价格", description = "根据优化算法获取最优价格建议")
    public ApiResponse<PriceCalculationResult> getOptimalPrice(
            @Parameter(description = "价格计算请求", required = true)
            @RequestBody PriceCalculationRequest request) {
        try {
            PriceCalculationResult result = priceEngineService.getOptimalPrice(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("优化计算失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/history/{productId}")
    @Operation(summary = "获取价格历史", description = "获取产品的价格计算历史")
    public ApiResponse<Map<String, Object>> getPriceHistory(
            @Parameter(description = "产品ID", required = true)
            @PathVariable String productId,
            @Parameter(description = "客户ID")
            @RequestParam(required = false) String customerId,
            @Parameter(description = "天数")
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> history = priceEngineService.getPriceHistory(productId, customerId, days);
            return ApiResponse.success(history);
        } catch (Exception e) {
            return ApiResponse.error("获取历史失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/statistics/{productId}")
    @Operation(summary = "获取价格统计", description = "获取产品的价格统计信息")
    public ApiResponse<Map<String, Object>> getPriceStatistics(
            @Parameter(description = "产品ID", required = true)
            @PathVariable String productId) {
        try {
            Map<String, Object> stats = priceEngineService.getPriceStatistics(productId);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            return ApiResponse.error("获取统计失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/strategies/applicable")
    @Operation(summary = "获取适用策略", description = "获取适用于当前请求的价格策略")
    public ApiResponse<List<String>> getApplicableStrategies(
            @Parameter(description = "价格计算请求", required = true)
            @RequestBody PriceCalculationRequest request) {
        try {
            List<String> strategies = priceEngineService.getApplicableStrategies(request);
            return ApiResponse.success(strategies);
        } catch (Exception e) {
            return ApiResponse.error("获取策略失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/validate/{strategyId}")
    @Operation(summary = "验证价格配置", description = "验证价格策略配置的有效性")
    public ApiResponse<Boolean> validatePriceConfiguration(
            @Parameter(description = "策略ID", required = true)
            @PathVariable String strategyId) {
        try {
            boolean valid = priceEngineService.validatePriceConfiguration(strategyId);
            return ApiResponse.success(valid);
        } catch (Exception e) {
            return ApiResponse.error("验证失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/cache/refresh")
    @Operation(summary = "刷新价格缓存", description = "清除并刷新价格计算缓存")
    public ApiResponse<Void> refreshPriceCache() {
        try {
            priceEngineService.refreshPriceCache();
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error("刷新缓存失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "引擎健康检查", description = "检查价格引擎的健康状态")
    public ApiResponse<Map<String, Object>> getEngineHealth() {
        try {
            Map<String, Object> health = priceEngineService.getEngineHealth();
            return ApiResponse.success(health);
        } catch (Exception e) {
            return ApiResponse.error("健康检查失败: " + e.getMessage());
        }
    }
    
    public record ApiResponse<T>(
            boolean success,
            String message,
            T data,
            long timestamp
    ) {
        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(true, "成功", data, System.currentTimeMillis());
        }
        
        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null, System.currentTimeMillis());
        }
    }
}