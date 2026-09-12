package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 互联账号（资料 → 往来单位 → 互联账号）
 * <p>
 * 行语义 = 「一个互联平台账号 ⇆ 一个往来单位」的绑定关系。
 * 全局基础数据单一口径：营销 / 会员 / 商城互联统一引用本表，禁止另建重复互联账号表。
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("erp_linked_account")
public class LinkedAccount {

    /** 主键：雪花 ID（与资料模块其它档案一致） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 往来单位ID（biz_party.id） */
    private Long partyId;

    /** 往来单位编号（绑定快照） */
    private String partyCode;

    /** 往来单位名称（绑定快照） */
    private String partyName;

    /** 互联平台：WECHAT / ALIPAY / DOUYIN / MALL / OTHER */
    private String platform;

    /** 关联类型：MEMBER / CUSTOMER / OTHER */
    private String linkType;

    /** 互联用户名 */
    private String linkedUserName;

    /** 手机号 */
    private String phone;

    /** 绑定状态：1 已绑定 / 0 已解绑 */
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
