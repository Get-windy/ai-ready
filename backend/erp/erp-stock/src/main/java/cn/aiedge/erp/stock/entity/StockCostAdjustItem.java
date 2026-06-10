package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_cost_adjust_item")
public class StockCostAdjustItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long adjustId;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private Long warehouseId;

    private String batchNo;

    private BigDecimal currentQuantity;

    private BigDecimal oldCost;

    private BigDecimal newCost;

    private BigDecimal diffAmount;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
