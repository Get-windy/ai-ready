package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "mall_order")
public class MallOrder extends BaseEntity {

    @Column(name = "order_no", length = 64, unique = true, nullable = false)
    private String orderNo;

    @Column(name = "customer_id", length = 64, nullable = false)
    private String customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "pay_amount", precision = 15, scale = 2)
    private BigDecimal payAmount;

    @Column(name = "order_status", length = 30)
    private String orderStatus;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "delivery_status", length = 30)
    private String deliveryStatus;

    @Column(name = "consignee", length = 50)
    private String consignee;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "remark", length = 500)
    private String remark;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @TableField(exist = false)
    private List<MallOrderItem> orderItems;
}
