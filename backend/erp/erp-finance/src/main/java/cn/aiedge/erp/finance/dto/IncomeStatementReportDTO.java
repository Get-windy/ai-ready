package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 利润表报表结果（纵向 3 列：项目 / 本期发生额 / 本年累计）
 *
 * 口径：本期发生额 = 单会计月当月（多会计月为区间合计）；本年累计 = 年初至止月累计。
 * P0 红线：只读取自凭证生成的科目余额（finance_ledger），不手工改表、不绕过凭证。
 */
@Data
public class IncomeStatementReportDTO {

    // ── 查询口径回显 ──
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String periodMode;
    private Integer startPeriod;
    private Integer endPeriod;
    private Integer subjectLevel;
    private Boolean showZero;

    /** 报表项目行（一/二/三/四 段汇总行 + 科目明细行） */
    private List<IncomeStatementRowDTO> rows = new ArrayList<>();

    // ── 段汇总（本期发生额） ──
    /** 一、营业收入 */
    private BigDecimal revenueTotal = BigDecimal.ZERO;
    /** 其他业务收入（加项合计） */
    private BigDecimal otherIncomeTotal = BigDecimal.ZERO;
    /** 营业成本（减项合计） */
    private BigDecimal costTotal = BigDecimal.ZERO;
    /** 期间费用合计（销售/管理/财务等） */
    private BigDecimal expenseTotal = BigDecimal.ZERO;
    /** 二、营业利润 */
    private BigDecimal operatingProfit = BigDecimal.ZERO;
    /** 营业外收入合计（加项） */
    private BigDecimal nonOperatingIncomeTotal = BigDecimal.ZERO;
    /** 营业外支出合计（减项） */
    private BigDecimal nonOperatingExpenseTotal = BigDecimal.ZERO;
    /** 三、利润总额 */
    private BigDecimal totalProfit = BigDecimal.ZERO;
    /** 所得税费用（减项） */
    private BigDecimal incomeTaxTotal = BigDecimal.ZERO;
    /** 四、净利润 */
    private BigDecimal netProfit = BigDecimal.ZERO;

    // ── 段汇总（本年累计） ──
    private BigDecimal revenueTotalCumulative = BigDecimal.ZERO;
    private BigDecimal otherIncomeTotalCumulative = BigDecimal.ZERO;
    private BigDecimal costTotalCumulative = BigDecimal.ZERO;
    private BigDecimal expenseTotalCumulative = BigDecimal.ZERO;
    private BigDecimal operatingProfitCumulative = BigDecimal.ZERO;
    private BigDecimal nonOperatingIncomeTotalCumulative = BigDecimal.ZERO;
    private BigDecimal nonOperatingExpenseTotalCumulative = BigDecimal.ZERO;
    private BigDecimal totalProfitCumulative = BigDecimal.ZERO;
    private BigDecimal incomeTaxTotalCumulative = BigDecimal.ZERO;
    private BigDecimal netProfitCumulative = BigDecimal.ZERO;

    /** 是否取到科目余额数据 */
    private Boolean hasData = Boolean.FALSE;
}
