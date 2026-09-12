package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.BalanceSheetDTO;
import cn.aiedge.erp.finance.dto.BalanceSheetQuery;
import cn.aiedge.erp.finance.dto.BalanceSheetReportDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementDTO;
import cn.aiedge.erp.finance.dto.IncomeStatementQuery;
import cn.aiedge.erp.finance.dto.IncomeStatementReportDTO;
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
     * 生成资产负债表（扁平列表，兼容旧报表页）
     */
    List<BalanceSheetDTO> generateBalanceSheet(Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 生成资产负债表（左右对照 · 金标准）
     *
     * 口径：期末余额 = 所选期止月时点数；年初余额 = 本年度年初数；
     * 固定项目行映射到公司实际启用科目，未结转损益计入未分配利润以保证「资产 = 负债 + 所有者权益」。
     */
    BalanceSheetReportDTO generateBalanceSheetReport(BalanceSheetQuery query);

    /**
     * 生成利润表
     */
    List<IncomeStatementDTO> generateIncomeStatement(Integer fiscalYear, Integer fiscalPeriod, Integer startMonth, Integer endMonth);

    /**
     * 生成利润表（科目层级驱动 · 金标准）
     *
     * 口径：本期发生额 = 单会计月当月（多会计月为区间合计）；本年累计 = 年初至止月累计；
     * 报表项目行由公司实际启用的成本类/损益类科目按「科目层级」展开生成，
     * 并插入 一、营业收入 / 二、营业利润 / 三、利润总额 / 四、净利润 段汇总行。
     */
    IncomeStatementReportDTO generateIncomeStatementReport(IncomeStatementQuery query);

    /**
     * 获取仪表盘KPI
     */
    Map<String, Object> getDashboardKPIs();
}
