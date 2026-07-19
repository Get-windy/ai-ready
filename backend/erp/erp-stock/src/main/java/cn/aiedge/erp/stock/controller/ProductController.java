package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO;
import cn.aiedge.erp.stock.dto.BatchStatusUpdateDTO;
import cn.aiedge.erp.stock.dto.ProductFormDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductRecommend;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.service.ProductRecommendService;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final ProductUnitService productUnitService;
    private final ProductRecommendService productRecommendService;

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
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) Integer useCoupon,
            @RequestParam(required = false) Integer isStandardProduct,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Integer mallShelfStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productService.getProductPage(categoryId, keyword, status, brand, industryCategory,
                createTimeStart, createTimeEnd, useCoupon, isStandardProduct, productType, mallShelfStatus,
                pageNum, pageSize));
    }

    @Operation(summary = "获取产品详情(含等级价格)")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @Operation(summary = "获取产品表单数据(含单位+推荐)")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/{id}/form")
    public Result<ProductFormDTO> getFormById(@PathVariable Long id) {
        Product product = productService.getProductDetail(id);
        if (product == null) {
            return Result.fail("商品不存在或已删除");
        }
        ProductFormDTO dto = new ProductFormDTO();
        dto.setProduct(product);
        dto.setUnits(productUnitService.getByProductId(id));
        dto.setRecommends(productRecommendService.getByProductId(id));
        return Result.ok(dto);
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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) Integer useCoupon,
            @RequestParam(required = false) Integer isStandardProduct) {
        return Result.ok(productService.exportList(categoryId, keyword, status,
                brand, industryCategory, createTimeStart, createTimeEnd,
                useCoupon, isStandardProduct));
    }

    @Operation(summary = "批量更新商品状态")
    @SaCheckPermission("erp:product:status")
    @PutMapping("/batch-status")
    public Result<Boolean> batchUpdateStatus(@RequestBody BatchStatusUpdateDTO dto) {
        return Result.ok(productService.batchUpdateStatus(dto.getIds(), dto.getStatus()));
    }

    @Operation(summary = "批量删除商品")
    @SaCheckPermission("erp:product:delete")
    @PutMapping("/batch-delete")
    public Result<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(productService.batchDelete(body.get("ids")));
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

    @Operation(summary = "获取行业类别选项(去重)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/industry-categories")
    public Result<List<String>> getIndustryCategories() {
        return Result.ok(productService.getDistinctIndustryCategories());
    }

    @Operation(summary = "获取品牌选项(去重)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/brands")
    public Result<List<String>> getBrands() {
        return Result.ok(productService.getDistinctBrands());
    }

    @Operation(summary = "批量创建商品(含单位+推荐)")
    @SaCheckPermission("erp:product:create")
    @PostMapping("/batch-create")
    public Result<Map<String, Object>> batchCreate(@RequestBody Map<String, Object> payload) {
        try {
            // 1. 保存产品主表
            Product product = new Product();
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) payload.get("product");
            if (productMap != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                product = mapper.convertValue(productMap, Product.class);
            }
            boolean saved = productService.createProduct(product);
            if (!saved) return Result.fail("产品保存失败");

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("productId", product.getId());

            // 2. 保存产品单位
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsData = (List<Map<String, Object>>) payload.get("units");
            if (unitsData != null && !unitsData.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                for (Map<String, Object> u : unitsData) {
                    ProductUnit unit = mapper.convertValue(u, ProductUnit.class);
                    unit.setProductId(product.getId());
                    productUnitService.save(unit);
                }
            }

            // 3. 保存推荐商品
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendsData = (List<Map<String, Object>>) payload.get("recommends");
            if (recommendsData != null && !recommendsData.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<ProductRecommend> recommends = new java.util.ArrayList<>();
                for (Map<String, Object> r : recommendsData) {
                    ProductRecommend rec = mapper.convertValue(r, ProductRecommend.class);
                    rec.setProductId(product.getId());
                    recommends.add(rec);
                }
                productRecommendService.batchSave(product.getId(), recommends);
            }

            return Result.ok(result);
        } catch (Exception e) {
            log.error("[批量创建商品] 失败", e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量更新商品(含单位+推荐)")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/batch-update/{id}")
    public Result<Boolean> batchUpdate(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            // 1. 更新产品主表
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) payload.get("product");
            if (productMap != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Product product = mapper.convertValue(productMap, Product.class);
                product.setId(id);
                productService.updateProduct(product);
            }

            // 2. 更新产品单位(先删后插)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsData = (List<Map<String, Object>>) payload.get("units");
            if (unitsData != null) {
                productUnitService.lambdaUpdate()
                        .eq(ProductUnit::getProductId, id)
                        .remove();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                for (Map<String, Object> u : unitsData) {
                    ProductUnit unit = mapper.convertValue(u, ProductUnit.class);
                    unit.setProductId(id);
                    unit.setId(null); // 强制新建
                    productUnitService.save(unit);
                }
            }

            // 3. 更新推荐商品
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendsData = (List<Map<String, Object>>) payload.get("recommends");
            if (recommendsData != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<ProductRecommend> recommends = new java.util.ArrayList<>();
                for (Map<String, Object> r : recommendsData) {
                    ProductRecommend rec = mapper.convertValue(r, ProductRecommend.class);
                    rec.setProductId(id);
                    recommends.add(rec);
                }
                productRecommendService.batchSave(id, recommends);
            }

            return Result.ok(true);
        } catch (Exception e) {
            log.error("[批量更新商品] 失败", e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }
}
