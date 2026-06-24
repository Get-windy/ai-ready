package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
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

    @Operation(summary = "完成检验")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String result = (String) params.get("result");
        BigDecimal passQuantity = new BigDecimal(params.get("passQuantity").toString());
        BigDecimal failQuantity = new BigDecimal(params.get("failQuantity").toString());
        service.complete(id, result, passQuantity, failQuantity);
        return Result.success();
    }

    @Operation(summary = "分页查询检验记录")
    @GetMapping("/page")
    public Result<PageResult<QualityInspection>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "检验结果") @RequestParam(required = false) String result) {
        return Result.success(service.page(pageNum, pageSize, bizType, result));
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