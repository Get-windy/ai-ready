package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_item")
public class SaleOrderItem {

    /**
     * 明细ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 规格型号
     */
    private String specification;

    /**
     * 单位
     */
    private String unit;

    /**
     * 订购数量
     */
    private BigDecimal quantity;

    /**
     * 已出库数量
     */
    private BigDecimal shippedQuantity;

    /**
     * 单价（不含税）
     */
    private BigDecimal unitPrice;

    /**
     * 税率（%）
     */
    private BigDecimal taxRate;

    /**
     * 含税单价
     */
    private BigDecimal unitPriceWithTax;

    /**
     * 金额（不含税）
     */
    private BigDecimal amount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 金额（含税）
     */
    private BigDecimal amountWithTax;

    /**
     * 折扣率（%）
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}