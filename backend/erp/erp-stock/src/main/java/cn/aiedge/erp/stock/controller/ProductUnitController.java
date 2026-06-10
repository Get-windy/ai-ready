package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.service.ProductUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "产品多单位管理")
@RestController
@RequestMapping("/api/erp/product/units")
@RequiredArgsConstructor
public class ProductUnitController {

    private final ProductUnitService productUnitService;

    @Operation(summary = "查询产品单位列表")
    @GetMapping("/{productId}")
    public Result<List<ProductUnit>> getByProduct(@PathVariable Long productId) {
        return Result.ok(productUnitService.getByProductId(productId));
    }

    @Operation(summary = "新增产品单位")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductUnit unit) {
        return Result.ok(productUnitService.save(unit));
    }

    @Operation(summary = "更新产品单位")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductUnit unit) {
        unit.setId(id);
        return Result.ok(productUnitService.updateById(unit));
    }

    @Operation(summary = "删除产品单位")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productUnitService.removeById(id));
    }
}
