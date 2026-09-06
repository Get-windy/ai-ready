package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.quality.entity.QualityStandard;
import cn.aiedge.quality.service.QualityStandardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "质检标准", description = "质检标准管理")
@RestController
@RequestMapping("/api/quality/standard")
@RequiredArgsConstructor
@Validated
public class QualityStandardController {

    private final QualityStandardService service;

    @Operation(summary = "创建质检标准")
    @PostMapping
    public Result<QualityStandard> create(@RequestBody QualityStandard standard) {
        return Result.success(service.create(standard));
    }

    @Operation(summary = "更新质检标准")
    @PutMapping("/{id}")
    public Result<QualityStandard> update(@PathVariable Long id, @RequestBody QualityStandard standard) {
        return Result.success(service.update(id, standard));
    }

    @Operation(summary = "删除质检标准")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "分页查询质检标准")
    @GetMapping("/page")
    public Result<PageResult<QualityStandard>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "标准编码") @RequestParam(required = false) String standardCode,
            @Parameter(description = "标准名称") @RequestParam(required = false) String standardName,
            @Parameter(description = "检验类型") @RequestParam(required = false) String inspectionType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(service.page(pageNum, pageSize, standardCode, standardName, inspectionType, status));
    }

    @Operation(summary = "查询质检标准详情")
    @GetMapping("/{id}")
    public Result<QualityStandard> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @Operation(summary = "生成质检标准编码")
    @GetMapping("/next-no")
    public Result<String> nextNo() {
        return Result.success(service.generateNo());
    }

    @Operation(summary = "根据检验类型查询标准列表")
    @GetMapping("/list-by-type")
    public Result<List<QualityStandard>> listByType(@RequestParam String inspectionType) {
        return Result.success(service.listByType(inspectionType));
    }
}