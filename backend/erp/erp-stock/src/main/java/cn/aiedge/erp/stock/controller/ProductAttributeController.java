package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductAttributeDef;
import cn.aiedge.erp.stock.entity.ProductAttributeOption;
import cn.aiedge.erp.stock.entity.ProductAttributeValue;
import cn.aiedge.erp.stock.service.ProductAttributeDefService;
import cn.aiedge.erp.stock.service.ProductAttributeValueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "产品属性管理")
@RestController
@RequestMapping("/api/erp/product/attributes")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductAttributeDefService attributeDefService;
    private final ProductAttributeValueService attributeValueService;

    @Operation(summary = "查询属性定义列表")
    @GetMapping("/defs")
    public Result<List<ProductAttributeDef>> getDefs() {
        return Result.ok(attributeDefService.lambdaQuery().eq(ProductAttributeDef::getDeleted, 0)
                .orderByAsc(ProductAttributeDef::getSortOrder).list());
    }

    @Operation(summary = "新增属性定义")
    @PostMapping("/defs")
    public Result<Boolean> createDef(@RequestBody ProductAttributeDef def) {
        return Result.ok(attributeDefService.save(def));
    }

    @Operation(summary = "更新属性定义")
    @PutMapping("/defs/{id}")
    public Result<Boolean> updateDef(@PathVariable Long id, @RequestBody ProductAttributeDef def) {
        def.setId(id);
        return Result.ok(attributeDefService.updateById(def));
    }

    @Operation(summary = "删除属性定义")
    @DeleteMapping("/defs/{id}")
    public Result<Boolean> deleteDef(@PathVariable Long id) {
        return Result.ok(attributeDefService.removeById(id));
    }

    @Operation(summary = "查询属性选项")
    @GetMapping("/defs/{defId}/options")
    public Result<List<ProductAttributeOption>> getOptions(@PathVariable Long defId) {
        return Result.ok(attributeDefService.getOptions(defId));
    }

    @Operation(summary = "新增属性选项")
    @PostMapping("/options")
    public Result<Boolean> createOption(@RequestBody ProductAttributeOption option) {
        return Result.ok(attributeDefService.saveOption(option));
    }

    @Operation(summary = "更新属性选项")
    @PutMapping("/options/{id}")
    public Result<Boolean> updateOption(@PathVariable Long id, @RequestBody ProductAttributeOption option) {
        option.setId(id);
        return Result.ok(attributeDefService.updateOption(option));
    }

    @Operation(summary = "删除属性选项")
    @DeleteMapping("/options/{id}")
    public Result<Boolean> deleteOption(@PathVariable Long id) {
        return Result.ok(attributeDefService.removeOption(id));
    }

    @Operation(summary = "查询产品属性值")
    @GetMapping("/values/{productId}")
    public Result<List<ProductAttributeValue>> getValues(@PathVariable Long productId) {
        return Result.ok(attributeValueService.getByProductId(productId));
    }

    @Operation(summary = "批量保存产品属性值")
    @PutMapping("/values/{productId}")
    public Result<Boolean> saveValues(@PathVariable Long productId, @RequestBody List<ProductAttributeValue> values) {
        return Result.ok(attributeValueService.batchSave(productId, values));
    }
}
