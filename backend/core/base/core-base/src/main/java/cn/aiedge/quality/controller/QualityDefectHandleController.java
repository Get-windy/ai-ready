package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.quality.entity.QualityDefectHandle;
import cn.aiedge.quality.service.QualityDefectHandleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(name = "不合格处理", description = "不合格处理管理")
@RestController
@RequestMapping("/api/quality/defect")
@RequiredArgsConstructor
@Validated
public class QualityDefectHandleController {

    private final QualityDefectHandleService service;

    @Operation(summary = "创建不合格处理记录")
    @PostMapping
    public Result<QualityDefectHandle> create(@RequestBody Map<String, Object> params) {
        Long inspectionId = Long.valueOf(params.get("inspectionId").toString());
        String defectType = (String) params.get("defectType");
        String defectDesc = (String) params.get("defectDesc");
        BigDecimal defectQuantity = new BigDecimal(params.get("defectQuantity").toString());
        return Result.success(service.create(inspectionId, defectType, defectDesc, defectQuantity));
    }

    @Operation(summary = "处理不合格")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String handleType = (String) params.get("handleType");
        BigDecimal handleQuantity = new BigDecimal(params.get("handleQuantity").toString());
        String handleResult = (String) params.get("handleResult");
        service.handle(id, handleType, handleQuantity, handleResult);
        return Result.success();
    }

    @Operation(summary = "分页查询不合格处理记录")
    @GetMapping("/page")
    public Result<PageResult<QualityDefectHandle>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return Result.success(service.page(pageNum, pageSize, status));
    }

    @Operation(summary = "查询处理记录详情")
    @GetMapping("/{id}")
    public Result<QualityDefectHandle> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @Operation(summary = "查询待处理记录")
    @GetMapping("/pending")
    public Result<List<QualityDefectHandle>> listPending() {
        return Result.success(service.listPending());
    }
}