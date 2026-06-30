package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dto.IdentityLinkDTO;
import cn.aiedge.erp.b2b.dto.PartyLinkApplyRequest;

import java.util.List;

/**
 * 商城用户-企业身份关联服务（申请-审批-撤销）
 */
public interface MallPartyLinkService {

    /**
     * 用户申请关联企业身份
     */
    void apply(PartyLinkApplyRequest request);

    /**
     * 撤回申请（仅 STATUS_PENDING_ENTERPRISE / STATUS_PENDING_TENANT 可撤回）
     */
    void withdraw(Long linkId);

    /**
     * 企业客户管理员审批通过（第一步）
     *
     * @param linkId      关联记录 ID
     * @param approvedBy  审批人 shop_user.id
     */
    void enterpriseApprove(Long linkId, Long approvedBy, String remark);

    /**
     * 企业客户管理员驳回
     */
    void enterpriseReject(Long linkId, Long approvedBy, String reason);

    /**
     * 租户管理员审批通过（第二步，最终通过）
     */
    void tenantApprove(Long linkId, Long tenantUserId, String remark);

    /**
     * 租户管理员驳回
     */
    void tenantReject(Long linkId, Long tenantUserId, String reason);

    /**
     * 撤销已通过的身份关联（企业客户管理员 或 租户管理员可操作）
     *
     * @param revokedBy   撤销人 ID（shop_user.id 或后台用户 ID）
     * @param revokeRole  "ENTERPRISE" 或 "TENANT"
     */
    void revoke(Long linkId, Long revokedBy, String revokeRole, String reason);

    /**
     * 查询当前用户的企业身份关联列表（含待审批、已通过等）
     */
    List<IdentityLinkDTO> listMyLinks();

    /**
     * 查询指定企业客户的待审批/已关联用户列表（企业客户管理员视角）
     */
    List<IdentityLinkDTO> listByParty(Long partyId, Integer status);
}
