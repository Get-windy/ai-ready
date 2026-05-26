package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产负债表实体
 */
@Data
@TableName("fin_balance_sheet")
public class BalanceSheet {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportNo; // 报表编号
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    
    // 资产
    private BigDecimal cashAndCashEquivalents; // 货币资金
    private BigDecimal accountsReceivable; // 应收账款
    private BigDecimal inventory; // 存货
    private BigDecimal prepaidExpenses; // 预付款项
    private BigDecimal otherCurrentAssets; // 其他流动资产
    private BigDecimal totalCurrentAssets; // 流动资产合计
    
    private BigDecimal fixedAssets; // 固定资产
    private BigDecimal constructionInProgress; // 在建工程
    private BigDecimal intangibleAssets; // 无形资产
    private BigDecimal longTermInvestments; // 长期投资
    private BigDecimal otherNonCurrentAssets; // 其他非流动资产
    private BigDecimal totalNonCurrentAssets; // 非流动资产合计
    
    private BigDecimal totalAssets; // 资产总计
    
    // 负债
    private BigDecimal shortTermLoans; // 短期借款
    private BigDecimal accountsPayable; // 应付账款
    private BigDecimal advancesFromCustomers; // 预收款项
    private BigDecimal salariesPayable; // 应付职工薪酬
    private BigDecimal taxesPayable; // 应交税费
    private BigDecimal otherCurrentLiabilities; // 其他流动负债
    private BigDecimal totalCurrentLiabilities; // 流动负债合计
    
    private BigDecimal longTermLoans; // 长期借款
    private BigDecimal bondsPayable; // 应付债券
    private BigDecimal otherNonCurrentLiabilities; // 其他非流动负债
    private BigDecimal totalNonCurrentLiabilities; // 非流动负债合计
    
    private BigDecimal totalLiabilities; // 负债合计
    
    // 所有者权益
    private BigDecimal paidInCapital; // 实收资本
    private BigDecimal capitalReserve; // 资本公积
    private BigDecimal surplusReserve; // 盈余公积
    private BigDecimal undistributedProfits; // 未分配利润
    private BigDecimal totalOwnersEquity; // 所有者权益合计
    
    private BigDecimal totalLiabilitiesAndOwnersEquity; // 负债和所有者权益总计
    
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
