package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

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
     * 科目全名（完整路径，取自科目树，非数据库字段冗余）
     */
    @TableField(exist = false)
    private String subjectFullName;

    /**
     * 明细科目 / 辅助核算项（如往来单位/部门/职员）
     */
    @TableField("detail_subject")
    private String detailSubject;

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
     * 对账标记 0-未对账 1-已对账
     */
    @TableField("reconcile_flag")
    private Integer reconcileFlag = 0;

    /**
     * 核算单位（往来单位核算项）
     */
    @TableField("aux_unit")
    private String auxUnit;

    /**
     * 核算部门
     */
    @TableField("aux_dept")
    private String auxDept;

    /**
     * 核算职员
     */
    @TableField("aux_staff")
    private String auxStaff;

    /**
     * 所属凭证 (非数据库字段)
     */
    @TableField(exist = false)
    private Voucher voucher;

    /**
     * 凭证编号（联查 finance_voucher 带入，非数据库字段）
     */
    @TableField(exist = false)
    private String voucherNo;

    /**
     * 凭证日期（联查 finance_voucher 带入，非数据库字段）
     */
    @TableField(exist = false)
    private LocalDate voucherDate;
}
