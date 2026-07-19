package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 佣金记录（分佣/裂变）
 */
@Data
@Accessors(chain = true)
@TableName("erp_commission_record")
public class CommissionRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 佣金归属的联系人（分销员/推荐人） */
    private Long partnerId;

    /** 关联的订单ID */
    private Long orderId;
    /** 订单金额 */
    private BigDecimal orderAmount;
    /** 佣金比例(%) */
    private BigDecimal commissionRate;
    /** 佣金金额 */
    private BigDecimal commissionAmount;

    /** 层级：1=一级, 2=二级, 3=三级 */
    private Integer tierLevel;
    /** 直接推荐人ID（多级分佣链路中的上一级） */
    private Long referrerId;

    /**
     * 状态:
     * DRAFT-待确认
     * CONFIRMED-已确认
     * PAID-已结算
     * CANCELLED-已取消
     */
    private String status;

    /** 确认时间 */
    private LocalDateTime confirmTime;
    /** 结算时间 */
    private LocalDateTime payTime;

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
