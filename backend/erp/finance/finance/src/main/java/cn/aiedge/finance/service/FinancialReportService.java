package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.ReportData;
import cn.aiedge.finance.entity.ReportTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface FinancialReportService extends IService<cn.aiedge.erp.finance.model.entity.FinanceReport> {
    
    Page<cn.aiedge.erp.finance.model.entity.FinanceReport> pageList(Long tenantId, Integer reportType, String period, Integer status, Page<cn.aiedge.erp.finance.model.entity.FinanceReport> page);
    
    cn.aiedge.erp.finance.model.entity.FinanceReport getReportDetail(Long tenantId, Long reportId);
    
    cn.aiedge.erp.finance.model.entity.FinanceReport generateBalanceSheet(Long tenantId, String period);
    
    cn.aiedge.erp.finance.model.entity.FinanceReport generateIncomeStatement(Long tenantId, String period);
    
    cn.aiedge.erp.finance.model.entity.FinanceReport generateCashFlowStatement(Long tenantId, String period);
    
    cn.aiedge.erp.finance.model.entity.FinanceReport generateEquityChangeStatement(Long tenantId, String period);
    
    boolean approveReport(Long tenantId, Long reportId);
    
    boolean publishReport(Long tenantId, Long reportId);
    
    boolean deleteReport(Long tenantId, Long reportId);
    
    Map<String, Object> exportToExcel(Long tenantId, Long reportId);
    
    List<ReportData> calculateBalanceSheetData(Long tenantId, String period);
    
    List<ReportData> calculateIncomeStatementData(Long tenantId, String period);
    
    List<ReportData> calculateCashFlowData(Long tenantId, String period);
    
    Map<String, Object> getReportSummary(Long tenantId, String period);
}