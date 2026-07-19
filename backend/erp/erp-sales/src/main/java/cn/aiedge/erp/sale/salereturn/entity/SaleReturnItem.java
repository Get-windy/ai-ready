package cn.aiedge.erp.sale.salereturn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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

    private String barcode;

    private String specification;

    private String productSpec;

    private String productUnit;

    private String unit;

    private String conversionRelation;

    private BigDecimal availableStock;

    private BigDecimal returnQuantity;

    private BigDecimal pieceQuantity;

    private BigDecimal bigPack;

    private BigDecimal midPack;

    private BigDecimal smallPack;

    private BigDecimal unitPrice;

    private BigDecimal lineAmount;

    private BigDecimal taxRate;

    private String exchangeGift;

    private BigDecimal exchangePoints;

    private String reason;

    private String remark;

    // 产品图片
    private String imageUrl;

    // 区域/型号/产地/品牌
    private String area;

    private String modelNo;

    private String originPlace;

    private String brand;

    // 库存扩展
    private BigDecimal availableStockConverted;

    private BigDecimal bookStock;

    // 价格体系
    private LocalDateTime lastSaleDate;

    private BigDecimal lastSalePrice;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    private BigDecimal minSalePrice;

    // 小单位
    private String smallUnit;

    private BigDecimal smallUnitPrice;

    private BigDecimal smallUnitQuantity;

    private BigDecimal conversionResult;

    // 折扣
    private BigDecimal discountRate;

    private BigDecimal discountedPrice;

    private BigDecimal discountedAmount;

    // 成本
    private BigDecimal refCostPrice;

    private BigDecimal refCostAmount;

    // 物理属性
    private BigDecimal weight;

    private BigDecimal volume;

    // 收货/终止
    private BigDecimal receivedQuantity;

    private BigDecimal terminatedQuantity;

    private BigDecimal terminatedAmount;

    // 行属性
    private Boolean isGift;

    private String productLineAttr;

    // 明细备注
    private String itemRemark;

    // 价格等级（8个标准化产品价格等级）
    private BigDecimal priceLevel1;

    private BigDecimal priceLevel2;

    private BigDecimal priceLevel3;

    private BigDecimal priceLevel4;

    private BigDecimal priceLevel5;

    private BigDecimal priceLevel6;

    private BigDecimal priceLevel7;

    private BigDecimal priceLevel8;

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
