package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预付款单-付款账户明细
 * 一单多账户：可填多行不同付款账户（现金/银行/微信等），本单金额=Σ付款金额。
 */
@Data
@Accessors(chain = true)
@TableName("erp_pre_payment_item")
public class PrePaymentItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 预付款单ID */
    private Long prePaymentId;

    /** 行号 */
    private Integer lineNo;

    /** 付款账户编号 */
    private String accountNo;

    /** 付款账户 */
    private String accountName;

    /** 付款金额 */
    private BigDecimal amount;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer versionNo;
}
