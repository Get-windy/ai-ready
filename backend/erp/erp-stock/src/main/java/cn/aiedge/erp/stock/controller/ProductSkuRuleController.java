package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductSkuRule;
import cn.aiedge.erp.stock.service.ProductSkuRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "SKU生成规则管理")
@RestController
@RequestMapping("/api/erp/product/sku-rules")
@RequiredArgsConstructor
public class ProductSkuRuleController {

    private final ProductSkuRuleService skuRuleService;

    @Operation(summary = "查询SKU规则列表")
    @GetMapping
    public Result<List<ProductSkuRule>> list() {
        return Result.ok(skuRuleService.lambdaQuery().eq(ProductSkuRule::getDeleted, 0).list());
    }

    @Operation(summary = "新增SKU规则")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductSkuRule rule) {
        return Result.ok(skuRuleService.save(rule));
    }

    @Operation(summary = "更新SKU规则")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductSkuRule rule) {
        rule.setId(id);
        return Result.ok(skuRuleService.updateById(rule));
    }

    @Operation(summary = "删除SKU规则")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(skuRuleService.removeById(id));
    }

    @Operation(summary = "设置默认规则")
    @PutMapping("/{id}/set-default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        return Result.ok(skuRuleService.setDefault(id));
    }

    @Operation(summary = "生成SKU")
    @PostMapping("/generate")
    public Result<String> generateSku(@RequestParam Long ruleId, @RequestParam Long productId) {
        return Result.ok(skuRuleService.generateSku(ruleId, productId));
    }
}
