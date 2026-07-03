package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductBrand;
import cn.aiedge.erp.stock.service.ProductBrandService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品品牌Controller
 */
@Slf4j
@Tag(name = "商品品牌管理")
@RestController
@RequestMapping("/api/erp/product-brand")
@RequiredArgsConstructor
public class ProductBrandController {

    private final ProductBrandService productBrandService;

    @Operation(summary = "分页查询品牌列表")
    @GetMapping("/page")
    public Result<IPage<ProductBrand>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(productBrandService.getPage(tenantId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取所有品牌（当前租户）")
    @GetMapping("/list")
    public Result<List<ProductBrand>> list() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(productBrandService.getByTenantId(tenantId));
    }

    @Operation(summary = "创建品牌")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductBrand brand) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        brand.setTenantId(tenantId);
        brand.setId(null);
        if (brand.getStatus() == null) brand.setStatus(1);
        if (brand.getSortOrder() == null) brand.setSortOrder(0);
        return Result.ok(productBrandService.save(brand));
    }

    @Operation(summary = "更新品牌")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductBrand brand) {
        brand.setId(id);
        return Result.ok(productBrandService.updateById(brand));
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productBrandService.removeById(id));
    }
}
