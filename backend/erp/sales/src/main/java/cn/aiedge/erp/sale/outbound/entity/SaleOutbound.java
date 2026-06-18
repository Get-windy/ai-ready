package cn.aiedge.erp.sale.outbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_sale_outbound")
public class SaleOutbound {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String outboundNo;

    private Long orderId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private LocalDate outboundDate;

    private Integer outboundType;

    private Integer status;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private Long warehouseId;

    private String warehouseName;

    private Long salesPersonId;

    private String salesPersonName;

    private Long departmentId;

    private String departmentName;

    private String shippingAddress;

    private String receiverName;

    private String receiverPhone;

    private String trackingNumber;

    private String logisticsCompany;

    private LocalDateTime expectedShipTime;

    private LocalDateTime actualShipTime;

    private Long pickingBy;

    private LocalDateTime pickingTime;

    private Long packingBy;

    private LocalDateTime packingTime;

    private Long shippedBy;

    private LocalDateTime shippedTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

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