package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 盘点单明细
 * 库存数量=账面库存，盘点数量=实盘数，盈亏数量=盘点数量-库存数量
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock_take_item")
public class StockTakeItem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long stockTakeId;
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
    private String image;
    /** 库存数量（账面库存） */
    private BigDecimal stockQuantity;
    /** 换算结果 */
    private BigDecimal conversionResult;
    /** 盘点数量 */
    private BigDecimal checkQuantity;
    /** 盘点数量换算结果 */
    private BigDecimal checkQuantityConversionResult;
    /** 件散数量 */
    private BigDecimal pieceQuantity;
    /** 盈亏数量 = 盘点数量 - 库存数量 */
    private BigDecimal diffQuantity;
    private LocalDate productionDate;
    /** 换算关系 */
    private String conversionRelation;
    private String shelfLife;
    private LocalDate expiryDate;
    private String batchCode;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;
    /** 盘点状态：1 未盘，2 已盘，3 已确认 */
    private Integer checkStatus;
    /** 成本单价 */
    private BigDecimal costPrice;
    /** 盈亏换算结果 */
    private BigDecimal diffConversionResult;
    /** 盈亏金额 */
    private BigDecimal diffAmount;
    /** 单据自定义1~3(数字) */
    private BigDecimal itemExtNum1;
    private BigDecimal itemExtNum2;
    private BigDecimal itemExtNum3;
    /** 单据自定义4~5(文本) */
    private String itemExtText1;
    private String itemExtText2;
    private String remark;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
