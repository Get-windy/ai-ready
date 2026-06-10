package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductBarcode;
import cn.aiedge.erp.stock.service.ProductBarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "产品条形码管理")
@RestController
@RequestMapping("/api/erp/product/barcodes")
@RequiredArgsConstructor
public class ProductBarcodeController {

    private final ProductBarcodeService productBarcodeService;

    @Operation(summary = "查询产品条形码列表")
    @GetMapping("/{productId}")
    public Result<List<ProductBarcode>> getByProduct(@PathVariable Long productId) {
        return Result.ok(productBarcodeService.getByProductId(productId));
    }

    @Operation(summary = "新增条形码")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductBarcode barcode) {
        return Result.ok(productBarcodeService.save(barcode));
    }

    @Operation(summary = "更新条形码")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductBarcode barcode) {
        barcode.setId(id);
        return Result.ok(productBarcodeService.updateById(barcode));
    }

    @Operation(summary = "删除条形码")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productBarcodeService.removeById(id));
    }
}
