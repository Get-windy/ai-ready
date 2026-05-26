package cn.aiedge.erp.sale.return.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_sale_return")
public class SaleReturn {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String returnNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate returnDate;

    private Integer returnType;

    private String returnReason;

    private Integer status;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private BigDecimal refundAmount;

    private BigDecimal refundRate;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long receivedBy;

    private LocalDateTime receivedTime;

    private Long warehouseConfirmedBy;

    private LocalDateTime warehouseConfirmedTime;

    private Long refundProcessedBy;

    private LocalDateTime refundProcessedTime;

    private String refundMethod;

    private String refundAccount;

    private String refundNote;

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