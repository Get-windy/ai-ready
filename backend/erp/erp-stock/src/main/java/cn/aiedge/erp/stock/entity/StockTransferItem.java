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

    private String image;

    private String model;

    private String origin;

    private String brand;

    private String region;

    private String locationOut;

    private String locationIn;

    private BigDecimal planQuantity;

    private BigDecimal actualQuantity;

    /** 调拨数量 */
    private BigDecimal quantity;

    /** 成本单价 */
    private BigDecimal unitCost;

    /** 成本金额 */
    private BigDecimal costAmount;

    private BigDecimal unitPrice;

    private Integer status;

    private BigDecimal lineAmount;

    private String batchNo;

    /** 批次条码 */
    private String batchCode;

    private LocalDateTime productionDate;

    private LocalDateTime validityDate;

    /** 保质期 */
    private String shelfLife;

    private String barcode;

    private String conversionRelation;

    private BigDecimal conversionResult;

    private BigDecimal pieceQuantity;

    private BigDecimal bigPack;

    private BigDecimal midPack;

    private BigDecimal smallPack;

    private String smallUnit;

    private BigDecimal smallUnitPrice;

    private BigDecimal smallUnitQuantity;

    /** 可用库存 */
    private BigDecimal availableStock;

    private BigDecimal availableStockConverted;

    /** 账面库存 */
    private BigDecimal bookStock;

    /** 调拨单价 */
    private BigDecimal transferPrice;

    /** 调拨金额 */
    private BigDecimal transferAmount;

    /** 调拨差额 */
    private BigDecimal transferDiff;

    private BigDecimal weight;

    private BigDecimal volume;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    /** 表体自定义1~3(数字) / 4~5(文本) */
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private String extText1;
    private String extText2;

    /** 单据自定义1~3(数字) / 4~5(文本) */
    private BigDecimal docCustom1;
    private BigDecimal docCustom2;
    private BigDecimal docCustom3;
    private String docCustom4;
    private String docCustom5;

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
