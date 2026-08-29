package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_damage_item")
public class StockDamageItem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long damageId;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private String productUnit;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate validityDate;
    private String barcode;
    private String location;
    private String shelfLife;
    private String conversionRelation;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
