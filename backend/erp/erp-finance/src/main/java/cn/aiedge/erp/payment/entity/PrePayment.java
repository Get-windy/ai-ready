package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预付款/定金实体
 * 管理预付给供应商的定金、押金、保证金等
 */
@Data
@Accessors(chain = true)
@TableName("erp_pre_payment")
public class PrePayment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String prePaymentNo;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private Long supplierId;

    private String supplierName;

    private BigDecimal amount;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    private String depositType;

    private LocalDate paymentDate;

    private String status;

    private String remark;

    private Integer paymentMethod;

    private String bankAccount;

    private String bankName;

    private String transactionNo;

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
