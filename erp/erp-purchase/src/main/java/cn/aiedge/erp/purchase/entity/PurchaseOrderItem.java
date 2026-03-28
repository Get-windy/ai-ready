package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单明细实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_item")
public class PurchaseOrderItem {

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
     * 租户ID
     */
    private Long tenantId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
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
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 已入库数量
     */
    private BigDecimal receivedQuantity;

    /**
     * 单价（不含税）
     */
    private BigDecimal price;

    /**
     * 税率（%）
     */
    private BigDecimal taxRate;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 行金额（含税）
     */
    private BigDecimal lineAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序号
     */
    private Integer sortOrder;

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