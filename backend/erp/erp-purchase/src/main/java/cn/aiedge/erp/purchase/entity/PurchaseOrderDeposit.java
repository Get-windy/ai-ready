package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单订金账户 (1:N)
 * 对标 SaleOrderDeposit
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_deposit")
public class PurchaseOrderDeposit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 账户名称 */
    private String accountName;

    /** 账号 */
    private String accountNo;

    /** 金额 */
    private BigDecimal amount;

    /** 已使用金额 */
    private BigDecimal usedAmount;

    /** 状态 */
    private Integer status;

    /** 排序序号 */
    private Integer sequenceNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
