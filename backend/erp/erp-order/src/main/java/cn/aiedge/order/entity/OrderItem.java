package cn.aiedge.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_order_item")
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品编码 */
    private String productCode;

    /** 规格型号 */
    private String specModel;

    /** 单位 */
    private String unit;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private BigDecimal quantity;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 小计金额 */
    private BigDecimal subtotal;

    /** 备注 */
    private String remark;

    /** 是否删除 */
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
