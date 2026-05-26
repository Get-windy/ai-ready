package cn.aiedge.finance.controller;

import cn.aiedge.finance.entity.ReportData;
import cn.aiedge.finance.entity.ReportTemplate;
import cn.aiedge.finance.enums.ReportStatus;
import cn.aiedge.finance.enums.ReportType;
import cn.aiedge.finance.service.FinancialReportService;
import cn.aiedge.finance.service.ReportDataService;
import cn.aiedge.finance.service.ReportTemplateService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/report")
@Tag(name = "财务报表管理", description = "财政部标准格式报表生成和管理")
public class FinancialReportController {
    
    @Autowired
    private FinancialReportService financialReportService;
    
    @Autowired
    private ReportTemplateService reportTemplateService;
    
    @Autowired
    private ReportDataService reportDataService;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询报表")
    public Page<cn.aiedge.erp.finance.model.entity.FinanceReport> page(
            @Parameter(description = "报表类型") @RequestParam(required = false) Integer reportType,
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<cn.aiedge.erp.finance.model.entity.FinanceReport> page = new Page<>(pageNum, pageSize);
        return financialReportService.pageList(tenantId, reportType, period, status, page);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取报表详情")
    public cn.aiedge.erp.finance.model.entity.FinanceReport getById(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.getReportDetail(tenantId, id);
    }
    
    @GetMapping("/{id}/data")
    @Operation(summary = "获取报表数据")
    public List<ReportData> getReportData(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return reportDataService.listByReportId(tenantId, id);
    }
    
    @GetMapping("/{id}/data-map")
    @Operation(summary = "获取报表数据Map")
    public Map<String, Object> getReportDataMap(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return reportDataService.getReportDataMap(tenantId, id);
    }
    
    @PostMapping("/balance-sheet/{period}")
    @Operation(summary = "生成资产负债表")
    public cn.aiedge.erp.finance.model.entity.FinanceReport generateBalanceSheet(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.generateBalanceSheet(tenantId, period);
    }
    
    @PostMapping("/income-statement/{period}")
    @Operation(summary = "生成利润表")
    public cn.aiedge.erp.finance.model.entity.FinanceReport generateIncomeStatement(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.generateIncomeStatement(tenantId, period);
    }
    
    @PostMapping("/cash-flow/{period}")
    @Operation(summary = "生成现金流量表")
    public cn.aiedge.erp.finance.model.entity.FinanceReport generateCashFlow(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.generateCashFlowStatement(tenantId, period);
    }
    
    @PostMapping("/equity-change/{period}")
    @Operation(summary = "生成所有者权益变动表")
    public cn.aiedge.erp.finance.model.entity.FinanceReport generateEquityChange(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.generateEquityChangeStatement(tenantId, period);
    }
    
    @PutMapping("/{id}/approve")
    @Operation(summary = "审核报表")
    public boolean approve(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.approveReport(tenantId, id);
    }
    
    @PutMapping("/{id}/publish")
    @Operation(summary = "发布报表")
    public boolean publish(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.publishReport(tenantId, id);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除报表")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.deleteReport(tenantId, id);
    }
    
    @GetMapping("/{id}/export")
    @Operation(summary = "导出报表")
    public Map<String, Object> export(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.exportToExcel(tenantId, id);
    }
    
    @GetMapping("/summary/{period}")
    @Operation(summary = "获取报表汇总")
    public Map<String, Object> getSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return financialReportService.getReportSummary(tenantId, period);
    }
    
    @GetMapping("/template/list/{reportType}")
    @Operation(summary = "获取报表模板")
    public List<ReportTemplate> listTemplates(@PathVariable Integer reportType) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return reportTemplateService.listByReportType(tenantId, reportType);
    }
    
    @PostMapping("/template/init/{reportType}")
    @Operation(summary = "初始化标准报表模板")
    public boolean initTemplates(@PathVariable Integer reportType) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return reportTemplateService.initStandardTemplates(tenantId, reportType);
    }
    
    @GetMapping("/types")
    @Operation(summary = "获取报表类型列表")
    public List<ReportType> listTypes() {
        return List.of(ReportType.values());
    }
    
    @GetMapping("/statuses")
    @Operation(summary = "获取报表状态列表")
    public List<ReportStatus> listStatuses() {
        return List.of(ReportStatus.values());
    }
}