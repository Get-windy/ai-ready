package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单结算信息 (1:1)
 * 结款方式、信用额度、支付信息
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_settlement")
public class SaleOrderSettlement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID (1:1) */
    private Long orderId;

    /** 结款方式 */
    private String settlementMethod;

    // ═══ 信用额度快照 ═══
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private BigDecimal prevDebt;

    // ═══ 收款日/对账日 ═══
    private LocalDate paymentDate;
    private LocalDate reconciliationDate;

    // ═══ 支付信息 ═══
    private Long paymentAccountId;
    private String paymentMethod;
    private Integer paymentStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
