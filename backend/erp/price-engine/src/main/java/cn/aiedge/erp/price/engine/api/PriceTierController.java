package cn.aiedge.erp.price.engine.api;

import cn.aiedge.erp.price.engine.strategy.calculator.TieredPricingCalculator;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 层级定价管理控制器
 * 管理客户价格层级、批量定价和客户专属价格
 */
@RestController
@RequestMapping("/api/v1/price-engine/tiers")
@Tag(name = "层级定价", description = "多级价格体系管理：客户等级定价、数量阶梯、批量定价")
public class PriceTierController {

    private final TieredPricingCalculator tieredPricingCalculator;

    // 内存存储（演示用，正式环境应使用数据库）
    private final Map<String, PriceTier> tierStore = new HashMap<>();

    public PriceTierController(TieredPricingCalculator tieredPricingCalculator) {
        this.tieredPricingCalculator = tieredPricingCalculator;
        initDefaultTiers();
    }

    private void initDefaultTiers() {
        addTier("TIER_STRATEGIC", "战略客户价", "strategic", null, "strategic", null, null,
                null, BigDecimal.valueOf(0.85), "factor");
        addTier("TIER_CORE", "核心客户价", "core", null, "core", null, null,
                null, BigDecimal.valueOf(0.92), "factor");
        addTier("TIER_NORMAL", "普通客户价", "normal", null, "normal", null, null,
                null, BigDecimal.valueOf(0.97), "factor");
        addTier("TIER_NEW", "新客户价", "new", null, "new", null, null,
                null, BigDecimal.valueOf(0.98), "factor");

        // 数量阶梯
        addTier("TIER_QTY_100", "批量100+", null, null, null, 100, 499,
                BigDecimal.valueOf(0.03), null, "discount");
        addTier("TIER_QTY_500", "批量500+", null, null, null, 500, 999,
                BigDecimal.valueOf(0.05), null, "discount");
        addTier("TIER_QTY_1000", "批量1000+", null, null, null, 1000, null,
                BigDecimal.valueOf(0.08), null, "discount");
    }

    private void addTier(String id, String name, String code, String productId,
                         String customerLevel, Integer minQty, Integer maxQty,
                         BigDecimal discountRate, BigDecimal priceFactor, String mode) {
        PriceTier tier = new PriceTier();
        tier.setTierId(id);
        tier.setTierName(name);
        tier.setTierCode(code);
        tier.setProductId(productId);
        tier.setCustomerLevel(customerLevel);
        tier.setMinQuantity(minQty);
        tier.setMaxQuantity(maxQty);
        tier.setDiscountRate(discountRate);
        tier.setPriceFactor(priceFactor);
        tier.setPricingMode(mode != null ? mode : "discount");
        tier.setStatus("active");
        tier.setPriority(tierStore.size() + 1);
        tierStore.put(id, tier);
    }

    // ==================== Controllers ====================

