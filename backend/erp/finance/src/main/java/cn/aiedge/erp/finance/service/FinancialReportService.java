package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceDTO;

import java.util.List;
import java.util.Map;

/**
 * 财务报表Service接口
 */
public interface FinancialReportService {

    /**
     * 生成试算平衡表
     */
    List<TrialBalanceDTO> generateTrialBalance(Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 生成资产负债表
     */
    List<BalanceSheetDTO> generateBalanceSheet(Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 生成利润表
     */
    List<IncomeStatementDTO> generateIncomeStatement(Integer fiscalYear, Integer fiscalPeriod, Integer startMonth, Integer endMonth);

    /**
     * 获取仪表盘KPI
     */
    Map<String, Object> getDashboardKPIs();
}
