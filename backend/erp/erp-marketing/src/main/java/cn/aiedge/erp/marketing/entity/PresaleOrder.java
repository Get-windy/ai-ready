package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 预售参与/下单记录
 */
@Data
@Accessors(chain = true)
@TableName("mkt_presale_order")
public class PresaleOrder {

    /** 参与状态: 0=已付定金 1=已付尾款 2=已取消 */
    public static final int STATUS_DEPOSIT_PAID = 0;
    public static final int STATUS_FINAL_PAID = 1;
    public static final int STATUS_CANCELLED = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 预售活动ID(mkt_presale.id) */
    private Long presaleId;
    /** 关联商城订单ID */
    private Long mallOrderId;

    private Long customerId;

    /** 是否已付定金: 0=否 1=是 */
    private Integer paidDeposit;
    /** 是否已付尾款: 0=否 1=是 */
    private Integer paidFinal;

    /** 参与状态: 0=已付定金 1=已付尾款 2=已取消 */
    private Integer status;

    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
