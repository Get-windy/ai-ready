package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 利润表VO
 */
@Data
@Schema(description = "利润表详情")
public class ProfitStatementVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报表编号")
    private String reportNo;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "报告期间")
    private String period;
    
    // 收入
    @Schema(description = "营业收入")
    private BigDecimal operatingRevenue;

    @Schema(description = "其他收入")
    private BigDecimal otherIncome;

    @Schema(description = "收入合计")
    private BigDecimal totalIncome;
    
    // 成本费用
    @Schema(description = "营业成本")
    private BigDecimal operatingCosts;

    @Schema(description = "税金及附加")
    private BigDecimal taxesAndSurcharges;

    @Schema(description = "销售费用")
    private BigDecimal salesExpenses;

    @Schema(description = "管理费用")
    private BigDecimal administrativeExpenses;

    @Schema(description = "财务费用")
    private BigDecimal financialExpenses;

    @Schema(description = "资产减值损失")
    private BigDecimal assetImpairmentLosses;

    @Schema(description = "信用减值损失")
    private BigDecimal creditImpairmentLosses;

    @Schema(description = "其他费用")
    private BigDecimal otherExpenses;

    @Schema(description = "费用合计")
    private BigDecimal totalExpenses;
    
    // 利润
    @Schema(description = "营业利润")
    private BigDecimal grossProfit;

    @Schema(description = "投资收益")
    private BigDecimal investmentIncome;

    @Schema(description = "营业外收入")
    private BigDecimal nonOperatingIncome;

    @Schema(description = "营业外支出")
    private BigDecimal nonOperatingExpenses;

    @Schema(description = "利润总额")
    private BigDecimal totalProfit;

    @Schema(description = "所得税费用")
    private BigDecimal incomeTaxExpense;

    @Schema(description = "净利润")
    private BigDecimal netProfit;
    
    // 每股收益
    @Schema(description = "基本每股收益")
    private BigDecimal basicEarningsPerShare;

    @Schema(description = "稀释每股收益")
    private BigDecimal dilutedEarningsPerShare;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
