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
