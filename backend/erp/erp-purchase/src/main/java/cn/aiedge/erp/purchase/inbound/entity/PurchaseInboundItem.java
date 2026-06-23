package cn.aiedge.erp.purchase.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_purchase_inbound_item")
public class PurchaseInboundItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long inboundId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal inboundQuantity;

    private BigDecimal pendingQuantity;

    private BigDecimal unitPrice;

    private BigDecimal unitCost;

    private BigDecimal lineAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal lineTotal;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

    private Integer warehouseLocationId;

    private String warehouseLocationCode;

    private String qualityStatus;

    private String qualityNote;

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
}