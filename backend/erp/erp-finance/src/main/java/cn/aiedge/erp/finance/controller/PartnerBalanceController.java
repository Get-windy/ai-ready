package cn.aiedge.erp.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.PartnerBalanceDTO;
import cn.aiedge.erp.finance.service.PartnerBalanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 往来余额表Controller
 */
@Tag(name = "往来余额表", description = "往来单位应收/应付/预收/预付余额汇总报表")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/partner-balance")
@RequiredArgsConstructor
public class PartnerBalanceController {

    private final PartnerBalanceService partnerBalanceService;

    @Operation(summary = "分页查询往来余额表")
    @GetMapping("/page")
    @SaCheckPermission("finance:partner-balance:view")
    @OperationLog(module = "往来余额表", type = "QUERY", desc = "分页查询往来余额表")
    public Result<IPage<PartnerBalanceDTO>> page(
            @Parameter(description = "往来单位类型(customer/supplier/空=全部)") @RequestParam(required = false) String partnerType,
            @Parameter(description = "关键字(名称或ID模糊查询)") @RequestParam(required = false) String keyword,
            @Parameter(description = "只显示余额非零的往来单位") @RequestParam(defaultValue = "false") boolean onlyNonZero,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        IPage<PartnerBalanceDTO> pageResult = partnerBalanceService.page(partnerType, keyword, onlyNonZero, new Page<>(page, size));
        return Result.success(pageResult);
    }
}
