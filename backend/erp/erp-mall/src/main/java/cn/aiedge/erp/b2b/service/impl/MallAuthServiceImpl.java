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
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.model.ShopUserPartyLink;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MallAuthServiceImpl implements MallAuthService {

    private final ShopUserMapper shopUserMapper;
    private final ShopConfigMapper shopConfigMapper;
    private final PasswordEncryptor passwordEncryptor;
    private final PartyService partyService;
    private final ShopUserPartyLinkMapper shopUserPartyLinkMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("商城用户登录: {}", request.getUsername());

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

        // 登录到 Sa-Token
        StpUtil.login(user.getId());

        // ⚠️ 必须在下面的 buildIdentities 之前写入会话租户：
        // buildIdentities 要查 biz_party / shop_user_party_link，而这些查询会经过租户拦截器；
        // 「已登录但 session 里没有 tenantId」在拦截器侧是 fail-closed（平台-BREAK-01），
        // 写晚了会让身份列表恒为空。与主站登录 SysUserServiceImpl#login 同口径。
        if (user.getTenantId() != null) {
            StpUtil.getSession().set("tenantId", user.getTenantId());
        }

        // 加载可用身份列表，默认激活个人会员身份
        List<IdentityDTO> identities = buildIdentities(user);
        Long defaultActiveId = identities.stream()
                .filter(i -> "MEMBER".equals(i.getType()))
                .map(IdentityDTO::getPartyId)
                .findFirst()
                .orElse(identities.isEmpty() ? null : identities.get(0).getPartyId());

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
    public void register(LoginRequest request) {
        log.info("商城用户注册: {}", request.getUsername());

        // 检查用户名是否已存在
        Long count = shopUserMapper.selectCount(
                new LambdaQueryWrapper<ShopUser>()
                        .eq(ShopUser::getUsername, request.getUsername())
        );
        if (count != null && count > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 判断是否需要审核（取第一个租户的配置，简化处理）
        //
        // ── 新列优先 + 旧列兼容回落（V11.365.0 起对接）─────────────────────────
        // 「店铺设置 → 注册设置」页自 Flyway V11.365.0 起重做为对标 5 项，**只写**
        // reg_audit_required（列 reg_audit_required，对标实测「买家账号注册审核：否(0)/是(1)」），
        // 已不再写旧的 enable_auto_audit，故此处必须优先读新列，否则页面配置对注册流程无效。
        //
        // ⚠️ 两列语义**方向相反**，映射必须按各自实际代码/对标口径，不能按列名望文生义：
        //   · reg_audit_required（新，对标列）：1 = 需要审核，0 = 免审核（直通）。
        //   · enable_auto_audit（旧，V6.4.0 历史自造列）：从列名与既有实现看是
        //     「注册自动审核 1=是 0=否」，即有旧代码语义为 `==1 → 不需审核`（免审）。
        //     因此二者映射为：reg_audit_required == (enable_auto_audit == 1 ? 0 : 1)。
        //     本方法只以「是否需要审核」这一布尔口径表达，无需在两列间做数值换算，
        //     只要各按自身口径解读即可（新列 1→needAudit=true；旧列 1→needAudit=false）。
        //
        // 回落原因：兼容「新列尚无值」的两种情形 —— ① 存量租户历史数据只写过旧列
        // enable_auto_audit，其 reg_audit_required 为 NULL（V11.365.0 未应用时该列尚不存在，
        // 应用后亦可能为 NULL）；② 旧前端/已发布 JAR 的写入口径只写旧列。
        // 注：V11.365.0 初稿的 ADD COLUMN ... DEFAULT 0 会把既有行回填为 0（0=免审核），
        // 从而绕过本回落分支并**翻转**存量租户的审核行为；该缺陷已订正为
        // 「无 DEFAULT 新增 + 按旧列反向映射订正存量行」，故存量行会得到**与本回落分支一致**
        // 的确定值（enable_auto_audit=1 → 0；否则 → 1），本分支仅在缺值兜底时生效。
        // 详见《店铺设置开发文档》「为什么 reg_audit_required 不能给默认值」。
        boolean needAudit = true;
        ShopConfig config = shopConfigMapper.selectOne(
                new LambdaQueryWrapper<ShopConfig>()
                        .last("LIMIT 1")
        );
        if (config != null) {
            if (config.getRegAuditRequired() != null) {
                // 新列（对标口径）：1=需审核，0=免审核
                needAudit = config.getRegAuditRequired() == 1;
            } else if (config.getEnableAutoAudit() != null) {
                // 旧列回落（历史自造口径）：1=自动审核（即免审），0=需人工审核
                needAudit = config.getEnableAutoAudit() != 1;
            }
        }

        ShopUser user = new ShopUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncryptor.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setSource("h5");
        user.setAuditStatus(needAudit ? 0 : 1);
        user.setAuditTime(needAudit ? null : LocalDateTime.now());
        user.setStatus(1);
        // 默认注册为个人会员，企业管理员可后续升级为 ENTERPRISE
        user.setUserType("MEMBER");

        // 尝试按手机号匹配已有会员（biz_party.party_level='MEMBER'）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
            try {
                Party matchedParty = partyService.getOne(
                    new LambdaQueryWrapper<Party>()
                        .eq(Party::getPhone, request.getPhone())
                        .eq(Party::getPartyLevel, "MEMBER")
                        .eq(Party::getDeleted, 0)
                        .last("LIMIT 1")
                );
                if (matchedParty != null) {
                    user.setPartyId(matchedParty.getId());
                    user.setNickname(matchedParty.getPartyName());
                    log.info("注册时匹配到已有会员: phone={}, partyId={}", request.getPhone(), matchedParty.getId());
                }
            } catch (Exception e) {
                log.warn("注册时匹配会员失败", e);
            }
        }

        shopUserMapper.insert(user);

        log.info("商城用户注册成功: {}, 身份类型: {}, 需审核: {}",
                request.getUsername(), user.getUserType(), needAudit);
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
