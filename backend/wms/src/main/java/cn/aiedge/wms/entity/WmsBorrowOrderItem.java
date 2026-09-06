package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_borrow_order_item")
@Schema(description = "借进借出单明细")
public class WmsBorrowOrderItem extends BaseEntity {
    @Schema(description = "关联借进借出单ID")
    private Long orderId;
    @Schema(description = "行号")
    private Integer lineNo;
    @Schema(description = "商品ID")
    private Long productId;
    @Schema(description = "商品编码")
    private String productCode;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "商品规格")
    private String productSpec;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "借进/借出数量")
    private BigDecimal quantity;
    @Schema(description = "已归还数量")
    private BigDecimal returnedQuantity;
    @Schema(description = "单价")
    private BigDecimal price;
    @Schema(description = "金额")
    private BigDecimal amount;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "条码")
    private String barcode;
    @Schema(description = "型号")
    private String model;
    @Schema(description = "产地")
    private String origin;
    @Schema(description = "区域")
    private String region;
    @Schema(description = "货位")
    private String location;
    @Schema(description = "口味")
    private String taste;
    @Schema(description = "生产日期")
    private LocalDate productionDate;
    @Schema(description = "保质期")
    private String shelfLife;
    @Schema(description = "到期日期")
    private LocalDate expiryDate;
    @Schema(description = "批次条码")
    private String batchCode;
    @Schema(description = "换算关系")
    private String conversionRelation;
    @Schema(description = "换算结果")
    private BigDecimal conversionResult;
    @Schema(description = "件散数量")
    private BigDecimal pieceQuantity;
    @Schema(description = "大包装")
    private BigDecimal bigPack;
    @Schema(description = "中包装")
    private BigDecimal midPack;
    @Schema(description = "小包装")
    private BigDecimal smallPack;
    @Schema(description = "小单位")
    private String smallUnit;
    @Schema(description = "小单位数量")
    private BigDecimal smallUnitQuantity;
    @Schema(description = "小单位单价")
    private BigDecimal smallUnitPrice;
    @Schema(description = "零售价")
    private BigDecimal retailPrice;
    @Schema(description = "批发价")
    private BigDecimal wholesalePrice;
    @Schema(description = "最低售价")
    private BigDecimal minPrice;
    @Schema(description = "可用库存")
    private BigDecimal availableStock;
    @Schema(description = "账面库存")
    private BigDecimal bookStock;
    @Schema(description = "重量（kg）")
    private BigDecimal weight;
    @Schema(description = "体积（m³）")
    private BigDecimal volume;
    @Schema(description = "已处理数量-还出")
    private BigDecimal processedReturnQuantity;
    @Schema(description = "已处理数量-借转采购")
    private BigDecimal processedPurchaseQuantity;
    @Schema(description = "未处理数量")
    private BigDecimal nonProcessedQuantity;
    @Schema(description = "未处理金额")
    private BigDecimal nonProcessedAmount;
    @Schema(description = "价格等级1")
    private BigDecimal priceLevel1;
    @Schema(description = "价格等级2")
    private BigDecimal priceLevel2;
    @Schema(description = "价格等级3")
    private BigDecimal priceLevel3;
    @Schema(description = "价格等级4")
    private BigDecimal priceLevel4;
    @Schema(description = "价格等级5")
    private BigDecimal priceLevel5;
    @Schema(description = "价格等级6")
    private BigDecimal priceLevel6;
    @Schema(description = "价格等级7")
    private BigDecimal priceLevel7;
    @Schema(description = "价格等级8")
    private BigDecimal priceLevel8;
    @Schema(description = "表体自定义1(数字)")
    private BigDecimal itemExtNum1;
    @Schema(description = "表体自定义2(数字)")
    private BigDecimal itemExtNum2;
    @Schema(description = "表体自定义3(数字)")
    private BigDecimal itemExtNum3;
    @Schema(description = "表体自定义4(文本)")
    private String itemExtText1;
    @Schema(description = "表体自定义5(文本)")
    private String itemExtText2;
}
