package cn.aiedge.erp.b2b.dto;

import lombok.Data;

/**
 * 用户可用的身份选项。
 * 登录后返回给前端，用户可切换当前下单身份。
 */
@Data
public class IdentityDTO {

    /** biz_party.id（MEMBER 或 ENTERPRISE 均可） */
    private Long partyId;

    /** 身份类型：MEMBER / ENTERPRISE */
    private String type;

    /** 显示名称：企业名称 或 会员姓名 */
    private String name;

    /** 企业编码（仅 ENTERPRISE 有值，用于前端区分） */
    private String code;

    /** 会员卡号（仅 MEMBER 有值） */
    private String memberCardNo;

    /** 联系电话 */
    private String phone;
}
