package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class UserInfo {
    private String id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private String level;
    private int points;
    private double balance;

    // ── 身份体系字段 ──

    /** 用户身份类型：ENTERPRISE=企业客户，MEMBER=个人会员 */
    private String userType;

    /** 关联往来单位ID → biz_party.id */
    private Long partyId;

    /** 往来单位名称（企业名 或 会员姓名） */
    private String partyName;

    /** 公司名称（仅 ENTERPRISE 类型有值） */
    private String companyName;

    /** 会员卡号（仅 MEMBER 类型有值） */
    private String memberCardNo;

    /**
     * 当前身份在**本店**的准入审核状态（`shop_user_tenant.status`）：
     * 0=待审核 / 1=已通过 / 2=已驳回 / 3=已解除；无关联行时为 null。
     *
     * <p>2026-09-26 新增：C 端「价格三态」要靠它区分**待认证**态 ——
     * 已登录但未过审的买家，按《商城App设计方案》§七应看到「认证后可见价 + 去认证」，
     * 而不是价格或报错。此前该状态只存在于后端，前端无从判断，三态只能做两态。</p>
     */
    private Integer auditStatus;

    /** 当前身份在**本店**是否启用（`shop_user_tenant.enabled`，1/0）；停用后不可下单 */
    private Integer shopEnabled;
}
