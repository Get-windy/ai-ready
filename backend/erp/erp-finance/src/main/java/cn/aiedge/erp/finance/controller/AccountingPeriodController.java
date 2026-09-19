package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.AccountingPeriodDTO;
import cn.aiedge.erp.finance.dto.AccountingPeriodDateDTO;
import cn.aiedge.erp.finance.service.AccountingPeriodService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计期间Controller
 */
@Tag(name = "会计期间管理", description = "会计期间查询、新增、启停接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/period")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    @Operation(summary = "分页查询会计期间")
    @GetMapping("/page")
    @PreAuthorize("hasPermission('/api/erp/finance/period/list', 'finance:period:view')")
    @OperationLog(module = "会计期间管理", type = "QUERY", desc = "分页查询会计期间")
    public Result<IPage<AccountingPeriodDTO>> page(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer periodYear,
            @Parameter(description = "状态(1开启/0关闭)") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        return Result.success(accountingPeriodService.page(periodYear, status, new Page<>(page, size)));
    }

    @Operation(summary = "查询会计期间列表")
    @GetMapping("/list")
    @PreAuthorize("hasPermission('/api/erp/finance/period/list', 'finance:period:view')")
    @OperationLog(module = "会计期间管理", type = "QUERY", desc = "查询会计期间列表")
    public Result<List<AccountingPeriodDTO>> list(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer periodYear) {
        return Result.success(accountingPeriodService.list(periodYear));
    }

    @Operation(summary = "新增会计期间")
    @PostMapping("/")
    @PreAuthorize("hasPermission('/api/erp/finance/period/create', 'finance:period:create')")
    @OperationLog(module = "会计期间管理", type = "CREATE", desc = "新增会计期间")
    public Result<AccountingPeriodDTO> create(@Valid @RequestBody AccountingPeriodDTO dto) {
        return Result.success("创建成功", accountingPeriodService.create(dto));
    }

    @Operation(summary = "批量保存会计期间起止日期")
    @PutMapping("/batch-dates")
    @PreAuthorize("hasPermission('/api/erp/finance/period/update', 'finance:period:update')")
    @OperationLog(module = "会计期间管理", type = "UPDATE", desc = "批量保存会计期间起止日期")
    public Result<List<AccountingPeriodDTO>> batchDates(@RequestBody List<AccountingPeriodDateDTO> items) {
        return Result.success("保存成功", accountingPeriodService.saveDates(items));
    }

    @Operation(summary = "启用/停用会计期间")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasPermission('/api/erp/finance/period/update', 'finance:period:update')")
    @OperationLog(module = "会计期间管理", type = "UPDATE", desc = "启用/停用会计期间")
    public Result<AccountingPeriodDTO> updateStatus(
            @Parameter(description = "期间ID") @PathVariable Long id,
            @Parameter(description = "状态(1开启/0关闭)") @RequestParam Integer status) {
        return Result.success("状态更新成功", accountingPeriodService.updateStatus(id, status));
    }
}
