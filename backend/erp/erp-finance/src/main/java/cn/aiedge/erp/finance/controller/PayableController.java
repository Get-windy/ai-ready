package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.service.PayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 应付管理Controller
 */
@Tag(name = "应付管理", description = "应付款登记、核销接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/payable")
@RequiredArgsConstructor
public class PayableController {

    private final PayableService payableService;

    @Operation(summary = "创建应付款")
    @PostMapping("/")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/create', 'finance:payable:create')")
    @OperationLog(module = "应付管理", type = "CREATE", desc = "创建应付款")
    public Result<PayableDTO> create(@Valid @RequestBody PayableDTO dto) {
        PayableDTO result = payableService.create(dto);
        return Result.success("创建成功", result);
    }

    @Operation(summary = "根据ID查询应付款")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/view', 'finance:payable:view')")
    @OperationLog(module = "应付管理", type = "QUERY", desc = "根据ID查询应付款")
    public Result<PayableDTO> getById(@Parameter(description = "应付款ID") @PathVariable Long id) {
        return Result.success(payableService.getById(id));
    }

    @Operation(summary = "分页查询应付款列表")
    @GetMapping("/list")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/list', 'finance:payable:view')")
    @OperationLog(module = "应付管理", type = "QUERY", desc = "分页查询应付款列表")
    public Result<IPage<PayableDTO>> list(
            @Parameter(description = "供应商ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "状态(draft/partial/paid)") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        IPage<PayableDTO> pageResult = payableService.list(supplierId, status, new Page<>(page, size));
        return Result.success(pageResult);
    }

    @Operation(summary = "应付账龄分析")
    @GetMapping("/aging")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/aging', 'finance:payable:view')")
    @OperationLog(module = "应付管理", type = "QUERY", desc = "应付账龄分析")
    public Result<List<Map<String, Object>>> aging() {
        return Result.success(payableService.getAgingAnalysis());
    }

    @Operation(summary = "核销应付款")
    @PutMapping("/{id}/write-off")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/write-off', 'finance:payable:write-off')")
    @OperationLog(module = "应付管理", type = "UPDATE", desc = "核销应付款")
    public Result<PayableDTO> writeOff(
            @Parameter(description = "应付款ID") @PathVariable Long id,
            @Valid @RequestBody WriteOffRequest request) {
        PayableDTO result = payableService.writeOff(id, request.getAmount());
        return Result.success("核销成功", result);
    }

    @Data
    public static class WriteOffRequest {
        @NotNull(message = "核销金额不能为空")
        private BigDecimal amount;
    }

    @Operation(summary = "批量删除应付账款")
    @DeleteMapping("/batch")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/delete', 'finance:payable:delete')")
    @OperationLog(module = "应付管理", type = "DELETE", desc = "批量删除应付账款")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        payableService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "导出应付账款列表")
    @GetMapping("/export")
    @PreAuthorize("hasPermission('/api/erp/finance/payable/list', 'finance:payable:view')")
    @OperationLog(module = "应付管理", type = "QUERY", desc = "导出应付账款列表")
    public Result<List<PayableDTO>> export(
            @Parameter(description = "供应商ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "状态(normal/overdue/written_off)") @RequestParam(required = false) String status) {
        return Result.success(payableService.exportList(supplierId, status));
    }
}
