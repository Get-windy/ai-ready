package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户忠诚卡/积分账户（对标 Odoo loyalty.card）
 */
@Data
@Accessors(chain = true)
@TableName("erp_loyalty_card")
public class LoyaltyCard {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long programId;

    /** 关联往来单位联系人ID */
    private Long partnerId;

    /** 卡号/会员卡编号 */
    private String cardCode;

    /** 当前积分/余额 */
    private BigDecimal points;
    /** 累计获得 */
    private BigDecimal totalEarned;
    /** 累计兑换 */
    private BigDecimal totalRedeemed;

    /** 积分/余额有效期 */
    private LocalDateTime expirationDate;

    private Integer isActive;
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
