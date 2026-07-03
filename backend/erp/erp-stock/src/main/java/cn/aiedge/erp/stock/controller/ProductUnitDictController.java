package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductUnitDict;
import cn.aiedge.erp.stock.service.ProductUnitDictService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品单位字典Controller
 */
@Slf4j
@Tag(name = "商品单位字典管理")
@RestController
@RequestMapping("/api/erp/product-unit-dict")
@RequiredArgsConstructor
public class ProductUnitDictController {

    private final ProductUnitDictService productUnitDictService;

    @Operation(summary = "分页查询单位列表")
    @GetMapping("/page")
    public Result<IPage<ProductUnitDict>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(productUnitDictService.getPage(tenantId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取所有单位（当前租户）")
    @GetMapping("/list")
    public Result<List<ProductUnitDict>> list() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        return Result.ok(productUnitDictService.getByTenantId(tenantId));
    }

    @Operation(summary = "创建单位")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductUnitDict unit) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) tenantId = 1L;
        unit.setTenantId(tenantId);
        unit.setId(null);
        if (unit.getStatus() == null) unit.setStatus(1);
        if (unit.getSortOrder() == null) unit.setSortOrder(0);
        return Result.ok(productUnitDictService.save(unit));
    }

    @Operation(summary = "更新单位")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductUnitDict unit) {
        unit.setId(id);
        return Result.ok(productUnitDictService.updateById(unit));
    }

    @Operation(summary = "删除单位")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productUnitDictService.removeById(id));
    }
}
