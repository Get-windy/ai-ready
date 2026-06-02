package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账凭证实体
 * 记录企业经济业务，作为登记账簿的依据
 */
@Data
@TableName("finance_voucher")
@EqualsAndHashCode(callSuper = true)
public class Voucher extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 凭证编号
     */
    @TableField("voucher_no")
    private String voucherNo;

    /**
     * 凭证日期
     */
    @TableField("voucher_date")
    private LocalDate voucherDate;

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
     * 附件数量
     */
    @TableField("attachments")
    private Integer attachments = 0;

    /**
     * 制单人
     */
    @TableField("prep_by")
    private String prepBy;

    /**
     * 制单时间
     */
    @TableField("prep_at")
    private LocalDateTime prepAt;

    /**
     * 审核人
     */
    @TableField("audit_by")
    private String auditBy;

    /**
     * 审核时间
     */
    @TableField("audit_at")
    private LocalDateTime auditAt;

    /**
     * 过账人
     */
    @TableField("post_by")
    private String postBy;

    /**
     * 过账时间
     */
    @TableField("post_at")
    private LocalDateTime postAt;

    /**
     * 凭证状态
     * draft-草稿 audited-已审核 posted-已过账 reversed-已冲销
     */
    @TableField("status")
    private String status = "draft";

    /**
     * 借方总金额
     */
    @TableField("total_debit")
    private BigDecimal totalDebit;

    /**
     * 贷方总金额
     */
    @TableField("total_credit")
    private BigDecimal totalCredit;

    /**
     * 凭证明细行 (非数据库字段)
     */
    @TableField(exist = false)
    private List<VoucherItem> items;
}
