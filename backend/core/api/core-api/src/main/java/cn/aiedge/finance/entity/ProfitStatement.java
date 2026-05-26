package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 利润表实体
 */
@Data
@TableName("fin_profit_statement")
public class ProfitStatement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String reportNo; // 报表编号
    private LocalDate reportDate; // 报表日期
    private String period; // 报告期间
    
    // 收入
    private BigDecimal operatingRevenue; // 营业收入
    private BigDecimal otherIncome; // 其他收入
    private BigDecimal totalIncome; // 收入合计
    
    // 成本费用
    private BigDecimal operatingCosts; // 营业成本
    private BigDecimal taxesAndSurcharges; // 税金及附加
    private BigDecimal salesExpenses; // 销售费用
    private BigDecimal administrativeExpenses; // 管理费用
    private BigDecimal financialExpenses; // 财务费用
    private BigDecimal assetImpairmentLosses; // 资产减值损失
    private BigDecimal creditImpairmentLosses; // 信用减值损失
    private BigDecimal otherExpenses; // 其他费用
    private BigDecimal totalExpenses; // 费用合计
    
    // 利润
    private BigDecimal grossProfit; // 营业利润
    private BigDecimal investmentIncome; // 投资收益
    private BigDecimal nonOperatingIncome; // 营业外收入
    private BigDecimal nonOperatingExpenses; // 营业外支出
    private BigDecimal totalProfit; // 利润总额
    private BigDecimal incomeTaxExpense; // 所得税费用
    private BigDecimal netProfit; // 净利润
    
    // 每股收益
    private BigDecimal basicEarningsPerShare; // 基本每股收益
    private BigDecimal dilutedEarningsPerShare; // 稀释每股收益
    
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
