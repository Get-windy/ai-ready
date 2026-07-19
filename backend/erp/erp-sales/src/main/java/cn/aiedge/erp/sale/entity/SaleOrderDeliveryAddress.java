package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 销售订单收货地址 (1:N)
 * 支持收货/提货多种地址类型
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_delivery_address")
public class SaleOrderDeliveryAddress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 地址类型: RECEIVER(收货) / PICKUP(提货) */
    private String addressType;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 地址 */
    private String address;

    /** 是否默认 */
    private Boolean isDefault;

    /** 排序 */
    private Integer sequence;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
