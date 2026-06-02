package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 分类账条目实体
 * 按科目和期间汇总的财务数据
 */
@Data
@TableName("finance_ledger")
@EqualsAndHashCode(callSuper = true)
public class LedgerEntry extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会计科目ID
     */
    @TableField("subject_id")
    private Long subjectId;

    /**
     * 科目编码
     */
    @TableField("subject_code")
    private String subjectCode;

    /**
     * 科目名称
     */
    @TableField("subject_name")
    private String subjectName;

    /**
     * 会计年度
     */
    @TableField("fiscal_year")
    private Integer fiscalYear;

    /**
     * 会计期间
     */
    @TableField("fiscal_period")
    private Integer fiscalPeriod;

    /**
     * 期初借方余额
     */
    @TableField("opening_debit")
    private BigDecimal openingDebit;

    /**
     * 期初贷方余额
     */
    @TableField("opening_credit")
    private BigDecimal openingCredit;

    /**
     * 本期借方发生额
     */
    @TableField("period_debit")
    private BigDecimal periodDebit;

    /**
     * 本期贷方发生额
     */
    @TableField("period_credit")
    private BigDecimal periodCredit;

    /**
     * 期末借方余额
     */
    @TableField("closing_debit")
    private BigDecimal closingDebit;

    /**
     * 期末贷方余额
     */
    @TableField("closing_credit")
    private BigDecimal closingCredit;

    /**
     * 期末余额
     */
    @TableField("closing_balance")
    private BigDecimal closingBalance;

    /**
     * 余额方向
     * 1-借方 2-贷方
     */
    @TableField("balance_direction")
    private Integer balanceDirection;
}
