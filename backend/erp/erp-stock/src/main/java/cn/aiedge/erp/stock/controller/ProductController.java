package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.service.ProductService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品Controller - 产品列表与详情管理
 */
@Slf4j
@Tag(name = "产品管理")
@RestController
@RequestMapping("/api/erp/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "查询产品列表")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.ok(productService.getProductList());
    }

    @Operation(summary = "分页查询产品")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/page")
    public Result<IPage<Product>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productService.getProductPage(categoryId, keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "获取产品详情(含等级价格)")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @Operation(summary = "按编码查询")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/by-code/{productCode}")
    public Result<Product> getByCode(@PathVariable String productCode) {
        Product product = productService.lambdaQuery()
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0)
                .one();
        return Result.ok(product);
    }

    @Operation(summary = "新增产品")
    @SaCheckPermission("erp:product:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody Product product) {
        return Result.ok(productService.createProduct(product));
    }

    @Operation(summary = "编辑产品")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return Result.ok(productService.updateProduct(product));
    }

    @Operation(summary = "更新产品状态")
    @SaCheckPermission("erp:product:status")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(productService.updateProductStatus(id, status));
    }

    @Operation(summary = "删除产品")
    @SaCheckPermission("erp:product:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productService.removeById(id));
    }

    @Operation(summary = "批量更新产品价格")
    @SaCheckPermission("erp:product:price-batch")
    @PutMapping("/batch-prices")
    public Result<Boolean> batchUpdatePrices(@RequestBody BatchPriceUpdateDTO dto) {
        return Result.ok(productService.batchUpdatePrices(dto.getItems()));
    }

    @Operation(summary = "导出产品列表")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/export")
    public Result<List<Product>> export(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(productService.exportList(categoryId, keyword, status));
    }

    @Operation(summary = "产品审批")
    @SaCheckPermission("erp:product:approval")
    @PutMapping("/{id}/approval")
    public Result<Boolean> approval(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String action = body.get("action");
        if (action == null) return Result.fail("审批操作不能为空");
        Product product = new Product();
        product.setId(id);
        product.setApprovalStatus(action);
        return Result.ok(productService.updateById(product));
    }
}
