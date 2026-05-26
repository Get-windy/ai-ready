package cn.aiedge.erp.sale.return.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_sale_return_item")
public class SaleReturnItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long returnId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long orderItemId;

    private BigDecimal orderQuantity;

    private BigDecimal returnQuantity;

    private BigDecimal acceptedQuantity;

    private BigDecimal rejectedQuantity;

    private BigDecimal unitPrice;

    private BigDecimal originalAmount;

    private BigDecimal returnAmount;

    private BigDecimal refundAmount;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

    private String returnReason;

    private String qualityStatus;

    private String qualityNote;

    private Integer warehouseLocationId;

    private String warehouseLocationCode;

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