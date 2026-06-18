package cn.aiedge.erp.purchase.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_purchase_inbound")
public class PurchaseInbound {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String inboundNo;

    private Long orderId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private Long contractId;

    private String contractNo;

    private LocalDate inboundDate;

    private Integer inboundType;

    private Integer status;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmountWithTax;

    private Long warehouseId;

    private String warehouseName;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private String trackingNumber;

    private String logisticsCompany;

    private LocalDateTime expectedArrivalTime;

    private LocalDateTime actualArrivalTime;

    private Long receivedBy;

    private LocalDateTime receivedTime;

    private Long qualityCheckedBy;

    private LocalDateTime qualityCheckedTime;

    private String qualityCheckResult;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long warehouseConfirmedBy;

    private LocalDateTime warehouseConfirmedTime;

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