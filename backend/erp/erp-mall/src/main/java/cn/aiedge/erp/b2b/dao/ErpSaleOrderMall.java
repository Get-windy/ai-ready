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

    // ═══════════════════════════════════════════════════════════════════════
    // 商城订单处理页（views/mall/order-process/index.vue；按单据 39 列 / 按明细 47 列）
    // 展示字段。列名严格对齐前端 column.key，库内已有语义同名字段一律复用
    // （见 V11.361.3 迁移头部口径说明），不重复加列。
    // ═══════════════════════════════════════════════════════════════════════

    /** 运费（复用既有列 shipping_fee，同 SaleOrder.shippingFee） */
    @TableField("shipping_fee")
    private BigDecimal freight;

    /** 运费承担方 */
    @TableField("freight_payer")
    private String freightPayer;

    /** 配送方式 */
    @TableField("delivery_method")
    private String deliveryMethod;

    /** 物流公司 */
    @TableField("logistics_company")
    private String logisticsCompany;

    /** 运单号（复用既有列 waybill_no，同 SaleOrder.waybillNo） */
    @TableField("waybill_no")
    private String trackingNo;

    /** 代收金额 */
    @TableField("cod_amount")
    private BigDecimal codAmount;

    /** 预计发货时间 */
    @TableField("expected_ship_time")
    private LocalDateTime expectedShipTime;

    /** 已发数量 */
    @TableField("shipped_quantity")
    private BigDecimal shippedQuantity;

    /** 未发数量 */
    @TableField("unshipped_quantity")
    private BigDecimal unshippedQuantity;

    /** 商品数量（复用既有列 total_quantity，同 SaleOrder.totalQuantity） */
    @TableField("total_quantity")
    private BigDecimal productQuantity;

    /** 仓库名称（发货仓库快照） */
    @TableField("warehouse_name")
    private String warehouseName;

    /** 部门名称（reuse 既有列 dept_name，同 SaleOrder.deptName；按明细「部门」列数据源） */
    @TableField("dept_name")
    private String deptName;

    /** 强制终止（0-否 1-是）；新增列 force_stop */
    @TableField("force_stop")
    private Integer forceStop;

    /** 推广人名称 */
    @TableField("promoter_name")
    private String promoterName;

    /** 经手人名称（复用既有列 salesman_name，同 SaleOrder.salesmanName） */
    @TableField("salesman_name")
    private String handlerName;

    /** 打印次数 */
    @TableField("print_count")
    private Integer printCount;

    /** 记账状态（0-未记账 1-已记账）；新增列 bookkeeping_status */
    @TableField("bookkeeping_status")
    private Integer bookkeepingStatus;

    /** 已结金额 */
    @TableField("settled_amount")
    private BigDecimal settledAmount;

    /** 是否使用优惠券（0-否 1-是）；新增列 coupon_used */
    @TableField("coupon_used")
    private Integer couponUsed;

    /** 审核时间 */
    @TableField("audit_time")
    private LocalDateTime auditTime;

    /** 其他费用 */
    @TableField("other_fee")
    private BigDecimal otherFee;

    /** 表头自定义字段1(数字) */
    @TableField("ext_num1")
    private BigDecimal extNum1;

    /** 表头自定义字段2(数字) */
    @TableField("ext_num2")
    private BigDecimal extNum2;

    /** 表头自定义字段3(文本) */
    @TableField("ext_text3")
    private String extText3;

    /** 表头自定义字段4(文本) */
    @TableField("ext_text4")
    private String extText4;

    /** 表头自定义字段5(文本) */
    @TableField("ext_text5")
    private String extText5;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;
}
