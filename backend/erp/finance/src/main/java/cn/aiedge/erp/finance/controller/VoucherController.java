package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.VoucherDTO;
import cn.aiedge.erp.finance.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 凭证Controller
 */
@Tag(name = "凭证管理", description = "凭证创建、审核、过账、冲销接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "创建凭证")
    @PostMapping("/")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/create', 'finance:voucher:create')")
    @OperationLog(module = "凭证管理", type = "CREATE", desc = "创建凭证")
    public Result<VoucherDTO> create(@Valid @RequestBody VoucherDTO dto) {
        VoucherDTO result = voucherService.create(dto);
        return Result.success("创建成功", result);
    }

    @Operation(summary = "根据ID查询凭证")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/view', 'finance:voucher:view')")
    @OperationLog(module = "凭证管理", type = "QUERY", desc = "根据ID查询凭证")
    public Result<VoucherDTO> getById(@Parameter(description = "凭证ID") @PathVariable Long id) {
        return Result.success(voucherService.getById(id));
    }

    @Operation(summary = "根据凭证编号查询")
    @GetMapping("/no/{voucherNo}")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/view', 'finance:voucher:view')")
    @OperationLog(module = "凭证管理", type = "QUERY", desc = "根据凭证编号查询")
    public Result<VoucherDTO> getByVoucherNo(@Parameter(description = "凭证编号") @PathVariable String voucherNo) {
        return Result.success(voucherService.getByVoucherNo(voucherNo));
    }

    @Operation(summary = "分页查询凭证列表")
    @GetMapping("/list")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/list', 'finance:voucher:view')")
    @OperationLog(module = "凭证管理", type = "QUERY", desc = "分页查询凭证列表")
    public Result<IPage<VoucherDTO>> list(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "状态(draft/audited/posted)") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        IPage<VoucherDTO> pageResult = voucherService.list(fiscalYear, fiscalPeriod, status, new Page<>(page, size));
        return Result.success(pageResult);
    }

    @Operation(summary = "审核凭证")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/audit', 'finance:voucher:audit')")
    @OperationLog(module = "凭证管理", type = "UPDATE", desc = "审核凭证")
    public Result<VoucherDTO> audit(
            @Parameter(description = "凭证ID") @PathVariable Long id,
            @Parameter(description = "审核人ID") @RequestHeader(value = "userId", required = false) String auditorId) {
        if (auditorId == null || auditorId.isEmpty()) {
            auditorId = "mock_auditor";
        }
        VoucherDTO result = voucherService.audit(id, auditorId);
        return Result.success("审核成功", result);
    }

    @Operation(summary = "凭证过账")
    @PutMapping("/{id}/post")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/post', 'finance:voucher:post')")
    @OperationLog(module = "凭证管理", type = "UPDATE", desc = "凭证过账")
    public Result<VoucherDTO> post(
            @Parameter(description = "凭证ID") @PathVariable Long id,
            @Parameter(description = "过账人ID") @RequestHeader(value = "userId", required = false) String posterId) {
        if (posterId == null || posterId.isEmpty()) {
            posterId = "mock_poster";
        }
        VoucherDTO result = voucherService.post(id, posterId);
        return Result.success("过账成功", result);
    }

    @Operation(summary = "冲销凭证")
    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/reverse', 'finance:voucher:reverse')")
    @OperationLog(module = "凭证管理", type = "CREATE", desc = "冲销凭证")
    public Result<VoucherDTO> reverse(
            @Parameter(description = "凭证ID") @PathVariable Long id,
            @Valid @RequestBody ReverseRequest request) {
        VoucherDTO result = voucherService.reverse(id, request.getReason());
        return Result.success("冲销成功", result);
    }

    @Data
    public static class ReverseRequest {
        @NotBlank(message = "冲销原因不能为空")
        private String reason;
    }

    @Operation(summary = "批量删除凭证")
    @DeleteMapping("/batch")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/delete', 'finance:voucher:delete')")
    @OperationLog(module = "凭证管理", type = "DELETE", desc = "批量删除凭证")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        voucherService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "导出凭证列表")
    @GetMapping("/export")
    @PreAuthorize("hasPermission('/api/erp/finance/voucher/list', 'finance:voucher:view')")
    @OperationLog(module = "凭证管理", type = "QUERY", desc = "导出凭证列表")
    public Result<List<VoucherDTO>> export(
            @Parameter(description = "会计年度") @RequestParam(required = false) Integer fiscalYear,
            @Parameter(description = "会计期间") @RequestParam(required = false) Integer fiscalPeriod,
            @Parameter(description = "状态(draft/audited/posted)") @RequestParam(required = false) String status) {
        return Result.success(voucherService.exportList(fiscalYear, fiscalPeriod, status));
    }
}
