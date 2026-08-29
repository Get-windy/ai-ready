package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 辅助核算余额实体
 */
@Data
@TableName("finance_auxiliary_balance")
@EqualsAndHashCode(callSuper = true)
public class FinanceAuxiliaryBalance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会计期间ID
     */
    @TableField("accounting_period_id")
    private Long accountingPeriodId;

    /**
     * 科目ID
     */
    @TableField("subject_id")
    private Long subjectId;

    /**
     * 辅助核算类型
     */
    @TableField("auxiliary_type_id")
    private Long auxiliaryTypeId;

    /**
     * 辅助核算项目
     */
    @TableField("auxiliary_item_id")
    private Long auxiliaryItemId;

    /**
     * 期初借方
     */
    @TableField("begin_debit")
    private BigDecimal beginDebit;

    /**
     * 期初贷方
     */
    @TableField("begin_credit")
    private BigDecimal beginCredit;

    /**
     * 本期借方
     */
    @TableField("period_debit")
    private BigDecimal periodDebit;

    /**
     * 本期贷方
     */
    @TableField("period_credit")
    private BigDecimal periodCredit;

    /**
     * 期末借方
     */
    @TableField("end_debit")
    private BigDecimal endDebit;

    /**
     * 期末贷方
     */
    @TableField("end_credit")
    private BigDecimal endCredit;

    /**
     * 本年累计借方
     */
    @TableField("year_debit")
    private BigDecimal yearDebit;

    /**
     * 本年累计贷方
     */
    @TableField("year_credit")
    private BigDecimal yearCredit;
}
