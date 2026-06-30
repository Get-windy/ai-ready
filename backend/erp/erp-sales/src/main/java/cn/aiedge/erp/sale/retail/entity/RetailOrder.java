package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 零售单主表
 * <p>
 * 行业设计：B2B+B2C统一往来单位模型
 * - customer_id → biz_party.id（party_type=1, party_level='MEMBER' 或 'ENTERPRISE'）
 * - 默认散客：系统预置的 WALKIN 会员（biz_party.party_code='WALKIN'）
 * - 有会员时：customer_id 指向真实的个人会员 party
 * </p>
 */
@Getter
@Setter
@TableName("erp_retail_order")
public class RetailOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 零售单号 */
    private String retailNo;

    /** 客户/会员ID → biz_party.id */
    private Long customerId;

    /** 客户名称（冗余，方便查询） */
    private String customerName;

    /** 商品总金额（优惠前） */
    private BigDecimal amount;

    /** 支付方式：CASH/ALIPAY/WECHAT/CARD/PREPAID/TRANSFER/MIXED */
    private String paymentMethod;

    /** 状态：0=草稿 1=已完成 2=已挂单 3=已作废 */
    private Integer status;

    private String remark;

    // ── 仓库 & 经手人 ──
    private Long warehouseId;
    private String warehouseName;
    private Long handlerId;
    private String handlerName;

    // ── 单据日期 & 销售类型 ──
    private LocalDate orderDate;
    private String saleType;   // NORMAL / RETURN

    // ── 会员信息 ──
    private String memberCardNo;
    private String memberName;

    // ── 优惠 ──
    private BigDecimal directDiscount;   // 直接优惠（手动折扣）
    private BigDecimal couponDiscount;   // 优惠券抵扣
    private BigDecimal promoDiscount;    // 促销优惠（活动折扣）

    // ── 积分 ──
    private Integer prevPoints;          // 此前积分

    // ── 应收与支付明细 ──
    private BigDecimal payableAmount;    // 应付金额
    private BigDecimal cashAmount;       // 现金支付
    private BigDecimal cardAmount;       // 银行卡支付
    private BigDecimal prepaidAmount;    // 预收款抵扣
    private BigDecimal transferAmount;   // 转账支付
    private Boolean  combinedPayment;    // 是否组合支付
    private BigDecimal changeAmount;     // 找零
    private BigDecimal prepaidBalance;   // 预收余额

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
