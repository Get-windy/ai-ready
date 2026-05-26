package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_transfer_item")
public class StockTransferItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long transferId;

    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal planQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal unitCost;

    private BigDecimal lineAmount;

    private String batchNo;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

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