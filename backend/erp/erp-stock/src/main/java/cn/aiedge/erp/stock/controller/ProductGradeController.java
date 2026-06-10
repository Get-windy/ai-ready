package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductGrade;
import cn.aiedge.erp.stock.service.ProductGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品等级Controller - 等级字典管理
 */
@Slf4j
@Tag(name = "产品等级管理")
@RestController
@RequestMapping("/api/erp/product-grade")
@RequiredArgsConstructor
public class ProductGradeController {

    private final ProductGradeService productGradeService;

    @Operation(summary = "获取全部等级列表")
    @GetMapping("/list")
    public Result<List<ProductGrade>> list() {
        return Result.ok(productGradeService.getActiveGrades());
    }

    @Operation(summary = "获取等级详情")
    @GetMapping("/{id}")
    public Result<ProductGrade> getById(@PathVariable Long id) {
        return Result.ok(productGradeService.getById(id));
    }

    @Operation(summary = "新增等级")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductGrade grade) {
        return Result.ok(productGradeService.save(grade));
    }

    @Operation(summary = "编辑等级")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductGrade grade) {
        grade.setId(id);
        return Result.ok(productGradeService.updateById(grade));
    }

    @Operation(summary = "删除等级")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productGradeService.removeById(id));
    }
}
