package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 核销记录实体
 * 记录收/付款单与应收/应付单的核销关系
 */
@Data
@Accessors(chain = true)
@TableName("erp_write_off")
public class WriteOff {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String writeOffNo;

    /** 核销方向: receipt_write_off / payment_write_off */
    private String writeOffType;

    /** 关联收款单 */
    private Long receiptId;
    private String receiptNo;

    /** 关联付款单 */
    private Long paymentId;
    private String paymentNo;

    /** 关联应收单 */
    private Long receivableId;

    /** 关联应付单 */
    private Long payableId;

    /** 客户 */
    private Long customerId;
    private String customerName;

    /** 供应商 */
    private Long supplierId;
    private String supplierName;

    /** 金额 */
    private BigDecimal totalAmount;
    private BigDecimal writeOffAmount;
    private BigDecimal remainingAmount;

    private LocalDate writeOffDate;

    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer versionNo;
}
