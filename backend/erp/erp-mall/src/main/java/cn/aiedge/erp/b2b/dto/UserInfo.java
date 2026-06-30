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
}
