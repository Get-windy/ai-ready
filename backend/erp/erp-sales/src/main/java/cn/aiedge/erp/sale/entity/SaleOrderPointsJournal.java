package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单会员积分流水 (1:1)
 * 记录本单引起的积分变动
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_points_journal")
public class SaleOrderPointsJournal {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID (1:1) */
    private Long orderId;

    /** 会员卡号 */
    private String memberCardNo;

    /** 会员姓名 */
    private String memberName;

    /** 会员折扣 */
    private Integer memberDiscount;

    // ═══ 积分变动 ═══
    private BigDecimal prevPoints;
    private BigDecimal salePoints;
    private BigDecimal returnPoints;
    private BigDecimal exchangePoints;
    private BigDecimal usedPoints;
    private BigDecimal currentPoints;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
