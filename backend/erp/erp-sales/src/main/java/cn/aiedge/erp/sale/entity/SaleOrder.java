package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单实体 - 生产级分布式存储重构版
 * 移除冗余的合计字段，通过明细实时计算
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order")
public class SaleOrder {

    /**
     * 订单ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 订单日期
     */
    private LocalDateTime orderDate;

    /**
     * 预计发货日期
     */
    private LocalDateTime expectedShipDate;

    /**
     * 订单状态（0-草稿 1-待审批 2-已审批 3-部分出库 4-完成 5-取消）
     */
    private Integer status;

    /**
     * 销售类型（1-正常销售 2-样品销售 3-促销销售）
     */
    private Integer saleType;

    // 注意：移除冗余字段 totalAmount, taxAmount, totalAmountWithTax
    // 这些字段将通过明细实时计算，不存储在表头

    /**
     * 已收款金额
     */
    private BigDecimal receivedAmount;

    /**
     * 销售员ID
     */
    private Long salesmanId;

    /**
     * 销售员名称
     */
    private String salesmanName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 收货地址
     */
    private String shippingAddress;

    /**
     * 收货人
     */
    private String receiverName;

    /**
     * 联系电话
     */
    private String receiverPhone;

    /**
     * 收款账户ID
     */
    private Long paymentAccountId;

    /**
     * 物流公司
     */
    private String logisticsCompany;

    /**
     * 物流单号
     */
    private String logisticsNo;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 会员卡号
     */
    private String memberCardNo;

    /**
     * 会员姓名
     */
    private String memberName;

    /**
     * 会员折扣
     */
    private Integer memberDiscount;

    /**
     * 单据备注
     */
    private String orderRemark;

    /**
     * 买家备注
     */
    private String buyerRemark;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单来源（1-内部销售 2-B2B商城 3-B2C零售 4-H5商城 5-小程序）
     */
    private Integer orderSource;

    /**
     * 支付方式（ALIPAY/WECHAT/UNIONPAY/BANK/CASH）
     */
    private String paymentMethod;

    /**
     * 支付状态（0-待支付 1-支付中 2-已支付 3-已退款）
     */
    private Integer paymentStatus;

    /**
     * 发货状态（0-待发货 1-部分发货 2-已发货 3-已签收）
     */
    private Integer deliveryStatus;

    /**
     * 收货人（别名）
     */
    private String consignee;

    /**
     * 收货人电话（别名）
     */
    private String consigneePhone;

    /**
     * 收货地址（详细）
     */
    private String consigneeAddress;

    /**
     * 扩展信息（JSON）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    // ===== 新增客户等级字段 =====

    /**
     * 客户等级代码 - 从 biz_customer_grade 获取并快照
     */
    private String customerGradeCode;

    /**
     * 客户等级名称 - 从 biz_customer_grade 获取并快照
     */
    private String customerGradeName;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

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

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}