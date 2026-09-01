package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_assemble_item")
public class StockAssembleItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long assembleId;

    private Long tenantId;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private String barcode;

    private String model;

    private String origin;

    private String brand;

    private String region;

    private String location;

    private BigDecimal itemExtNum1;

    private BigDecimal itemExtNum2;

    private BigDecimal itemExtNum3;

    private String itemExtText1;

    private String itemExtText2;

    private String batchCode;

    private String batchNo;

    private LocalDate productionDate;

    private String shelfLife;

    private LocalDate expiryDate;

    private BigDecimal quantity;

    private String conversionRelation;

    private BigDecimal conversionResult;

    private BigDecimal pieceQuantity;

    private BigDecimal bigPack;

    private BigDecimal midPack;

    private BigDecimal smallPack;

    private String smallUnit;

    private BigDecimal smallUnitQuantity;

    private BigDecimal smallUnitPrice;

    private BigDecimal wholesalePrice;

    private BigDecimal retailPrice;

    private BigDecimal unitPrice;

    private BigDecimal availableStock;

    private BigDecimal availableStockConverted;

    private BigDecimal unitCost;

    private BigDecimal cost;

    private BigDecimal weight;

    private BigDecimal volume;

    private String image;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
