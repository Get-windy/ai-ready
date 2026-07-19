package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单订金账户 (1:N)
 * 替代原 depositAccount, depositAccount1-4 重复组
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_deposit")
public class SaleOrderDeposit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 账户名称 */
    private String accountName;

    /** 金额 */
    private BigDecimal amount;

    /** 排序 */
    private Integer sequence;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
