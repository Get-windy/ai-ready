package cn.aiedge.erp.b2b.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城用户-企业身份关联（申请审批制）
 * <p>
 * 状态流转：
 * 0(待企业审批) → 1(待租户审批) → 2(已通过)
 * 任意阶段 → 3(已驳回)
 * 2(已通过) → 4(已撤销)
 * </p>
 */
@Data
@TableName("shop_user_party_link")
public class ShopUserPartyLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商城用户 ID（shop_user.id） */
    private Long shopUserId;

    /** 企业客户 ID（biz_party.id） */
    private Long partyId;

    /**
     * 状态: 0=待企业审批 1=待租户审批 2=已通过 3=已驳回 4=已撤销
     */
    private Integer status;

    /** 申请时使用的手机号 */
    private String appliedPhone;

    // ── 企业客户管理员审批 ──

    /** 企业审批人 shop_user.id（该企业的 is_primary 联系人） */
    private Long enterpriseApprovedBy;

    private LocalDateTime enterpriseApprovedTime;

    private String enterpriseRemark;

    // ── 租户管理员审批 ──

    private Long tenantApprovedBy;

    private LocalDateTime tenantApprovedTime;

    private String tenantRemark;

    // ── 撤销 ──

    /** 撤销人 shop_user.id 或后台管理员 user_id */
    private Long revokedBy;

    private LocalDateTime revokedTime;

    private String revokeReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    // ── 状态常量 ──

    /** 待企业客户管理员审批 */
    public static final int STATUS_PENDING_ENTERPRISE = 0;
    /** 待租户管理员审批 */
    public static final int STATUS_PENDING_TENANT = 1;
    /** 已通过，可正常使用企业身份 */
    public static final int STATUS_APPROVED = 2;
    /** 已驳回 */
    public static final int STATUS_REJECTED = 3;
    /** 已撤销（管理员或企业主动解除关联） */
    public static final int STATUS_REVOKED = 4;
}
