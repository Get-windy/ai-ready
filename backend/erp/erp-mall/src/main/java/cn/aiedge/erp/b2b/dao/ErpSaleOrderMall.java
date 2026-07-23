package cn.aiedge.erp.b2b.dao;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * erp_sale_order 表映射（商城订单视图）
 * 替代 mall_order 表，消除数据冗余
 * order_source=2 为企业客户商城订单，order_source=3 为个人会员商城订单
 */
@Data
@TableName("erp_sale_order")
public class ErpSaleOrderMall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private LocalDateTime orderDate;

    /** 订单状态: 0草稿,1待审批,2已审批,3部分出库,4完成,5交易完成,6已取消 */
    private Integer status;

    /** 订单来源: 2=企业客户商城 3=个人会员商城 */
    private Integer orderSource;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 已收款金额 */
    private BigDecimal receivedAmount;

    /** 支付方式: ALIPAY, WECHAT, UNIONPAY, BANK, CASH */
    private String paymentMethod;

    /** 支付状态: 0待支付,1支付中,2已支付,3部分支付,4已退款 */
    private Integer paymentStatus;

    /** 发货状态: 0待发货,1部分发货,2已发货,3已签收 */
    private Integer deliveryStatus;

    /** 收货人 */
    private String consignee;

    /** 收货人电话 */
    private String consigneePhone;

    /** 收货详细地址 */
    private String consigneeAddress;

    /** 收货地址（冗余合并） */
    private String shippingAddress;

    /** 单据备注 */
    private String orderRemark;

    /** 买家备注 */
    private String buyerRemark;

    /** 备注 */
    private String remark;

    /** 扩展信息（JSON，存储原始商城状态等） */
    private String extInfo;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;
}
