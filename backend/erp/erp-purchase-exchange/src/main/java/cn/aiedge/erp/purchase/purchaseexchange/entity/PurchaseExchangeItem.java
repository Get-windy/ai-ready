package cn.aiedge.erp.purchase.purchaseexchange.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("erp_purchase_exchange_item")
public class PurchaseExchangeItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long exchangeId;

    private Long originalItemId;

    private Long productId;

    private String productName;

    private String productCode;

    private String productSpec;

    private BigDecimal originalQuantity;

    private BigDecimal exchangeQuantity;

    private BigDecimal originalPrice;

    private BigDecimal exchangePrice;

    private String unit;

    private String batchNo;

    private Long warehouseId;

    private String warehouseName;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
