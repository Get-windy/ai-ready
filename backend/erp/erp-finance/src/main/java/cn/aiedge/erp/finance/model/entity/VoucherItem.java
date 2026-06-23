package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 凭证明细行实体
 * 记录凭证中每一条借贷分录
 */
@Data
@TableName("finance_voucher_item")
@EqualsAndHashCode(callSuper = true)
public class VoucherItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 凭证ID
     */
    @TableField("voucher_id")
    private Long voucherId;

    /**
     * 摘要
     */
    @TableField("summary")
    private String summary;

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
     * 借方金额
     */
    @TableField("debit_amount")
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    @TableField("credit_amount")
    private BigDecimal creditAmount;

    /**
     * 来源业务类型
     * purchase/sales/expense/fixed_asset/payment/receipt
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源业务ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 来源业务编号
     */
    @TableField("source_no")
    private String sourceNo;

    /**
     * 所属凭证 (非数据库字段)
     */
    @TableField(exist = false)
    private Voucher voucher;
}
