package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class MallOrder extends BaseEntity {

    @TableField("order_no")
    private String orderNo;

    @TableField("customer_id")
    private Long customerId;

    @TableField("customer_name")
    private String customerName;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("order_status")
    private String orderStatus;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("payment_status")
    private String paymentStatus;

    @TableField("delivery_status")
    private String deliveryStatus;

    @TableField("consignee")
    private String consignee;

    @TableField("phone")
    private String phone;

    @TableField("address")
    private String address;

    @TableField("remark")
    private String remark;

    @TableField("source")
    private String source;

    /** 订单明细（非数据库字段） */
    @TableField(exist = false)
    private List<MallOrderItem> orderItems;
}
