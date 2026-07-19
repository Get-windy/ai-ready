package cn.aiedge.erp.sale.retail.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 零售单支付明细实体
 * 组合支付时每种支付方式一行
 */
@Data
@Accessors(chain = true)
@TableName("erp_retail_order_payment")
public class RetailOrderPayment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 所属零售单ID */
    private Long orderId;

    /** 支付方式：CASH/CARD/PREPAID/TRANSFER/ALIPAY/WECHAT/AGGREGATE/ABC/CCB/JD */
    private String paymentMethod;

    /** 支付金额 */
    private BigDecimal paymentAmount;

    /** 收款账户 */
    private String paymentAccount;

    /** 交易流水号 */
    private String transactionNo;

    /** 支付时间 */
    private LocalDateTime paymentTime;

    /** 备注 */
    private String remark;

    // ═══ 系统字段 ═══
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
