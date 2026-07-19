package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券/兑换码/礼品卡（唯一码管理）
 */
@Data
@Accessors(chain = true)
@TableName("erp_loyalty_coupon")
public class LoyaltyCoupon {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long programId;

    /** 使用人（关联往来单位联系人，NULL=未绑定） */
    private Long partnerId;

    /** 优惠码/兑换码（唯一） */
    private String code;

    /**
     * 状态:
     * UNUSED-未使用
     * USED-已使用
     * EXPIRED-已过期
     * CANCELLED-已取消
     */
    private String status;

    /** 使用时间 */
    private LocalDateTime usedTime;
    /** 关联订单ID */
    private Long usedOrderId;

    /** 面额（礼品卡/电子钱包用） */
    private BigDecimal faceValue;
    /** 当前余额（礼品卡/电子钱包用） */
    private BigDecimal balance;

    /** 有效期 */
    private LocalDateTime expirationDate;

    private String remark;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
