package cn.aiedge.erp.sale.returnDoc.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售退货单明细
 *
 * <p>对应文档 67 个明细列字段，按业务分组：</p>
 * <ul>
 *   <li>商品基本信息：productId, productCode, productName, barcode, specification,
 *       productSpec, productUnit, imageUrl, storageLocation, area, modelNo,
 *       originPlace, brand</li>
 *   <li>库存信息：availableStock, availableStockConverted, bookStock</li>
 *   <li>批次信息：batchBarcode, productionDate, shelfLife, expiryDate</li>
 *   <li>包装/数量：returnQuantity, conversionRelation, pieceQuantity,
 *       bigPack, midPack, smallPack</li>
 *   <li>价格体系：lastSaleDate, lastSalePrice, retailPrice, wholesalePrice,
 *       minSalePrice, unitPrice, lineAmount</li>
 *   <li>小单位：smallUnit, smallUnitPrice, smallUnitQuantity, conversionResult</li>
 *   <li>成本：refCostPrice, refCostAmount</li>
 *   <li>折扣：discountRate, discountedPrice, discountedAmount</li>
 *   <li>积分/礼品：exchangeGift, exchangePoints, generatedPoints, usedPoints</li>
 *   <li>价格等级(8个标准化)：priceLevel1-8</li>
 *   <li>物理属性：weight, volume</li>
 *   <li>行属性：isGift, productLineAttr, receivedQuantity,
 *       terminatedQuantity, terminatedAmount</li>
 *   <li>单据自定义(行)：extNum1-7, extText1-2, extPartner, extStaff, extDept</li>
 * </ul>
 */
@Data
@TableName("erp_sale_return_doc_item")
public class SaleReturnDocItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long returnDocId;

    private Integer lineNo;

    // 商品基本信息
    private Long productId;
    private String productCode;
    private String productName;
    private String barcode;
    private String specification;
    private String productSpec;
    private String productUnit;
    private String unit;
    private String imageUrl;
    private String storageLocation;
    private String area;
    private String modelNo;
    private String originPlace;
    private String brand;

    // 库存信息
    private BigDecimal availableStock;
    private BigDecimal availableStockConverted;
    private BigDecimal bookStock;

    // 批次信息
    private String batchBarcode;
    private LocalDateTime productionDate;
    private String shelfLife;
    private LocalDateTime expiryDate;

    // 包装/数量
    private BigDecimal returnQuantity;
    private String conversionRelation;
    private BigDecimal pieceQuantity;
    private BigDecimal bigPack;
    private BigDecimal midPack;
    private BigDecimal smallPack;

    // 价格体系
    private LocalDateTime lastSaleDate;
    private BigDecimal lastSalePrice;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal minSalePrice;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;

    // 小单位
    private String smallUnit;
    private BigDecimal smallUnitPrice;
    private BigDecimal smallUnitQuantity;
    private BigDecimal conversionResult;

    // 成本
    private BigDecimal refCostPrice;
    private BigDecimal refCostAmount;

    // 折扣
    private BigDecimal discountRate;
    private BigDecimal discountedPrice;
    private BigDecimal discountedAmount;

    // 积分/礼品
    private String exchangeGift;
    private BigDecimal exchangePoints;
    private BigDecimal generatedPoints;
    private BigDecimal usedPoints;

    // 价格等级(8个标准化产品价格等级)
    // 50:餐饮店 51:食堂团餐 52:外围餐饮店 53:自助vip
    // 54:大团餐 55:重点|vip01 56:连锁|vip 57:特价客户
    private BigDecimal priceLevel1;
    private BigDecimal priceLevel2;
    private BigDecimal priceLevel3;
    private BigDecimal priceLevel4;
    private BigDecimal priceLevel5;
    private BigDecimal priceLevel6;
    private BigDecimal priceLevel7;
    private BigDecimal priceLevel8;

    // 物理属性
    private BigDecimal weight;
    private BigDecimal volume;

    // 行属性
    private Boolean isGift;
    private String productLineAttr;
    private BigDecimal receivedQuantity;
    private BigDecimal terminatedQuantity;
    private BigDecimal terminatedAmount;

    // 备注
    private String remark;
    private String itemRemark;

    // 单据自定义字段（数字1-7）
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private BigDecimal extNum6;
    private BigDecimal extNum7;

    // 单据自定义字段（文本1-2）
    private String extText1;
    private String extText2;

    // 单据自定义字段（往来单位/职员/部门）
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    private Integer sort;

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
