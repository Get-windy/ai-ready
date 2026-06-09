package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 智能补货建议
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_replenishment")
public class StockReplenishment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal currentQty;

    private BigDecimal safetyStock;

    private BigDecimal shortageQty;

    private BigDecimal avgDailySales;

    private BigDecimal daysOfStock;

    private Integer leadTime;

    private BigDecimal suggestedQty;

    private String priority;

    private String reason;

    private String status;

    private Long supplierId;

    private String supplierName;

    private String createdOrderNo;

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

    @Version
    private Integer versionNo;
}
