package cn.aiedge.erp.finance.controller;

import cn.aiedge.erp.finance.model.entity.FinanceTaxDeclaration;
import cn.aiedge.erp.finance.service.FinanceTaxDeclarationService;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 税务申报Controller
 */
@Tag(name = "税务申报管理", description = "税务申报管理接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/tax")
public class FinanceTaxDeclarationController {
    
    @Autowired
    private FinanceTaxDeclarationService financeTaxDeclarationService;
    
    /**
     * 创建税务申报
     */
    @OperationLog(module = "税务申报管理", type = "CREATE", desc = "创建税务申报")
    @PreAuthorize("hasPermission('/api/erp/finance/tax/declaration', 'finance:tax:create')")
    @PostMapping("/declaration")
    public Result<FinanceTaxDeclaration> createDeclaration(@RequestBody FinanceTaxDeclaration declaration) {
        boolean success = financeTaxDeclarationService.createDeclaration(declaration);
        return success ? Result.success(declaration) : Result.error("申报失败");
    }
    
    /**
     * 查询申报列表
     */
    @OperationLog(module = "税务申报管理", type = "QUERY", desc = "查询申报列表")
    @PreAuthorize("hasPermission('/api/erp/finance/tax/declaration/list', 'finance:tax:view')")
    @GetMapping("/declaration/list")
    public Result<List<FinanceTaxDeclaration>> listDeclarations(
            @Parameter(description = "税种") @RequestParam(required = false) Integer taxType,
            @Parameter(description = "申报期间") @RequestParam(required = false) String declarationPeriod,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        List<FinanceTaxDeclaration> declarations = financeTaxDeclarationService.listDeclarations(taxType, declarationPeriod, status);
        return Result.success(declarations);
    }
    
    /**
     * 查询申报详情
     */
    @OperationLog(module = "税务申报管理", type = "QUERY", desc = "查询申报详情")
    @PreAuthorize("hasPermission('/api/erp/finance/tax/declaration/detail', 'finance:tax:view')")
    @GetMapping("/declaration/detail/{declarationNo}")
    public Result<FinanceTaxDeclaration> getDeclarationDetail(@PathVariable String declarationNo) {
        FinanceTaxDeclaration declaration = financeTaxDeclarationService.getDeclarationDetail(declarationNo);
        return Result.success(declaration);
    }
    
    /**
     * 更新申报状态
     */
    @OperationLog(module = "税务申报管理", type = "UPDATE", desc = "更新申报状态")
    @PreAuthorize("hasPermission('/api/erp/finance/tax/declaration/status', 'finance:tax:edit')")
    @PutMapping("/declaration/status")
    public Result<Void> updateDeclarationStatus(
            @RequestBody FinanceTaxDeclaration declaration) {
        boolean success = financeTaxDeclarationService.updateDeclarationStatus(
                declaration.getDeclarationNo(), declaration.getStatus());
        return success ? Result.success() : Result.error("更新失败");
    }
    
    /**
     * 查询应缴税款统计
     */
    @OperationLog(module = "税务申报管理", type = "QUERY", desc = "查询应缴税款统计")
    @PreAuthorize("hasPermission('/api/erp/finance/tax/payable', 'finance:tax:view')")
    @GetMapping("/tax/payable")
    public Result<Object> getTaxPayableStatistics(@RequestParam(required = false) String period) {
        Object statistics = financeTaxDeclarationService.getTaxPayableStatistics(period);
        return Result.success(statistics);
    }
}
