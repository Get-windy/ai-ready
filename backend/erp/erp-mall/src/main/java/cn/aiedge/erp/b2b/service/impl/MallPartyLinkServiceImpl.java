package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.IdentityLinkDTO;
import cn.aiedge.erp.b2b.dto.PartyLinkApplyRequest;
import cn.aiedge.erp.b2b.mapper.ShopUserPartyLinkMapper;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.model.ShopUserPartyLink;
import cn.aiedge.erp.b2b.service.MallPartyLinkService;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.entity.PartyContact;
import cn.aiedge.erp.party.mapper.PartyContactMapper;
import cn.aiedge.erp.party.service.PartyService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallPartyLinkServiceImpl implements MallPartyLinkService {

    private final ShopUserPartyLinkMapper linkMapper;
    private final PartyService partyService;
    private final PartyContactMapper partyContactMapper;
    private final cn.aiedge.erp.b2b.mapper.ShopUserMapper shopUserMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── 申请 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(PartyLinkApplyRequest request) {
        Long userId = currentUserId();

        if (request.getPartyId() == null) {
            throw BusinessException.badRequest("请指定目标企业");
        }

        // 校验目标企业存在且为企业类型
        Party party = partyService.getById(request.getPartyId());
        if (party == null || party.getDeleted() != 0) {
            throw BusinessException.notFound("企业不存在");
        }
        if ("MEMBER".equals(party.getPartyLevel())) {
            throw BusinessException.badRequest("不能关联个人会员，请选择企业客户");
        }

        // 检查是否已有未撤销的关联
        ShopUserPartyLink existing = linkMapper.selectOne(
                new LambdaQueryWrapper<ShopUserPartyLink>()
                        .eq(ShopUserPartyLink::getShopUserId, userId)
                        .eq(ShopUserPartyLink::getPartyId, request.getPartyId())
                        .ne(ShopUserPartyLink::getStatus, ShopUserPartyLink.STATUS_REVOKED)
                        .ne(ShopUserPartyLink::getStatus, ShopUserPartyLink.STATUS_REJECTED)
        );
        if (existing != null) {
            throw BusinessException.badRequest("您已申请或已关联该企业，请勿重复申请");
        }

        // 检查被撤销后再次申请：更新已撤销记录
        ShopUserPartyLink revoked = linkMapper.selectOne(
                new LambdaQueryWrapper<ShopUserPartyLink>()
                        .eq(ShopUserPartyLink::getShopUserId, userId)
                        .eq(ShopUserPartyLink::getPartyId, request.getPartyId())
                        .eq(ShopUserPartyLink::getStatus, ShopUserPartyLink.STATUS_REVOKED)
                        .last("LIMIT 1")
        );
        if (revoked != null) {
            revoked.setStatus(ShopUserPartyLink.STATUS_PENDING_ENTERPRISE);
            revoked.setRevokedBy(null);
            revoked.setRevokedTime(null);
            revoked.setRevokeReason(null);
            revoked.setEnterpriseApprovedBy(null);
            revoked.setEnterpriseApprovedTime(null);
            revoked.setEnterpriseRemark(null);
            revoked.setTenantApprovedBy(null);
            revoked.setTenantApprovedTime(null);
            revoked.setTenantRemark(null);
            linkMapper.updateById(revoked);
            log.info("用户重新申请企业身份（已撤销记录复用）: userId={}, partyId={}", userId, request.getPartyId());
            return;
        }

        ShopUserPartyLink link = new ShopUserPartyLink();
        link.setShopUserId(userId);
        link.setPartyId(request.getPartyId());
        link.setStatus(ShopUserPartyLink.STATUS_PENDING_ENTERPRISE);
        link.setAppliedPhone(getCurrentUserPhone());
        linkMapper.insert(link);

        log.info("用户申请企业身份: userId={}, partyId={}, partyName={}",
                userId, request.getPartyId(), party.getPartyName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long linkId) {
        Long userId = currentUserId();
        ShopUserPartyLink link = getLinkForUser(linkId, userId);

        if (link.getStatus() != ShopUserPartyLink.STATUS_PENDING_ENTERPRISE
                && link.getStatus() != ShopUserPartyLink.STATUS_PENDING_TENANT) {
            throw BusinessException.badRequest("当前状态不可撤回");
        }

        // 撤回 = 软删除
        linkMapper.deleteById(linkId);
        log.info("用户撤回企业身份申请: userId={}, linkId={}", userId, linkId);
    }

    // ── 企业客户管理员审批 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enterpriseApprove(Long linkId, Long approvedBy, String remark) {
        ShopUserPartyLink link = getLink(linkId);
        assertStatus(link, ShopUserPartyLink.STATUS_PENDING_ENTERPRISE, "该申请不在待企业审批状态");

        // 校验审批人确实是该企业的管理员（is_primary=1 的联系人对应的 shop_user）
        assertIsEnterpriseAdmin(approvedBy, link.getPartyId());

        link.setStatus(ShopUserPartyLink.STATUS_PENDING_TENANT);
        link.setEnterpriseApprovedBy(approvedBy);
        link.setEnterpriseApprovedTime(LocalDateTime.now());
        link.setEnterpriseRemark(remark);
        linkMapper.updateById(link);

        log.info("企业客户管理员审批通过: linkId={}, partyId={}, approvedBy={}",
                linkId, link.getPartyId(), approvedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enterpriseReject(Long linkId, Long approvedBy, String reason) {
        ShopUserPartyLink link = getLink(linkId);
        assertStatus(link, ShopUserPartyLink.STATUS_PENDING_ENTERPRISE, "该申请不在待企业审批状态");
        assertIsEnterpriseAdmin(approvedBy, link.getPartyId());

        link.setStatus(ShopUserPartyLink.STATUS_REJECTED);
        link.setEnterpriseApprovedBy(approvedBy);
        link.setEnterpriseApprovedTime(LocalDateTime.now());
        link.setEnterpriseRemark(reason);
        linkMapper.updateById(link);

        log.info("企业客户管理员驳回申请: linkId={}, partyId={}, reason={}", linkId, link.getPartyId(), reason);
    }

    // ── 租户管理员审批 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tenantApprove(Long linkId, Long tenantUserId, String remark) {
        ShopUserPartyLink link = getLink(linkId);
        assertStatus(link, ShopUserPartyLink.STATUS_PENDING_TENANT, "该申请不在待租户审批状态");

        link.setStatus(ShopUserPartyLink.STATUS_APPROVED);
        link.setTenantApprovedBy(tenantUserId);
        link.setTenantApprovedTime(LocalDateTime.now());
        link.setTenantRemark(remark);
        linkMapper.updateById(link);

        log.info("租户管理员审批通过（关联正式生效）: linkId={}, userId={}, partyId={}",
                linkId, link.getShopUserId(), link.getPartyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tenantReject(Long linkId, Long tenantUserId, String reason) {
        ShopUserPartyLink link = getLink(linkId);
        assertStatus(link, ShopUserPartyLink.STATUS_PENDING_TENANT, "该申请不在待租户审批状态");

        link.setStatus(ShopUserPartyLink.STATUS_REJECTED);
        link.setTenantApprovedBy(tenantUserId);
        link.setTenantApprovedTime(LocalDateTime.now());
        link.setTenantRemark(reason);
        linkMapper.updateById(link);

        log.info("租户管理员驳回申请: linkId={}, userId={}, partyId={}", linkId, link.getShopUserId(), link.getPartyId());
    }

    // ── 撤销 ──

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long linkId, Long revokedBy, String revokeRole, String reason) {
        ShopUserPartyLink link = getLink(linkId);
        if (link.getStatus() != ShopUserPartyLink.STATUS_APPROVED) {
            throw BusinessException.badRequest("仅已通过的身份关联可撤销");
        }

        // 验证操作权限
        if ("ENTERPRISE".equals(revokeRole)) {
            assertIsEnterpriseAdmin(revokedBy, link.getPartyId());
        }
        // TENANT 角色由后台管理员身份保证，此处不做额外校验

        link.setStatus(ShopUserPartyLink.STATUS_REVOKED);
        link.setRevokedBy(revokedBy);
        link.setRevokedTime(LocalDateTime.now());
        link.setRevokeReason(reason);
        linkMapper.updateById(link);

        log.info("身份关联已撤销: linkId={}, userId={}, partyId={}, role={}, reason={}",
                linkId, link.getShopUserId(), link.getPartyId(), revokeRole, reason);
    }

    // ── 查询 ──

    @Override
    public List<IdentityLinkDTO> listMyLinks() {
        Long userId = currentUserId();
        List<ShopUserPartyLink> links = linkMapper.selectList(
                new LambdaQueryWrapper<ShopUserPartyLink>()
                        .eq(ShopUserPartyLink::getShopUserId, userId)
                        .ne(ShopUserPartyLink::getStatus, ShopUserPartyLink.STATUS_REVOKED)
                        .orderByDesc(ShopUserPartyLink::getStatus)
                        .orderByDesc(ShopUserPartyLink::getCreateTime)
        );

        List<IdentityLinkDTO> result = new ArrayList<>();
        for (ShopUserPartyLink link : links) {
            result.add(toLinkDTO(link));
        }
        return result;
    }

    @Override
    public List<IdentityLinkDTO> listByParty(Long partyId, Integer status) {
        LambdaQueryWrapper<ShopUserPartyLink> wrapper = new LambdaQueryWrapper<>();
        if (partyId != null) {
            wrapper.eq(ShopUserPartyLink::getPartyId, partyId);
        }
        if (status != null) {
            wrapper.eq(ShopUserPartyLink::getStatus, status);
        }
        wrapper.orderByDesc(ShopUserPartyLink::getCreateTime);

        List<ShopUserPartyLink> links = linkMapper.selectList(wrapper);
        List<IdentityLinkDTO> result = new ArrayList<>();
        for (ShopUserPartyLink link : links) {
            result.add(toLinkDTO(link));
        }
        return result;
    }

    // ── 内部工具 ──

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private String getCurrentUserPhone() {
        ShopUser user = shopUserMapper.selectById(currentUserId());
        return user != null ? user.getPhone() : null;
    }

    private ShopUserPartyLink getLink(Long linkId) {
        ShopUserPartyLink link = linkMapper.selectById(linkId);
        if (link == null) {
            throw BusinessException.notFound("关联记录不存在");
        }
        return link;
    }

    private ShopUserPartyLink getLinkForUser(Long linkId, Long userId) {
        ShopUserPartyLink link = linkMapper.selectById(linkId);
        if (link == null || !link.getShopUserId().equals(userId)) {
            throw BusinessException.notFound("关联记录不存在或无权操作");
        }
        return link;
    }

    private void assertStatus(ShopUserPartyLink link, int expected, String errMsg) {
        if (link.getStatus() != expected) {
            throw BusinessException.badRequest(errMsg);
        }
    }

    /**
     * 校验 shop_user 是否为该企业的管理员。
     * 判断标准：该 shop_user 的手机号在该企业的 biz_party_contact 中且 is_primary=1。
     */
    private void assertIsEnterpriseAdmin(Long shopUserId, Long partyId) {
        ShopUser user = shopUserMapper.selectById(shopUserId);
        if (user == null || user.getPhone() == null || user.getPhone().isEmpty()) {
            throw BusinessException.badRequest("您的账号未绑定手机号，无法执行审批操作");
        }
        Long count = partyContactMapper.selectCount(
                new LambdaQueryWrapper<PartyContact>()
                        .eq(PartyContact::getPartyId, partyId)
                        .and(w -> w.eq(PartyContact::getPhone, user.getPhone())
                                .or().eq(PartyContact::getMobile, user.getPhone()))
                        .eq(PartyContact::getIsPrimary, 1)
                        .eq(PartyContact::getDeleted, 0)
        );
        if (count == null || count == 0) {
            throw BusinessException.badRequest("您不是该企业的常用联系人（管理员），无权审批");
        }
    }

    private IdentityLinkDTO toLinkDTO(ShopUserPartyLink link) {
        IdentityLinkDTO dto = new IdentityLinkDTO();
        dto.setLinkId(link.getId());
        dto.setPartyId(link.getPartyId());
        dto.setStatus(link.getStatus());
        dto.setAppliedTime(link.getCreateTime() != null ? link.getCreateTime().format(DT_FMT) : null);
        dto.setUsable(link.getStatus() == ShopUserPartyLink.STATUS_APPROVED);

        Party party = partyService.getById(link.getPartyId());
        if (party != null) {
            dto.setPartyName(party.getPartyName());
            dto.setPartyCode(party.getPartyCode());
        }

        switch (link.getStatus()) {
            case ShopUserPartyLink.STATUS_PENDING_ENTERPRISE:
                dto.setStatusText("待企业审批");
                break;
            case ShopUserPartyLink.STATUS_PENDING_TENANT:
                dto.setStatusText("待平台审批");
                break;
            case ShopUserPartyLink.STATUS_APPROVED:
                dto.setStatusText("已通过");
                break;
            case ShopUserPartyLink.STATUS_REJECTED:
                dto.setStatusText("已驳回");
                break;
            case ShopUserPartyLink.STATUS_REVOKED:
                dto.setStatusText("已撤销");
                break;
            default:
                dto.setStatusText("未知");
        }
        return dto;
    }
}
