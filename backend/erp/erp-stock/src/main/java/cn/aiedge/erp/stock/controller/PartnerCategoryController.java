package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerCategory;
import cn.aiedge.erp.stock.service.PartnerCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位分类管理")
@RestController
@RequestMapping("/api/erp/partner/categories")
@RequiredArgsConstructor
public class PartnerCategoryController {

    private final PartnerCategoryService partnerCategoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<PartnerCategory>> getTree(@RequestParam(required = false) String categoryType) {
        return Result.ok(partnerCategoryService.getTree(categoryType));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerCategory category) {
        return Result.ok(partnerCategoryService.save(category));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerCategory category) {
        category.setId(id);
        return Result.ok(partnerCategoryService.updateById(category));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerCategoryService.removeById(id));
    }
}
