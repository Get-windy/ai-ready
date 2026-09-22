package cn.aiedge.quality.controller;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.base.vo.Result;
import cn.aiedge.quality.entity.QualityDefectHandle;
import cn.aiedge.quality.entity.QualityDefectHandleHistory;
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
import cn.dev33.satoken.annotation.SaCheckPermission;

@Tag(name = "不合格处理", description = "不合格处理管理")
@RestController
@RequestMapping("/api/quality/defect")
@RequiredArgsConstructor
@Validated
public class QualityDefectHandleController {

    private final QualityDefectHandleService service;

    @Operation(summary = "创建不合格处理记录")
    @SaCheckPermission("quality:defect:create")
    @PostMapping
    public Result<QualityDefectHandle> create(@RequestBody Map<String, Object> params) {
        Long inspectionId = Long.valueOf(params.get("inspectionId").toString());
        String defectType = (String) params.get("defectType");
        String defectDesc = (String) params.get("defectDesc");
        BigDecimal defectQuantity = new BigDecimal(params.get("defectQuantity").toString());
        String defectLevel = (String) params.get("defectLevel");
        return Result.success(service.create(inspectionId, defectType, defectDesc, defectQuantity, defectLevel));
    }

    @Operation(summary = "处理不合格")
    @SaCheckPermission("quality:defect:update")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        String handleType = (String) params.get("handleType");
        BigDecimal handleQuantity = new BigDecimal(params.get("handleQuantity").toString());
        String handleResult = (String) params.get("handleResult");
        String correctiveAction = (String) params.get("correctiveAction");
        String preventiveAction = (String) params.get("preventiveAction");
        service.handle(id, handleType, handleQuantity, handleResult, correctiveAction, preventiveAction);
        return Result.success();
    }

    @Operation(summary = "分页查询不合格处理记录")
    @SaCheckPermission("quality:defect:list")
    @GetMapping("/page")
    public Result<PageResult<QualityDefectHandle>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "检验记录ID") @RequestParam(required = false) String inspectionId,
            @Parameter(description = "缺陷类型") @RequestParam(required = false) String defectType,
            @Parameter(description = "缺陷等级") @RequestParam(required = false) String defectLevel,
            @Parameter(description = "状态(PENDING/HANDLED 或 0/1)") @RequestParam(required = false) String status,
            @Parameter(description = "来源单号(模糊)") @RequestParam(required = false) String bizNo,
            @Parameter(description = "处理人(模糊)") @RequestParam(required = false) String handlerName,
            @Parameter(description = "创建日期起") @RequestParam(required = false) String createTimeStart,
            @Parameter(description = "创建日期止") @RequestParam(required = false) String createTimeEnd) {
        return Result.success(service.page(pageNum, pageSize, parseInspectionId(inspectionId), defectType, defectLevel,
                parseStatus(status), bizNo, handlerName, createTimeStart, createTimeEnd));
    }

    /**
     * 解析检验记录ID，非数字返回 null（不筛选），避免前端传空串/非法值触发类型转换异常
     */
    private Long parseInspectionId(String inspectionId) {
        if (inspectionId == null || inspectionId.trim().isEmpty()) return null;
        try {
            return Long.valueOf(inspectionId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析状态：兼容前端字符串 PENDING/HANDLED 与数字 0/1，非法值视为不筛选
     */
    private Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) return null;
        String s = status.trim();
        if (s.equalsIgnoreCase("PENDING") || s.equals("0")) return 0;
        if (s.equalsIgnoreCase("HANDLED") || s.equals("1")) return 1;
        return null;
    }

    @Operation(summary = "查询处理记录详情")
    @SaCheckPermission("quality:defect:detail")
    @GetMapping("/{id}")
    public Result<QualityDefectHandle> get(@PathVariable Long id) {
        return Result.success(service.get(id));
    }

    @Operation(summary = "查询待处理记录")
    @SaCheckPermission("quality:defect:list")
    @GetMapping("/pending")
    public Result<List<QualityDefectHandle>> listPending() {
        return Result.success(service.listPending());
    }

    @Operation(summary = "查询缺陷处理历史")
    @SaCheckPermission("quality:defect:list")
    @GetMapping("/{id}/history")
    public Result<List<QualityDefectHandleHistory>> history(@PathVariable Long id) {
        return Result.success(service.listHistory(id));
    }
}