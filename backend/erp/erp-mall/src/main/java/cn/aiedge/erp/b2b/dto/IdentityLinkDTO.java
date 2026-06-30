package cn.aiedge.erp.b2b.dto;

import lombok.Data;

/**
 * 用户身份关联状态（含审批进度）
 */
@Data
public class IdentityLinkDTO {

    /** shop_user_party_link.id */
    private Long linkId;

    /** biz_party.id */
    private Long partyId;

    /** 企业名称 */
    private String partyName;

    /** 企业编码 */
    private String partyCode;

    /** 状态: 0=待企业审批 1=待租户审批 2=已通过 3=已驳回 4=已撤销 */
    private Integer status;

    /** 状态文字描述 */
    private String statusText;

    /** 是否可以作为当前下单身份（仅 status=2 时为 true） */
    private boolean usable;

    /** 申请时间 */
    private String appliedTime;
}
