package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现金流量表实体
 */
@Data
@TableName("fin_cash_flow_statement")
public class CashFlowStatement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportNo; // 报表编号
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    
    // 经营活动产生的现金流量
    private BigDecimal cashReceivedFromSales; // 销售商品、提供劳务收到的现金
    private BigDecimal taxRefundsReceived; // 收到的税费返还
    private BigDecimal otherCashReceivedFromOperating; // 收到其他与经营活动有关的现金
    private BigDecimal cashPaidForGoods; // 购买商品、接受劳务支付的现金
    private BigDecimal cashPaidToEmployees; // 支付给职工以及为职工支付的现金
    private BigDecimal taxPayments; // 支付的各项税费
    private BigDecimal otherCashPaidForOperating; // 支付其他与经营活动有关的现金
    
    private BigDecimal netCashFromOperatingActivities; // 经营活动现金流量净额
    
    // 投资活动产生的现金流量
    private BigDecimal proceedsFromDisposalOfInvestments; // 收回投资收到的现金
    private BigDecimal investmentIncomeReceived; // 取得投资收益收到的现金
    private BigDecimal proceedsFromDisposalOfFixedAssets; // 处置固定资产、无形资产和其他长期资产收回的现金净额
    private BigDecimal otherCashReceivedFromInvesting; // 收到其他与投资活动有关的现金
    private BigDecimal cashPaidForInvestments; // 投资支付的现金
    private BigDecimal cashPaidForAcquisitionOfFixedAssets; // 购建固定资产、无形资产和其他长期资产支付的现金
    private BigDecimal otherCashPaidForInvesting; // 支付其他与投资活动有关的现金
    
    private BigDecimal netCashFromInvestingActivities; // 投资活动现金流量净额
    
    // 筹资活动产生的现金流量
    private BigDecimal cashReceivedFromInvestors; // 吸收投资收到的现金
    private BigDecimal borrowingsReceived; // 取得借款收到的现金
    private BigDecimal otherCashReceivedFromFinancing; // 收到其他与筹资活动有关的现金
    private BigDecimal repaymentsOfPrincipal; // 偿还债务支付的现金
    private BigDecimal dividendInterestPayments; // 分配股利、利润或偿付利息支付的现金
    private BigDecimal otherCashPaidForFinancing; // 支付其他与筹资活动有关的现金
    
    private BigDecimal netCashFromFinancingActivities; // 筹资活动现金流量净额
    
    private BigDecimal exchangeRateEffect; // 汇率变动对现金的影响
    private BigDecimal netIncreaseInCash; // 现金及现金等价物净增加额
    private BigDecimal beginningCashBalance; // 期初现金及现金等价物余额
    private BigDecimal endingCashBalance; // 期末现金及现金等价物余额
    
    private String auditor; // 审核人
    private String status; // 状态 (DRAFT-草稿, AUDITED-已审核, APPROVED-已批准)
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
