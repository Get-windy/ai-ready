package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现金流量表VO
 */
@Data
@Schema(description = "现金流量表详情")
public class CashFlowStatementVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报表编号")
    private String reportNo;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "报告期间")
    private String period;
    
    // 经营活动产生的现金流量
    @Schema(description = "销售商品、提供劳务收到的现金")
    private BigDecimal cashReceivedFromSales;

    @Schema(description = "收到的税费返还")
    private BigDecimal taxRefundsReceived;

    @Schema(description = "收到其他与经营活动有关的现金")
    private BigDecimal otherCashReceivedFromOperating;

    @Schema(description = "购买商品、接受劳务支付的现金")
    private BigDecimal cashPaidForGoods;

    @Schema(description = "支付给职工以及为职工支付的现金")
    private BigDecimal cashPaidToEmployees;

    @Schema(description = "支付的各项税费")
    private BigDecimal taxPayments;

    @Schema(description = "支付其他与经营活动有关的现金")
    private BigDecimal otherCashPaidForOperating;

    @Schema(description = "经营活动现金流量净额")
    private BigDecimal netCashFromOperatingActivities;
    
    // 投资活动产生的现金流量
    @Schema(description = "收回投资收到的现金")
    private BigDecimal proceedsFromDisposalOfInvestments;

    @Schema(description = "取得投资收益收到的现金")
    private BigDecimal investmentIncomeReceived;

    @Schema(description = "处置固定资产、无形资产和其他长期资产收回的现金净额")
    private BigDecimal proceedsFromDisposalOfFixedAssets;

    @Schema(description = "收到其他与投资活动有关的现金")
    private BigDecimal otherCashReceivedFromInvesting;

    @Schema(description = "投资支付的现金")
    private BigDecimal cashPaidForInvestments;

    @Schema(description = "购建固定资产、无形资产和其他长期资产支付的现金")
    private BigDecimal cashPaidForAcquisitionOfFixedAssets;

    @Schema(description = "支付其他与投资活动有关的现金")
    private BigDecimal otherCashPaidForInvesting;

    @Schema(description = "投资活动现金流量净额")
    private BigDecimal netCashFromInvestingActivities;
    
    // 筹资活动产生的现金流量
    @Schema(description = "吸收投资收到的现金")
    private BigDecimal cashReceivedFromInvestors;

    @Schema(description = "取得借款收到的现金")
    private BigDecimal borrowingsReceived;

    @Schema(description = "收到其他与筹资活动有关的现金")
    private BigDecimal otherCashReceivedFromFinancing;

    @Schema(description = "偿还债务支付的现金")
    private BigDecimal repaymentsOfPrincipal;

    @Schema(description = "分配股利、利润或偿付利息支付的现金")
    private BigDecimal dividendInterestPayments;

    @Schema(description = "支付其他与筹资活动有关的现金")
    private BigDecimal otherCashPaidForFinancing;

    @Schema(description = "筹资活动现金流量净额")
    private BigDecimal netCashFromFinancingActivities;

    @Schema(description = "汇率变动对现金的影响")
    private BigDecimal exchangeRateEffect;

    @Schema(description = "现金及现金等价物净增加额")
    private BigDecimal netIncreaseInCash;

    @Schema(description = "期初现金及现金等价物余额")
    private BigDecimal beginningCashBalance;

    @Schema(description = "期末现金及现金等价物余额")
    private BigDecimal endingCashBalance;

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
