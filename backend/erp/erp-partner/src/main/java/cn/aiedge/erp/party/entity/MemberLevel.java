package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员级别（营销权益等级）
 * <p>
 * 与「客户级别」（价格等级，决定拿货价）是两套独立体系：会员级别用于会员卡的权益/折扣，
 * 挂在往来单位的会员卡上（biz_party.member_level）。
 * </p>
 */
@Getter
@Setter
@TableName("erp_member_level")
public class MemberLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 级别名称 */
    private String levelName;

    /** 会员折扣率（100 = 不打折） */
    private BigDecimal discountRate;

    private Integer sortOrder;

    private Integer status;

    private String remark;

    // ── 会员等级规则（V11.381.0）：门槛 / 保级周期 / 默认等级 ──
    /** 升级门槛：累计消费额达到该值即升到本级（NULL=不按消费额） */
    private BigDecimal upgradeAmount;
    /** 升级门槛：成长值/累计积分达到该值即升到本级（NULL=不按积分） */
    private Integer upgradePoints;
    /** 保级周期（月，NULL=永久保级） */
    private Integer keepMonths;
    /** 是否默认等级（1=新会员初始等级，全租户唯一） */
    private Integer isDefault;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
