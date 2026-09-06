package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.quality.dto.QualityInspectionQuery;
import cn.aiedge.quality.entity.QualityInspection;
import cn.aiedge.quality.service.QualityInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "检验记录", description = "检验记录管理")
@RestController
@RequestMapping("/api/quality/inspection")
@RequiredArgsConstructor
@Validated
public class QualityInspectionController {

    private final QualityInspectionService service;

    @Operation(summary = "创建检验记录")
    @PostMapping
    public Result<QualityInspection> create(@RequestBody QualityInspection inspection) {
        return Result.success(service.create(inspection));
    }

    @Operation(summary = "更新检验记录")
    @PutMapping("/{id}")
    public Result<QualityInspection> update(@PathVariable Long id, @RequestBody QualityInspection inspection) {
        return Result.success(service.updateInspection(id, inspection));
    }

    @Operation(summary = "完成检验")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String result = (String) params.get("result");
        BigDecimal passQuantity = new BigDecimal(params.get("passQuantity").toString());
        BigDecimal failQuantity = new BigDecimal(params.get("failQuantity").toString());
        String remark = (String) params.get("remark");
        service.complete(id, result, passQuantity, failQuantity, remark);
        return Result.success();
    }

    @Operation(summary = "分页查询检验记录")
    @GetMapping("/page")
    public Result<PageResult<QualityInspection>> page(@ModelAttribute QualityInspectionQuery query) {
        return Result.success(service.page(query));
    }

    @Operation(summary = "生成质检单号")
    @GetMapping("/next-no")
    public Result<String> nextNo() {
        return Result.success(service.generateNo());
    }

    @Operation(summary = "作废质检单")
    @PostMapping("/{id}/cancel")
    public Result<QualityInspection> cancel(@PathVariable Long id) {
        return Result.success(service.cancel(id));
    }

    @Operation(summary = "删除质检单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteInspection(id);
        return Result.success();
    }

    @Operation(summary = "批量删除质检单")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        service.batchDeleteInspection(ids);
        return Result.success();
    }

    @Operation(summary = "查询检验记录详情")
    @GetMapping("/{id}")
    public Result<QualityInspection> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @Operation(summary = "查询待检记录")
    @GetMapping("/pending")
    public Result<List<QualityInspection>> listPending(
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType) {
        return Result.success(service.listPending(bizType));
    }
}