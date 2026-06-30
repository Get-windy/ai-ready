package cn.aiedge.erp.b2b.dto;

import lombok.Data;

/**
 * 用户申请关联企业身份的请求
 */
@Data
public class PartyLinkApplyRequest {

    /** 目标企业客户的 biz_party.id */
    private Long partyId;

    /** 申请说明（可选） */
    private String remark;
}
