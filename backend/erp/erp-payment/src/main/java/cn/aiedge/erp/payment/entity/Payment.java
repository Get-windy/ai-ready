package cn.aiedge.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_payment")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String paymentNo;

    private Integer paymentType;

    private Long supplierId;

    private String supplierName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate paymentDate;

    private Integer status;

    private BigDecimal paymentAmount;

    private BigDecimal verifiedAmount;

    private BigDecimal pendingAmount;

    private String paymentMethod;

    private String bankAccount;

    private String bankName;

    private String checkNo;

    private String transactionNo;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long verifiedBy;

    private LocalDateTime verifiedTime;

    private Long completedBy;

    private LocalDateTime completedTime;

    private String remark;

    private String internalNote;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

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