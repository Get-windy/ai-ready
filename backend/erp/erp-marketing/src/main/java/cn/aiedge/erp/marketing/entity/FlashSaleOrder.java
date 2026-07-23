package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀参与/下单记录
 */
@Data
@Accessors(chain = true)
@TableName("mkt_flash_sale_order")
public class FlashSaleOrder {

    /** 参与状态: 0=已参与 1=已下单 2=已取消 */
    public static final int STATUS_JOINED = 0;
    public static final int STATUS_ORDERED = 1;
    public static final int STATUS_CANCELLED = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 秒杀场次ID(mkt_flash_sale.id) */
    private Long sessionId;
    /** 关联订单ID */
    private Long orderId;

    private Long customerId;
    private String customerName;

    private BigDecimal quantity;
    private BigDecimal amount;

    /** 参与状态: 0=已参与 1=已下单 2=已取消 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