    @Operation(summary = "计算层级价格", description = "根据客户等级和数量计算产品的层级价格")
    @PostMapping("/calculate")
    public ApiResponse<TieredPricingCalculator.TieredPriceResult> calculateTierPrice(
            @Parameter(description = "价格计算请求") @RequestBody PriceCalculationRequest request) {
        try {
            List<PriceTier> allTiers = new ArrayList<>(tierStore.values());
            List<PriceTier> productTiers = tieredPricingCalculator
                    .getAvailableTiersForProduct(allTiers, request.getProductId(), request.getCustomerLevel());
            TieredPricingCalculator.TieredPriceResult result =
                    tieredPricingCalculator.calculateBestTierPrice(productTiers, request);
            if (result == null) {
                // 没有适用价层时返回基准价
                result = new TieredPricingCalculator.TieredPriceResult();
                result.setOriginalBasePrice(request.getEffectiveBasePrice());
                result.setTierPrice(request.getEffectiveBasePrice());
                result.setDiscountAmount(BigDecimal.ZERO);
                result.setDiscountRate(BigDecimal.ZERO);
            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("层级价格计算失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取客户价格", description = "获取客户专属价格（含所有适用价层）")
    @GetMapping("/customer-price")
    public ApiResponse<List<PriceTier>> getCustomerTierPrices(
            @Parameter(description = "客户ID") @RequestParam String customerId,
            @Parameter(description = "客户等级") @RequestParam(defaultValue = "normal") String customerLevel,
            @Parameter(description = "产品ID") @RequestParam(required = false) String productId) {
        try {
            List<PriceTier> allTiers = new ArrayList<>(tierStore.values());
            List<PriceTier> applicable = allTiers.stream()
                    .filter(t -> "active".equals(t.getStatus()))
                    .filter(t -> customerLevel == null || t.isCustomerLevelApplicable(customerLevel))
                    .filter(t -> productId == null || productId.equals(t.getProductId())
                            || t.getProductId() == null) // 全局价层也适用
                    .sorted(Comparator.comparingInt(PriceTier::getPriority))
                    .collect(Collectors.toList());
            return ApiResponse.success(applicable);
        } catch (Exception e) {
            return ApiResponse.error("获取客户价格失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建价层", description = "创建新的价格层级")
    @PostMapping("/tier")
    public ApiResponse<PriceTier> createTier(@RequestBody PriceTier tier) {
        try {
            String id = "TIER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            tier.setTierId(id);
            tier.setStatus("active");
            tierStore.put(id, tier);
            return ApiResponse.success(tier);
        } catch (Exception e) {
            return ApiResponse.error("创建价层失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新价层", description = "更新价格层级信息")
    @PutMapping("/tier/{tierId}")
    public ApiResponse<PriceTier> updateTier(
            @Parameter(description = "价层ID") @PathVariable String tierId,
            @RequestBody PriceTier tier) {
        try {
            if (!tierStore.containsKey(tierId)) {
                return ApiResponse.error("价层不存在: " + tierId);
            }
            tier.setTierId(tierId);
            tierStore.put(tierId, tier);
            return ApiResponse.success(tier);
        } catch (Exception e) {
            return ApiResponse.error("更新价层失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除价层", description = "删除指定的价格层级")
    @DeleteMapping("/tier/{tierId}")
    public ApiResponse<Void> deleteTier(
            @Parameter(description = "价层ID") @PathVariable String tierId) {
        try {
            tierStore.remove(tierId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error("删除价层失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取所有价层", description = "获取全部价格层级列表")
    @GetMapping("/tiers")
    public ApiResponse<List<PriceTier>> getAllTiers() {
        try {
            List<PriceTier> tiers = new ArrayList<>(tierStore.values());
            tiers.sort(Comparator.comparingInt(PriceTier::getPriority));
            return ApiResponse.success(tiers);
        } catch (Exception e) {
            return ApiResponse.error("获取价层列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量查询层级价格", description = "批量查询多个产品的层级价格")
    @PostMapping("/calculate/batch")
    public ApiResponse<List<TieredPricingCalculator.TieredPriceResult>> calculateBatchTierPrices(
            @RequestBody List<PriceCalculationRequest> requests) {
        try {
            List<PriceTier> allTiers = new ArrayList<>(tierStore.values());
            Map<String, List<PriceTier>> productTiersMap = new HashMap<>();
            for (PriceCalculationRequest req : requests) {
                productTiersMap.put(req.getProductId(),
                        tieredPricingCalculator.getAvailableTiersForProduct(allTiers, req.getProductId(), req.getCustomerLevel()));
            }
            List<TieredPricingCalculator.TieredPriceResult> results =
                    tieredPricingCalculator.calculateBatchTierPrices(productTiersMap, requests);
            return ApiResponse.success(results);
        } catch (Exception e) {
            return ApiResponse.error("批量查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取产品可用价层", description = "获取指定产品的所有可用价格层级")
    @GetMapping("/product/{productId}")
    public ApiResponse<List<PriceTier>> getProductTiers(
            @Parameter(description = "产品ID") @PathVariable String productId,
            @Parameter(description = "客户等级") @RequestParam(required = false) String customerLevel) {
        try {
            List<PriceTier> allTiers = new ArrayList<>(tierStore.values());
            List<PriceTier> productTiers = tieredPricingCalculator
                    .getAvailableTiersForProduct(allTiers, productId, customerLevel);
            return ApiResponse.success(productTiers);
        } catch (Exception e) {
            return ApiResponse.error("获取产品价层失败: " + e.getMessage());
        }
    }

    /**
     * 内部ApiResponse记录（与PriceEngineController一致）
     */
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
