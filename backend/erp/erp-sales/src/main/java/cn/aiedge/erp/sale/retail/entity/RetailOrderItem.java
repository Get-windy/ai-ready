package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 零售单明细行
 */
@Getter
@Setter
@TableName("erp_retail_order_item")
public class RetailOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属零售单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 货号 */
    private String itemCode;

    /** 条码 */
    private String barcode;

    /** 计价单位 */
    private String unit;

    /** 可用库存 */
    private BigDecimal availableStock;

    /** 批次号 */
    private String batchCode;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 保质期 */
    private String shelfLife;

    /** 到期日期 */
    private LocalDate expiryDate;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 = quantity × unitPrice */
    private BigDecimal amount;

    /** 大包装数 */
    private Integer bigPack;

    /** 中包装数 */
    private Integer midPack;

    /** 小包装数 */
    private Integer smallPack;

    /** 备注 */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
