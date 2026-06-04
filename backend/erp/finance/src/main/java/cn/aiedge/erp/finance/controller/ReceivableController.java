package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.service.ReceivableService;
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
 * 应收管理Controller
 */
@Tag(name = "应收管理", description = "应收款登记、核销、坏账处理接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/receivable")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;

    @Operation(summary = "创建应收款")
    @PostMapping("/")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/create', 'finance:receivable:create')")
    @OperationLog(module = "应收管理", type = "CREATE", desc = "创建应收款")
    public Result<ReceivableDTO> create(@Valid @RequestBody ReceivableDTO dto) {
        ReceivableDTO result = receivableService.create(dto);
        return Result.success("创建成功", result);
    }

    @Operation(summary = "根据ID查询应收款")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/view', 'finance:receivable:view')")
    @OperationLog(module = "应收管理", type = "QUERY", desc = "根据ID查询应收款")
    public Result<ReceivableDTO> getById(@Parameter(description = "应收款ID") @PathVariable Long id) {
        return Result.success(receivableService.getById(id));
    }

    @Operation(summary = "分页查询应收款列表")
    @GetMapping("/list")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/list', 'finance:receivable:view')")
    @OperationLog(module = "应收管理", type = "QUERY", desc = "分页查询应收款列表")
    public Result<IPage<ReceivableDTO>> list(
            @Parameter(description = "客户ID") @RequestParam(required = false) String customerId,
            @Parameter(description = "状态(draft/partial/paid/baddebt)") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        IPage<ReceivableDTO> pageResult = receivableService.list(customerId, status, new Page<>(page, size));
        return Result.success(pageResult);
    }

    @Operation(summary = "应收账龄分析")
    @GetMapping("/aging")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/aging', 'finance:receivable:view')")
    @OperationLog(module = "应收管理", type = "QUERY", desc = "应收账龄分析")
    public Result<List<Map<String, Object>>> aging() {
        return Result.success(receivableService.getAgingAnalysis());
    }

    @Operation(summary = "核销应收款")
    @PutMapping("/{id}/write-off")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/write-off', 'finance:receivable:write-off')")
    @OperationLog(module = "应收管理", type = "UPDATE", desc = "核销应收款")
    public Result<ReceivableDTO> writeOff(
            @Parameter(description = "应收款ID") @PathVariable Long id,
            @Valid @RequestBody WriteOffRequest request) {
        ReceivableDTO result = receivableService.writeOff(id, request.getAmount());
        return Result.success("核销成功", result);
    }

    @Operation(summary = "标记为坏账")
    @PutMapping("/{id}/bad-debt")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/bad-debt', 'finance:receivable:bad-debt')")
    @OperationLog(module = "应收管理", type = "UPDATE", desc = "标记为坏账")
    public Result<ReceivableDTO> markBadDebt(
            @Parameter(description = "应收款ID") @PathVariable Long id) {
        ReceivableDTO result = receivableService.markBadDebt(id);
        return Result.success("操作成功", result);
    }

    @Data
    public static class WriteOffRequest {
        @NotNull(message = "核销金额不能为空")
        private BigDecimal amount;
    }

    @Operation(summary = "批量删除应收账款")
    @DeleteMapping("/batch")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/delete', 'finance:receivable:delete')")
    @OperationLog(module = "应收管理", type = "DELETE", desc = "批量删除应收账款")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        receivableService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "导出应收账款列表")
    @GetMapping("/export")
    @PreAuthorize("hasPermission('/api/erp/finance/receivable/list', 'finance:receivable:view')")
    @OperationLog(module = "应收管理", type = "QUERY", desc = "导出应收账款列表")
    public Result<List<ReceivableDTO>> export(
            @Parameter(description = "客户ID") @RequestParam(required = false) String customerId,
            @Parameter(description = "状态(normal/overdue/written_off/bad_debt)") @RequestParam(required = false) String status) {
        return Result.success(receivableService.exportList(customerId, status));
    }
}
