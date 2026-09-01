package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_stock_bom_item")
public class StockBomItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long bomId;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpec;

    private String productUnit;

    private BigDecimal quantity;

    private BigDecimal unitCost;

    private BigDecimal cost;

    /** 损耗率（生产模板/组装拆分配套） */
    private BigDecimal wastageRate;

    /** 图片 */
    private String imageUrl;

    /** 型号 */
    private String model;

    /** 产地 */
    private String origin;

    /** 品牌 */
    private String brand;

    /** 条码 */
    private String barcode;

    /** 小单位（辅助计量单位） */
    private String smallUnit;

    /** 小单位数量 */
    private BigDecimal smallUnitQty;

    /** 小单位单价 */
    private BigDecimal smallUnitPrice;

    /** 单据自定义1(数字) */
    private BigDecimal extNum1;

    /** 单据自定义2(数字) */
    private BigDecimal extNum2;

    /** 单据自定义3(数字) */
    private BigDecimal extNum3;

    /** 单据自定义4(文本) */
    private String extText4;

    /** 单据自定义5(文本) */
    private String extText5;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
