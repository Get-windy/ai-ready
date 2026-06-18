package cn.aiedge.erp.purchase.purchaseexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("erp_purchase_exchange")
public class PurchaseExchange {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String exchangeNo;

    private Long originalOrderId;

    private String originalOrderNo;

    private Long supplierId;

    private String supplierName;

    private LocalDateTime exchangeDate;

    private String exchangeReason;

    private Integer exchangeType;

    private Integer status;

    private String remark;

    private BigDecimal totalAmount;

    private Long createdBy;

    private String createdByName;

    private Long approvedBy;

    private String approvedByName;

    private LocalDateTime approvedTime;

    private LocalDateTime completedTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}
