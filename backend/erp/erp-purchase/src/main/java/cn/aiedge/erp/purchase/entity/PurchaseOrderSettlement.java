package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单结算信息 (1:1)
 * 对标 SaleOrderSettlement
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_settlement")
public class PurchaseOrderSettlement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 付款方式ID */
    private Long paymentMethodId;

    /** 付款方式名称 */
    private String paymentMethodName;

    /** 结算方式ID */
    private Long settleMethodId;

    /** 结算日期 */
    private LocalDateTime settleDate;

    /** 现金折扣 */
    private String cashDiscount;

    /** 供方要求 */
    private String requireProvide;

    /** 订金账户1（DB 列带下划线数字） */
    @TableField("deposit_account_1")
    private String depositAccount1;

    /** 订金金额1（DB 列带下划线数字） */
    @TableField("deposit_amount_1")
    private BigDecimal depositAmount1;

    /** 更多账户(JSON) */
    private String moreAccounts;

    /** 此前预付 */
    private BigDecimal prevPrepaid;

    /** 预付余额 */
    private BigDecimal prepaidBalance;

    /** 此前欠款 */
    private BigDecimal prevDebt;

    /** 付款期限 */
    private String paymentTerm;

    /** 其他费用 */
    private BigDecimal otherExpense;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
