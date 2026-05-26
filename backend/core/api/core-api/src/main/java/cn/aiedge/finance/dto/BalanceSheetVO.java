package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产负债表VO
 */
@Data
@Schema(description = "资产负债表详情")
public class BalanceSheetVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "报表编号")
    private String reportNo;

    @Schema(description = "报表日期")
    private LocalDate reportDate;

    @Schema(description = "报告期间")
    private String period;
    
    // 资产
    @Schema(description = "货币资金")
    private BigDecimal cashAndCashEquivalents;

    @Schema(description = "应收账款")
    private BigDecimal accountsReceivable;

    @Schema(description = "存货")
    private BigDecimal inventory;

    @Schema(description = "预付款项")
    private BigDecimal prepaidExpenses;

    @Schema(description = "其他流动资产")
    private BigDecimal otherCurrentAssets;

    @Schema(description = "流动资产合计")
    private BigDecimal totalCurrentAssets;

    @Schema(description = "固定资产")
    private BigDecimal fixedAssets;

    @Schema(description = "在建工程")
    private BigDecimal constructionInProgress;

    @Schema(description = "无形资产")
    private BigDecimal intangibleAssets;

    @Schema(description = "长期投资")
    private BigDecimal longTermInvestments;

    @Schema(description = "其他非流动资产")
    private BigDecimal otherNonCurrentAssets;

    @Schema(description = "非流动资产合计")
    private BigDecimal totalNonCurrentAssets;

    @Schema(description = "资产总计")
    private BigDecimal totalAssets;
    
    // 负债
    @Schema(description = "短期借款")
    private BigDecimal shortTermLoans;

    @Schema(description = "应付账款")
    private BigDecimal accountsPayable;

    @Schema(description = "预收款项")
    private BigDecimal advancesFromCustomers;

    @Schema(description = "应付职工薪酬")
    private BigDecimal salariesPayable;

    @Schema(description = "应交税费")
    private BigDecimal taxesPayable;

    @Schema(description = "其他流动负债")
    private BigDecimal otherCurrentLiabilities;

    @Schema(description = "流动负债合计")
    private BigDecimal totalCurrentLiabilities;

    @Schema(description = "长期借款")
    private BigDecimal longTermLoans;

    @Schema(description = "应付债券")
    private BigDecimal bondsPayable;

    @Schema(description = "其他非流动负债")
    private BigDecimal otherNonCurrentLiabilities;

    @Schema(description = "非流动负债合计")
    private BigDecimal totalNonCurrentLiabilities;

    @Schema(description = "负债合计")
    private BigDecimal totalLiabilities;
    
    // 所有者权益
    @Schema(description = "实收资本")
    private BigDecimal paidInCapital;

    @Schema(description = "资本公积")
    private BigDecimal capitalReserve;

    @Schema(description = "盈余公积")
    private BigDecimal surplusReserve;

    @Schema(description = "未分配利润")
    private BigDecimal undistributedProfits;

    @Schema(description = "所有者权益合计")
    private BigDecimal totalOwnersEquity;

    @Schema(description = "负债和所有者权益总计")
    private BigDecimal totalLiabilitiesAndOwnersEquity;

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
