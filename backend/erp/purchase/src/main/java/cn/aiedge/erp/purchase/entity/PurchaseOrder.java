package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单实体（遵循金蝶K3标准）
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal discountAmount;

    private BigDecimal paidAmount;

    private Integer status;

    private Integer approvalStatus;

    private Long approvalUserId;

    private LocalDateTime approvalTime;

    private Long warehouseId;

    private String paymentMethod;

    private Integer paymentStatus;

    private Integer deliveryStatus;

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

    private Long purchaserId;

    private String purchaserName;

    private Long deptId;

    private Long currencyId;

    private BigDecimal exchangeRate;

    private BigDecimal totalAmountWithTax;

    private BigDecimal totalQuantity;

    private BigDecimal receivedAmount;

    private BigDecimal fulfillmentPercent;

    private Long contractId;

    private Integer sourceType;

    private Long sourceId;

    private String sourceBillNo;

    private Integer childrenFlag;

    private String saleOrderNo;

    private Long multiCheckLevel1;

    private Long multiCheckLevel2;

    private Long multiCheckLevel3;

    private Long multiCheckLevel4;

    private Long multiCheckLevel5;

    private Long multiCheckLevel6;

    private LocalDateTime multiCheckDate1;

    private LocalDateTime multiCheckDate2;

    private LocalDateTime multiCheckDate3;

    private LocalDateTime multiCheckDate4;

    private LocalDateTime multiCheckDate5;

    private LocalDateTime multiCheckDate6;

    private Integer curCheckLevel;

    private Integer closedFlag;

    private Integer cancellationFlag;

    private Integer tranStatus;

    private Integer orderAffirm;

    private Long paymentMethodId;

    private String requireProvide;

    private String cashDiscount;

    private LocalDateTime settleDate;

    private Long settleMethodId;

    private String deliveryAddress;

    private LocalDateTime lastModifyDate;

    private Boolean supplierConfirmed;

    private LocalDateTime supplierConfirmTime;

    private Boolean shipped;

    private LocalDateTime shipTime;

    private String trackingNumber;

    private LocalDateTime estimatedArrivalTime;

    private Boolean received;

    private LocalDateTime receiveTime;

    private BigDecimal receivedQuantity;

    private String qualityCheckResult;

    private Integer invoiceStatus;

    private String invoiceNumber;

    private BigDecimal invoiceAmount;

    private LocalDateTime invoiceDate;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;
}