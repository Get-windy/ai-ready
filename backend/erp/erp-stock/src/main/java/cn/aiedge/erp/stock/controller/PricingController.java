package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PriceMemory;
import cn.aiedge.erp.stock.entity.PricingRuleConfig;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.Partner;
import cn.aiedge.erp.stock.mapper.PriceMemoryMapper;
import cn.aiedge.erp.stock.mapper.PricingRuleConfigMapper;
import cn.aiedge.erp.stock.mapper.ProductMapper;
import cn.aiedge.erp.stock.mapper.PartnerMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Tag(name = "取价逻辑引擎")
@RestController
@RequestMapping("/api/erp/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingRuleConfigMapper configMapper;
    private final PriceMemoryMapper priceMemoryMapper;
    private final ProductMapper productMapper;
    private final PartnerMapper partnerMapper;

    @Operation(summary = "查询取价配置")
    @GetMapping("/configs")
    public Result<List<PricingRuleConfig>> getConfigs() {
        return Result.ok(configMapper.selectList(
                new QueryWrapper<PricingRuleConfig>().eq("is_active", 1).eq("deleted", 0)));
    }

    @Operation(summary = "更新取价配置")
    @PutMapping("/configs/{id}")
    public Result<Boolean> updateConfig(@PathVariable Long id, @RequestBody PricingRuleConfig config) {
        config.setId(id);
        return Result.ok(configMapper.updateById(config) > 0);
    }

    @Operation(summary = "解析最终价格(按优先级链路)")
    @GetMapping("/resolve")
    public Result<Map<String, Object>> resolve(
            @RequestParam Long productId,
            @RequestParam Long partnerId,
            @RequestParam(defaultValue = "SALE") String bizType,
            @RequestParam(defaultValue = "1") BigDecimal quantity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", productId);
        result.put("partnerId", partnerId);
        result.put("bizType", bizType);

        Product product = productMapper.selectById(productId);
        Partner partner = partnerMapper.selectById(partnerId);
        if (product == null) {
            return Result.fail("产品不存在");
        }

        List<Map<String, Object>> priceChain = new ArrayList<>();
        BigDecimal finalPrice = null;
        String finalSource = null;

        // 1. 客户特定价
        if (partner != null) {
            // CustomerProductPrice check would go here in real implementation
            // For now, fall through
        }

        // 2. 最近交易价
        PriceMemory latest = priceMemoryMapper.selectLatest(productId, partnerId, bizType);
        if (latest != null) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", "PRICE_MEMORY");
            entry.put("label", "最近交易价");
            entry.put("price", latest.getUnitPrice());
            priceChain.add(entry);
            if (finalPrice == null) {
                finalPrice = latest.getUnitPrice();
                finalSource = "PRICE_MEMORY";
            }
        }

        // 3. 标准售价/采购价
        BigDecimal defaultPrice = "SALE".equals(bizType) ? product.getStandardPrice() : product.getPurchasePrice();
        if (defaultPrice != null && defaultPrice.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", "STANDARD_PRICE");
            entry.put("label", "SALE".equals(bizType) ? "标准售价" : "采购价");
            entry.put("price", defaultPrice);
            priceChain.add(entry);
            if (finalPrice == null) {
                finalPrice = defaultPrice;
                finalSource = "STANDARD_PRICE";
            }
        }

        // 4. 成本价(最后兜底)
        if (product.getCostPrice() != null && product.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("source", "COST_PRICE");
            entry.put("label", "成本价");
            entry.put("price", product.getCostPrice());
            priceChain.add(entry);
            if (finalPrice == null) {
                finalPrice = product.getCostPrice();
                finalSource = "COST_PRICE";
            }
        }

        result.put("priceChain", priceChain);
        result.put("finalPrice", finalPrice);
        result.put("finalSource", finalSource);
        return Result.ok(result);
    }

    @Operation(summary = "分页查询价格记忆")
    @GetMapping("/price-memory/page")
    public Result<IPage<PriceMemory>> priceMemoryPage(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        QueryWrapper<PriceMemory> wrapper = new QueryWrapper<PriceMemory>().eq("deleted", 0);
        if (productId != null) wrapper.eq("product_id", productId);
        if (partnerId != null) wrapper.eq("partner_id", partnerId);
        wrapper.orderByDesc("create_time");
        return Result.ok(priceMemoryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "记录交易价格")
    @PostMapping("/price-memory")
    public Result<Boolean> recordPrice(@RequestBody PriceMemory priceMemory) {
        // 取消旧的最新标记
        priceMemoryMapper.update(null, new QueryWrapper<PriceMemory>()
                .eq("product_id", priceMemory.getProductId())
                .eq("partner_id", priceMemory.getPartnerId())
                .eq("biz_type", priceMemory.getBizType())
                .eq("is_latest", 1)
                .set("is_latest", 0));
        priceMemory.setIsLatest(1);
        return Result.ok(priceMemoryMapper.insert(priceMemory) > 0);
    }

    @Operation(summary = "查询最新交易价格")
    @GetMapping("/price-memory/latest/{productId}/{partnerId}")
    public Result<PriceMemory> getLatestPrice(
            @PathVariable Long productId,
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "SALE") String bizType) {
        return Result.ok(priceMemoryMapper.selectLatest(productId, partnerId, bizType));
    }
}
