package cn.aiedge.erp.b2b.service.impl;

import cn.aiedge.base.security.PasswordEncryptor;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.b2b.dto.IdentityDTO;
import cn.aiedge.erp.b2b.dto.LoginRequest;
import cn.aiedge.erp.b2b.dto.LoginResponse;
import cn.aiedge.erp.b2b.dto.UserInfo;
import cn.aiedge.erp.b2b.mapper.ShopConfigMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserPartyLinkMapper;
import cn.aiedge.erp.b2b.mapper.ShopUserTenantMapper;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.model.ShopUserPartyLink;
import cn.aiedge.erp.b2b.model.ShopUserTenant;
import cn.aiedge.erp.b2b.service.MallAuthService;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.PartyService;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallAuthServiceImpl implements MallAuthService {

    private final ShopUserMapper shopUserMapper;
    private final ShopUserTenantMapper shopUserTenantMapper;
    private final ShopConfigMapper shopConfigMapper;
    private final PasswordEncryptor passwordEncryptor;
    private final PartyService partyService;
    private final ShopUserPartyLinkMapper shopUserPartyLinkMapper;

    @Override
    public LoginResponse login(LoginRequest request, Long shopTenantId) {
        Long shop = requireShopTenant(shopTenantId);
        log.info("商城用户登录: {}（本店租户 {}）", request.getUsername(), shop);

        ShopUser user = shopUserMapper.selectOne(
                new LambdaQueryWrapper<ShopUser>()
                        .eq(ShopUser::getUsername, request.getUsername())
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw BusinessException.badRequest("账号已被禁用");
        }

        if (!passwordEncryptor.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // ★ 入店校验（§11.3 阶段 1）——「必须在本店注册过」。
        // ⚠️ 刻意放在**密码验证之后**：先确认"是本人"，再说"他能不能进这家店"。
        //    顺序反了会把"本店有没有这个顾客"泄露给没通过密码验证的人（对方随口试用户名就能探测）。
        assertShopEntryAllowed(shopUserTenantMapper.selectLink(user.getId(), shop), shop);

        // 登录到 Sa-Token
        StpUtil.login(user.getId());

        // ⚠️ 会话租户 = **本店租户**（不是 shop_user.tenant_id：那是系统级归属位、恒 0）。
        // 且必须在下面的 buildIdentities 之前写入：
        // buildIdentities 要查 biz_party / shop_user_party_link，而这些查询会经过租户拦截器；
        // 「已登录但 session 里没有 tenantId」在拦截器侧是 fail-closed（平台-BREAK-01），
        // 写晚了会让身份列表恒为空。与主站登录 SysUserServiceImpl#login 同口径。
        StpUtil.getSession().set("tenantId", shop);

        // 加载可用身份列表，默认激活个人会员身份
        List<IdentityDTO> identities = buildIdentities(user);
        // ⚠️ 两处 filter(Objects::nonNull) **不是防御性代码，是修 P0**：
        //    未关联 biz_party 的顾客只有"虚拟会员身份"，其 partyId 为 null；
        //    而 null 一旦流进 Stream.findFirst()，FindOps 会执行 `Optional.of(null)` ⇒
        //    NullPointerException ⇒ **每个新注册顾客的首次登录都 500**
        //    （2026-09-22 由 `tools/verify-mall-shop-entry.cjs` 在真机上暴露；
        //     此前库里没有任何商城顾客，这条路从未被走通过）。
        Long defaultActiveId = identities.stream()
                .filter(i -> "MEMBER".equals(i.getType()))
                .map(IdentityDTO::getPartyId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> identities.stream()
                        .map(IdentityDTO::getPartyId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));

        // 默认激活身份写入 session
        if (defaultActiveId != null) {
            StpUtil.getSession().set("activePartyId", defaultActiveId);
        }

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        UserInfo userInfo = buildUserInfo(user, defaultActiveId);

        LoginResponse response = new LoginResponse();
        response.setToken(tokenInfo.getTokenValue());
        response.setUser(userInfo);
        response.setIdentities(identities);
        response.setActiveIdentity(defaultActiveId);

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        shopUserMapper.updateById(user);

        log.info("商城用户登录成功: {}, 身份数: {}, 默认激活: {}, token: {}",
                request.getUsername(), identities.size(), defaultActiveId, tokenInfo.getTokenValue());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(LoginRequest request, Long shopTenantId) {
        Long shop = requireShopTenant(shopTenantId);
        boolean needAudit = needAuditInShop(shop);
        String appliedPhone = trimToNull(request.getPhone());

        ShopUser existing = shopUserMapper.selectOne(
                new LambdaQueryWrapper<ShopUser>()
                        .eq(ShopUser::getUsername, request.getUsername())
                        .last("LIMIT 1")
        );

        if (existing == null) {
            registerNewSystemCustomer(request, shop, needAudit, appliedPhone);
            return;
        }

        // ══ 分支 B：系统里已有这个自然人（多半是在别家店注册过）══
        // ⚠️ **必须自证身份**（原密码）。没有这一步，任何人都能拿别人的用户名绑到新店 ——
        //    这正是 §7.6 U1 用户强调的冒用场景（"手机号被别人重新注册，怎么确定？"）。
        //    自证 = 他知道那个账号的密码 ⇒ 就是他本人。
        if (!passwordEncryptor.matches(request.getPassword(), existing.getPassword())) {
            throw BusinessException.badRequest("该用户名已存在（可能您已在其它店铺注册过）。"
                    + "为避免冒名绑定，请使用原账号密码验证后再开通本店");
        }
        if (existing.getStatus() != 1) {
            throw BusinessException.badRequest("账号已被禁用，请联系平台客服");
        }

        ShopUserTenant link = shopUserTenantMapper.selectLink(existing.getId(), shop);
        if (link != null && Integer.valueOf(ShopUserTenant.STATUS_ACTIVE).equals(link.getStatus())) {
            throw BusinessException.badRequest("您已在本店注册，请直接登录");
        }
        if (link != null && Integer.valueOf(ShopUserTenant.STATUS_PENDING).equals(link.getStatus())) {
            throw BusinessException.badRequest("您的注册申请正在等待店铺审核，无需重复提交");
        }

        // 复用**同一个系统顾客**，只补一条本店关联。
        // ⚠️ 已经存在"已拒绝 / 已解除"的行时**就地改**而不是再插一条：
        //    shop_user_tenant 上有"同一顾客×同一租户"的部分唯一索引，重复插入会直接违反约束
        //    （CRM-BREAK-03 的教训：解除后无法重新注册）。
        if (link == null) {
            link = new ShopUserTenant();
            link.setShopUserId(existing.getId());
            link.setTenantId(shop);
            link.setIsDefault(1);
        }
        link.setStatus(needAudit ? ShopUserTenant.STATUS_PENDING : ShopUserTenant.STATUS_ACTIVE);
        link.setSource(ShopUserTenant.SOURCE_SYSTEM_REUSE);
        link.setAppliedPhone(appliedPhone);
        link.setRejectReason(null);
        link.setAuditBy(null);
        link.setAuditTime(needAudit ? null : LocalDateTime.now());

        if (link.getId() == null) {
            shopUserTenantMapper.insert(link);
        } else {
            shopUserTenantMapper.updateById(link);
        }

        log.info("商城用户注册（复用系统顾客）: username={}, shopUserId={}, 本店租户={}, 需审核={}",
                request.getUsername(), existing.getId(), shop, needAudit);
    }

    /** 分支 A：系统里还没有这个自然人 ⇒ 建系统顾客（tenant_id = 0）+ 本店关联。 */
    private void registerNewSystemCustomer(LoginRequest request, Long shop, boolean needAudit,
                                           String appliedPhone) {
        ShopUser user = new ShopUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncryptor.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setSource("h5");
        // ⚠️ 顾客是**系统级身份**：shop_user 已登记进 IGNORE_TENANT_TABLES，tenant_id 恒 0。
        //    "他属于哪家店"由 shop_user_tenant 表达，**不要**在这里写本店租户。
        user.setTenantId(0L);
        // 审核已改挂关联表（§11.3 阶段 1）：shop_user.audit_status 降级为历史列，
        // 这里只写"平台级账号是否可用"的 status，不再用它表达"某家店的注册审核"。
        user.setAuditStatus(1);
        user.setAuditTime(LocalDateTime.now());
        user.setStatus(1);
        // 默认注册为个人会员，企业管理员可后续升级为 ENTERPRISE
        user.setUserType("MEMBER");

        // 尝试按手机号匹配**本店**已有会员（biz_party.party_level='MEMBER'）。
        // ⚠️ 必须显式限定本店租户：注册是白名单端点、没有登录会话，
        //    不加租户条件会把别家店的同号会员匹配过来（跨租户串档）。
        if (appliedPhone != null) {
            user.setPhone(appliedPhone);
            try {
                Party matchedParty = partyService.getOne(
                    new LambdaQueryWrapper<Party>()
                        .eq(Party::getPhone, appliedPhone)
                        .eq(Party::getTenantId, shop)
                        .eq(Party::getPartyLevel, "MEMBER")
                        .eq(Party::getDeleted, 0)
                        .last("LIMIT 1")
                );
                if (matchedParty != null) {
                    user.setPartyId(matchedParty.getId());
                    user.setNickname(matchedParty.getPartyName());
                    log.info("注册时匹配到本店已有会员: phone={}, partyId={}", appliedPhone, matchedParty.getId());
                }
            } catch (Exception e) {
                log.warn("注册时匹配会员失败", e);
            }
        }

        shopUserMapper.insert(user);

        ShopUserTenant link = new ShopUserTenant();
        link.setShopUserId(user.getId());
        link.setTenantId(shop);
        link.setStatus(needAudit ? ShopUserTenant.STATUS_PENDING : ShopUserTenant.STATUS_ACTIVE);
        link.setSource(ShopUserTenant.SOURCE_SELF_REGISTER);
        link.setIsDefault(1);
        link.setAppliedPhone(appliedPhone);
        link.setAuditTime(needAudit ? null : LocalDateTime.now());
        shopUserTenantMapper.insert(link);

        log.info("商城用户注册成功（新建系统顾客）: {}, 本店租户={}, 身份类型={}, 需审核={}",
                request.getUsername(), shop, user.getUserType(), needAudit);
    }

    // ══════════════════════ 本店口径（入店 / 审核） ══════════════════════

    /** 本店租户必须解析得到：拿不到就明确报错，**绝不**退化成"看全表"或"猜一家店"。 */
    static Long requireShopTenant(Long shopTenantId) {
        if (shopTenantId == null) {
            throw BusinessException.badRequest("无法确定您正在访问哪家店铺（缺少店铺标识）—— "
                    + "请从店铺入口进入后再登录/注册");
        }
        return shopTenantId;
    }

    /**
     * 入店校验：这个系统顾客在**本店**的关联必须是"正常"。
     *
     * <p>四种状态分开说，不许合成一句"登录失败"：顾客要知道自己该去注册、还是等审核、
     * 还是被拒了（被拒要能看到理由），否则只能反复试。</p>
     */
    void assertShopEntryAllowed(ShopUserTenant link, Long shop) {
        if (link == null) {
            throw BusinessException.badRequest("您还没有在本店注册，请先注册后再登录");
        }
        Integer status = link.getStatus();
        if (status == null || ShopUserTenant.STATUS_PENDING == status) {
            throw BusinessException.badRequest("您的注册申请正在等待店铺审核，审核通过后即可登录");
        }
        if (ShopUserTenant.STATUS_REJECTED == status) {
            String reason = trimToNull(link.getRejectReason());
            throw BusinessException.badRequest("您的注册申请已被本店拒绝"
                    + (reason == null ? "" : "：" + reason) + "。如有疑问请联系店铺客服");
        }
        if (ShopUserTenant.STATUS_REVOKED == status) {
            throw BusinessException.badRequest("您在本店的注册已被解除，如需继续购物请重新注册");
        }
        if (ShopUserTenant.STATUS_ACTIVE != status) {
            // 未知状态一律**不放行**（fail-closed）：宁可让顾客问一句，也不放一个状态不明的人进店
            throw BusinessException.badRequest("您在本店的注册状态异常（" + status + "），请联系店铺管理员");
        }
        log.debug("入店校验通过: shopUserId={}, 本店租户={}", link.getShopUserId(), shop);
    }

    /**
     * **本店**是否需要注册审核。
     *
     * <p>⚠️ 原来这里是 `selectOne(... .last("LIMIT 1"))` —— **取全表第一条配置**。
     * 多租户下这等于"审核不审核由另一家店的设置决定"，是实打实的错。
     * 现按本店租户精确取。</p>
     *
     * <p>两列语义**方向相反**（V11.365.0 起新列优先、旧列回落，理由见《店铺设置开发文档》）：
     * {code reg_audit_required}：1=需审核、0=免审核（直通）；
     * {@code enable_auto_audit}（历史自造列）：1=自动审核（即免审）、0=需人工审核。
     * 缺值兜底取"需审核"（从严）—— 免审核是**放宽**，不能靠默认值悄悄生效。</p>
     */
    private boolean needAuditInShop(Long shop) {
        ShopConfig config = shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>()
                        .eq(ShopConfig::getTenantId, shop)
                        .last("LIMIT 1")
        );
        if (config == null) {
            return true;
        }
        if (config.getRegAuditRequired() != null) {
            return config.getRegAuditRequired() == 1;
        }
        if (config.getEnableAutoAudit() != null) {
            return config.getEnableAutoAudit() != 1;
        }
        return true;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public void logout() {
        log.info("商城用户登出");
        StpUtil.logout();
    }

    @Override
    public String refreshToken() {
        log.info("刷新商城用户token");
        // StpUtil.login 会重建会话，会话里的租户标记随之丢失；必须在重建后补写，
        // 否则「刷新过 token 的会话」又变回「已登录但无租户上下文」（平台-BREAK-01）。
        // 先按旧会话取用户（此时旧 tenantId 还在，查询正常），再重建。
        ShopUser user = currentShopUser();
        StpUtil.login(user.getId());
        if (user.getTenantId() != null) {
            StpUtil.getSession().set("tenantId", user.getTenantId());
        }
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return tokenInfo.getTokenValue();
    }

    @Override
    public List<IdentityDTO> listIdentities() {
        ShopUser user = currentShopUser();
        return buildIdentities(user);
    }

    @Override
    public void switchIdentity(Long partyId) {
        ShopUser user = currentShopUser();
        List<IdentityDTO> identities = buildIdentities(user);
        boolean allowed = identities.stream().anyMatch(i -> partyId.equals(i.getPartyId()));
        if (!allowed) {
            throw BusinessException.badRequest("无权切换到该身份");
        }
        StpUtil.getSession().set("activePartyId", partyId);
        log.info("用户切换身份: userId={}, activePartyId={}", user.getId(), partyId);
    }

    // ==================== 内部工具方法 ====================

    /** 获取当前登录的 ShopUser */
    private ShopUser currentShopUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        ShopUser user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    /**
     * 构建用户可切换的所有身份：
     * 1. 个人会员身份（来自 shop_user.party_id → biz_party.MEMBER，或构造虚拟身份）
     * 2. 企业客户身份（仅来自 shop_user_party_link 审批通过的记录，申请审批制）
     */
    private List<IdentityDTO> buildIdentities(ShopUser user) {
        List<IdentityDTO> identities = new ArrayList<>();
        Set<Long> addedPartyIds = new LinkedHashSet<>();

        // 1. 个人会员身份
        if (user.getPartyId() != null) {
            Party memberParty = partyService.getById(user.getPartyId());
            if (memberParty != null) {
                IdentityDTO member = new IdentityDTO();
                member.setPartyId(memberParty.getId());
                member.setType("MEMBER");
                member.setName(memberParty.getPartyName() != null ? memberParty.getPartyName() : user.getNickname());
                member.setMemberCardNo(memberParty.getMemberCardNo());
                member.setPhone(memberParty.getPhone() != null ? memberParty.getPhone() : user.getPhone());
                identities.add(member);
                addedPartyIds.add(memberParty.getId());
            }
        } else {
            // 没有关联 biz_party，构造虚拟个人会员身份
            IdentityDTO virtualMember = new IdentityDTO();
            virtualMember.setType("MEMBER");
            virtualMember.setName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            virtualMember.setPhone(user.getPhone());
            identities.add(virtualMember);
        }

        // 2. 企业客户身份：仅来自审批通过的 shop_user_party_link（申请审批制）
        List<ShopUserPartyLink> approvedLinks = shopUserPartyLinkMapper.selectList(
                new LambdaQueryWrapper<ShopUserPartyLink>()
                        .eq(ShopUserPartyLink::getShopUserId, user.getId())
                        .eq(ShopUserPartyLink::getStatus, ShopUserPartyLink.STATUS_APPROVED)
        );
        for (ShopUserPartyLink link : approvedLinks) {
            if (link.getPartyId() == null || addedPartyIds.contains(link.getPartyId())) {
                continue;
            }
            Party enterprise = partyService.getById(link.getPartyId());
            if (enterprise == null || enterprise.getDeleted() != 0) {
                continue;
            }
            if ("MEMBER".equals(enterprise.getPartyLevel())) {
                continue;
            }
            IdentityDTO ent = new IdentityDTO();
            ent.setPartyId(enterprise.getId());
            ent.setType("ENTERPRISE");
            ent.setName(enterprise.getPartyName());
            ent.setCode(enterprise.getPartyCode());
            ent.setPhone(link.getAppliedPhone() != null ? link.getAppliedPhone() : user.getPhone());
            identities.add(ent);
            addedPartyIds.add(enterprise.getId());
        }

        return identities;
    }

    /**
     * 构建 UserInfo，填充身份体系信息。
     * 如果传入了 activePartyId，则根据该身份填充；否则使用 shop_user 默认信息。
     */
    private UserInfo buildUserInfo(ShopUser user, Long activePartyId) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(String.valueOf(user.getId()));
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setPhone(user.getPhone());
        userInfo.setUserType(user.getUserType() != null ? user.getUserType() : "MEMBER");
        userInfo.setCompanyName(user.getCompanyName());
        userInfo.setPartyId(user.getPartyId());

        // 如果有激活身份，用该身份覆盖 userType / partyId / companyName
        if (activePartyId != null) {
            try {
                Party party = partyService.getById(activePartyId);
                if (party != null) {
                    userInfo.setPartyId(party.getId());
                    userInfo.setPartyName(party.getPartyName());
                    userInfo.setMemberCardNo(party.getMemberCardNo());
                    userInfo.setPoints(party.getPoints() != null ? party.getPoints() : 0);
                    if ("MEMBER".equals(party.getPartyLevel()) || party.getPartyLevel() == null) {
                        userInfo.setUserType("MEMBER");
                    } else {
                        userInfo.setUserType("ENTERPRISE");
                        userInfo.setCompanyName(party.getPartyName());
                    }
                }
            } catch (Exception e) {
                log.warn("获取往来单位信息失败: partyId={}", activePartyId, e);
            }
        } else if (user.getPartyId() != null) {
            // 回退：使用 shop_user.party_id 对应的 party
            try {
                Party party = partyService.getById(user.getPartyId());
                if (party != null) {
                    userInfo.setPartyName(party.getPartyName());
                    userInfo.setMemberCardNo(party.getMemberCardNo());
                    userInfo.setPoints(party.getPoints() != null ? party.getPoints() : 0);
                    if ("ENTERPRISE".equals(user.getUserType())
                            && (user.getCompanyName() == null || user.getCompanyName().isEmpty())) {
                        userInfo.setCompanyName(party.getPartyName());
                    }
                }
            } catch (Exception e) {
                log.warn("获取往来单位信息失败: partyId={}", user.getPartyId(), e);
            }
        }

        return userInfo;
    }
}
