package cn.aiedge.erp.finance.controller;

import cn.aiedge.erp.finance.model.entity.FinanceReport;
import cn.aiedge.erp.finance.service.FinanceReportService;
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
 * 财务报表Controller
 */
@Tag(name = "财务报表管理", description = "财务报表管理接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/report")
public class FinanceReportController {
    
    @Autowired
    private FinanceReportService financeReportService;
    
    /**
     * 生成财务报表
     */
    @OperationLog(module = "财务报表管理", type = "CREATE", desc = "生成财务报表")
    @PreAuthorize("hasPermission('/api/erp/finance/report/generate', 'finance:report:generate')")
    @PostMapping("/generate")
    public Result<Void> generateReport(
            @Parameter(description = "报表类型") @RequestBody Integer reportType,
            @Parameter(description = "报表期间") @RequestBody String reportPeriod) {
        boolean success = financeReportService.generateReport(reportType, reportPeriod);
        return success ? Result.success() : Result.error("生成失败");
    }
    
    /**
     * 查询报表列表
     */
    @OperationLog(module = "财务报表管理", type = "QUERY", desc = "查询报表列表")
    @PreAuthorize("hasPermission('/api/erp/finance/report/list', 'finance:report:view')")
    @GetMapping("/list")
    public Result<List<FinanceReport>> listReports(
            @Parameter(description = "报表类型") @RequestParam(required = false) Integer reportType,
            @Parameter(description = "报表期间") @RequestParam(required = false) String reportPeriod,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        List<FinanceReport> reports = financeReportService.listReports(reportType, reportPeriod, status);
        return Result.success(reports);
    }
    
    /**
     * 查询报表详情
     */
    @OperationLog(module = "财务报表管理", type = "QUERY", desc = "查询报表详情")
    @PreAuthorize("hasPermission('/api/erp/finance/report/detail', 'finance:report:view')")
    @GetMapping("/detail/{reportNo}")
    public Result<FinanceReport> getReportDetail(@PathVariable String reportNo) {
        FinanceReport report = financeReportService.getReportDetail(reportNo);
        return Result.success(report);
    }
    
    /**
     * 审核报表
     */
    @OperationLog(module = "财务报表管理", type = "UPDATE", desc = "审核报表")
    @PreAuthorize("hasPermission('/api/erp/finance/report/approve', 'finance:report:approve')")
    @PutMapping("/approve/{reportNo}")
    public Result<Void> approveReport(
            @PathVariable String reportNo,
            @Parameter(description = "审核人") @RequestHeader("userId") String approvedBy) {
        boolean success = financeReportService.approveReport(reportNo, approvedBy);
        return success ? Result.success() : Result.error("审核失败");
    }
}
