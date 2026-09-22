package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.quality.entity.QualityCertificate;
import cn.aiedge.quality.service.QualityCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Tag(name = "质量证书", description = "质量证书(COA)管理")
@RestController
@RequestMapping("/api/quality/certificate")
@RequiredArgsConstructor
@Validated
public class QualityCertificateController {

    private final QualityCertificateService service;

    @Operation(summary = "创建质量证书")
    @SaCheckPermission("quality:certificate:create")
    @PostMapping
    public Result<QualityCertificate> create(@RequestBody QualityCertificate certificate) {
        return Result.success(service.create(certificate));
    }

    @Operation(summary = "更新质量证书")
    @SaCheckPermission("quality:certificate:update")
    @PutMapping("/{id}")
    public Result<QualityCertificate> update(@PathVariable Long id, @RequestBody QualityCertificate certificate) {
        return Result.success(service.update(id, certificate));
    }

    @Operation(summary = "删除质量证书")
    @SaCheckPermission("quality:certificate:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "分页查询质量证书")
    @SaCheckPermission("quality:certificate:list")
    @GetMapping("/page")
    public Result<PageResult<QualityCertificate>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "产品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNo,
            @Parameter(description = "检验结论") @RequestParam(required = false) String result,
            @Parameter(description = "检验日期起始") @RequestParam(required = false) String startDate,
            @Parameter(description = "检验日期结束") @RequestParam(required = false) String endDate) {
        return Result.success(service.page(pageNum, pageSize, productName, batchNo, result, startDate, endDate));
    }

    @Operation(summary = "查询质量证书详情")
    @SaCheckPermission("quality:certificate:detail")
    @GetMapping("/{id}")
    public Result<QualityCertificate> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }
}
