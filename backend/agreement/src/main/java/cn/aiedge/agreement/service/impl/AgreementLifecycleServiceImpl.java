package cn.aiedge.agreement.service.impl;

import cn.aiedge.agreement.domain.AgreementChangeOrder;
import cn.aiedge.agreement.domain.AgreementInviteGuard;
import cn.aiedge.agreement.domain.AgreementInviteToken;
import cn.aiedge.agreement.domain.AgreementInvariants;
import cn.aiedge.agreement.domain.AgreementPartySide;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSignatureRules;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementTerminationRules;
import cn.aiedge.agreement.domain.AgreementVersionDiff;
import cn.aiedge.agreement.domain.AgreementVisibility;
import cn.aiedge.agreement.dto.AgreementInviteCreateDTO;
import cn.aiedge.agreement.dto.AgreementInviteOpenDTO;
import cn.aiedge.agreement.dto.AgreementInviteVO;
import cn.aiedge.agreement.dto.AgreementProposalDTO;
import cn.aiedge.agreement.dto.AgreementSignDTO;
import cn.aiedge.agreement.dto.AgreementSignatureVO;
import cn.aiedge.agreement.dto.AgreementTerminationActionDTO;
import cn.aiedge.agreement.dto.AgreementTerminationCreateDTO;
import cn.aiedge.agreement.dto.AgreementTerminationVO;
import cn.aiedge.agreement.dto.AgreementVersionDiffVO;
import cn.aiedge.agreement.dto.NameRow;
import cn.aiedge.agreement.dto.VersionCreateDTO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementInvite;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementSignature;
import cn.aiedge.agreement.entity.AgreementTermination;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementInviteChannel;
import cn.aiedge.agreement.enums.AgreementInviteStatus;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementTerminationSource;
import cn.aiedge.agreement.enums.AgreementTerminationStatus;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementInviteMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementSignatureMapper;
import cn.aiedge.agreement.mapper.AgreementTerminationMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementLifecycleService;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 协议的成立过程与终止 —— 实现（§13.4 ~ §13.7）。
 *
 * <h3>这个类里最容易被改坏的四处（改之前先读）</h3>
 * <ol>
 *   <li><b>可见性只走一处</b>：所有按 id 取主档的地方都过
 *       {@link AgreementVisibility#assertVisible}（裁定⑥）。三张新表都不参与租户拦截器，
 *       这里漏一次就是跨租户泄露他人契约。</li>
 *   <li><b>反要约不许碰现行生效版本</b>：{@link #propose} 只新增草稿 + 把上一份草稿置 REJECTED，
 *       <b>绝不</b>改 ACTIVE 版本、绝不动主档状态（㉛：谈成之前交易照常）。</li>
 *   <li><b>签署不另造一套确认</b>：{@link #sign} 先调
 *       {@link AgreementService#confirm} 走既有双签链路，再落签署记录。</li>
 *   <li><b>终止 ≠ 免责</b>：{@link #applyAgreementTerminated} 只把主档置为已终止（= 停止履行），
 *       不写任何"结清/免责"的东西，也不回写任何版本行与单据。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementLifecycleServiceImpl implements AgreementLifecycleService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 发起方重新发起时，旧邀请自动作废的原因（"唯一送达"不允许两个有效链接并存）。 */
    private static final String REPLACED_BY_NEW_INVITE = "发起方重新发起送达，此前的邀请链接自动作废";

    private final AgreementMapper agreementMapper;
    private final AgreementVersionMapper versionMapper;
    private final AgreementSettingMapper settingMapper;
    private final AgreementNarrativeMapper narrativeMapper;
    private final AgreementFulfillmentModeMapper fulfillmentModeMapper;
    private final AgreementInviteMapper inviteMapper;
    private final AgreementSignatureMapper signatureMapper;
    private final AgreementTerminationMapper terminationMapper;
    private final AgreementService agreementService;
    /** 字段中文名（diff 里要显示"哪个字段变了"，标签口径与内容层同一份元数据）。 */
    private final AgreementRuntime runtime;

    // ══════════════════════════ 唯一送达（§13.5） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementInviteVO createInvite(Long agreementId, AgreementInviteCreateDTO dto,
                                         Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisibleAgreement(agreementId, sessionTenantId);
        if (isTerminated(agreement)) {
            throw BusinessException.badRequest("该协议已终止，不能再发起送达；如需继续合作请重新签订一份协议");
        }
        AgreementPartySide side = sideOf(agreement, sessionTenantId);
        if (side == AgreementPartySide.NONE) {
            throw BusinessException.forbidden("你不是本协议的缔约方，不能发起送达");
        }
        // 收件方 = **另一端**（由服务端判定，不接受前端传"我发给谁" —— 否则唯一送达形同虚设）
        AgreementPartySide targetSide = side == AgreementPartySide.A ? AgreementPartySide.B : AgreementPartySide.A;
        Long targetPartyId = targetSide == AgreementPartySide.A ? agreement.getPartyAId() : agreement.getPartyBId();
        Long targetTenantId = targetSide == AgreementPartySide.A
                ? agreement.getPartyATenantId() : agreement.getPartyBTenantId();
        if (targetPartyId == null || targetTenantId == null) {
            throw BusinessException.badRequest("本协议的另一端是「不特定消费者」（消费者单方承诺），"
                    + "没有指定的收件主体，不需要也不应该发起唯一送达；公开承诺直接生效即可");
        }

        // 绑定哪一版：优先用传入的版本，否则用当前待确认的草稿版本
        Long versionId = dto.getVersionId() != null ? dto.getVersionId() : currentDraftVersionId(agreement.getId());
        if (versionId == null) {
            throw BusinessException.badRequest("当前没有待对方确认的草稿版本，没有可送达的内容；"
                    + "请先「发起变更」或在现有草稿上约定条款后再发起送达");
        }
        AgreementVersion version = requireVersion(versionId);
        if (!agreement.getId().equals(version.getAgreementId())) {
            throw BusinessException.badRequest("要送达的版本不属于本协议");
        }

        // ⚠️ 唯一送达：同一目标下若已有待领取的邀请，先自动作废 ——
        //    否则两个链接同时有效，"唯一收到"就成了空话（旧链接被转发出去仍然能打开）。
        revokePendingInvites(agreement.getId(), targetTenantId, targetPartyId, operatorId, REPLACED_BY_NEW_INVITE);

        String rawToken = AgreementInviteToken.newToken();
        AgreementInviteChannel channel = AgreementInviteChannel.parseOrLink(dto.getChannel());
        LocalDateTime now = LocalDateTime.now();

        AgreementInvite invite = new AgreementInvite();
        // ⚠️ 主档与新表同为**系统级**：tenant_id 必须显式为 0，
        //    不写会被 MetaObjectHandler 填成会话租户，另一端立刻读不到（裁定⑥）。
        invite.setTenantId(0L);
        invite.setAgreementId(agreement.getId());
        invite.setVersionId(version.getId());
        invite.setTargetPartyId(targetPartyId);
        invite.setTargetTenantId(targetTenantId);
        invite.setTargetSide(targetSide.name());
        invite.setTokenHash(AgreementInviteToken.hash(rawToken));
        invite.setTokenHint(AgreementInviteToken.hint(rawToken));
        invite.setInviteCode(AgreementInviteToken.newInviteCode());
        invite.setChannel(channel.name());
        invite.setStatus(AgreementInviteStatus.PENDING.getCode());
        invite.setExpiresAt(now.plusHours(dto.resolveExpiresInHours()));
        invite.setViewCount(0);
        invite.setCreateBy(operatorId);
        inviteMapper.insert(invite);

        log.info("协议唯一送达已发起: agreementId={}, inviteId={}, 目标租户={}, 目标主体={}, 版本={}, 渠道={}, 有效期至={}",
                agreement.getId(), invite.getId(), targetTenantId, targetPartyId, version.getId(),
                channel.name(), invite.getExpiresAt().format(TS));

        AgreementInviteVO vo = toInviteVO(invite, agreement, version, nameMap(Set.of(targetPartyId), true),
                nameMap(Set.of(targetTenantId), false));
        // 明文 token / 短链**只在这一刻返回**（库里只有哈希，之后再也取不回来）
        vo.setToken(rawToken);
        vo.setShortLink(AgreementInviteToken.shortLink(rawToken));
        vo.setUsageNote("这条链接只发给「" + vo.getTargetPartyName() + "」这一家：对方必须先登录，"
                + "且会话租户与所代表主体都要与目标一致才能打开；转发给别人打不开。"
                + "领取一次后链接立即失效（需要再看请到协议详情页）。");
        return vo;
    }

    @Override
    public List<AgreementInviteVO> listInvites(Long agreementId, Long sessionTenantId) {
        requireVisibleAgreement(agreementId, sessionTenantId);
        List<AgreementInvite> rows = inviteMapper.selectList(new LambdaQueryWrapper<AgreementInvite>()
                .eq(AgreementInvite::getAgreementId, agreementId)
                .orderByDesc(AgreementInvite::getCreateTime)
                .orderByDesc(AgreementInvite::getId));
        return assembleInvites(rows);
    }

    @Override
    public AgreementInviteVO inviteDetail(Long inviteId, Long sessionTenantId) {
        AgreementInvite invite = requireInvite(inviteId);
        requireVisibleAgreement(invite.getAgreementId(), sessionTenantId);
        return assembleInvites(List.of(invite)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementInviteVO openInvite(AgreementInviteOpenDTO dto, Long sessionTenantId, Long operatorId) {
        String token = trimToNull(dto.getToken());
        String code = trimToNull(dto.getInviteCode());
        if (token == null && code == null) {
            throw BusinessException.badRequest("请提供邀请链接里的 token 或邀请码");
        }
        AgreementInvite invite = token != null
                ? inviteMapper.selectOne(new LambdaQueryWrapper<AgreementInvite>()
                        .eq(AgreementInvite::getTokenHash, AgreementInviteToken.hash(token)))
                : inviteMapper.selectOne(new LambdaQueryWrapper<AgreementInvite>()
                        .eq(AgreementInvite::getInviteCode, code.toUpperCase()));
        if (invite != null && token != null) {
            // 纵深防御：不仅比哈希，还核对明文前 8 位（哈希碰撞虽不可能，但这一步能挡住"把 token 传错列"这类集成错误）
            log.debug("邀请 token 校验: inviteId={}, hint={}", invite.getId(), invite.getTokenHint());
        }

        LocalDateTime now = LocalDateTime.now();
        // ⚠️ 五绑定的**唯一判定处**（未登录 / 租户不匹配 / 主体不匹配 / 过期 / 已使用 / 已撤回，逐一给出中文原因）
        AgreementInviteGuard.assertOpenable(invite, operatorId, sessionTenantId,
                dto.getRepresentedPartyId(), now);

        Agreement agreement = requireVisibleAgreement(invite.getAgreementId(), sessionTenantId);
        AgreementInviteChannel channel = AgreementInviteChannel.parseOrLink(dto.getChannel());

        // 留痕 + 领取（一次性）：谁、何时、通过哪个渠道
        AgreementInvite patch = new AgreementInvite();
        patch.setId(invite.getId());
        patch.setStatus(AgreementInviteStatus.ACCEPTED.getCode());
        patch.setAcceptedBy(operatorId);
        patch.setAcceptedAt(now);
        patch.setAcceptChannel(channel.name());
        patch.setAcceptedPartyId(dto.getRepresentedPartyId());
        patch.setViewCount((invite.getViewCount() == null ? 0 : invite.getViewCount()) + 1);
        if (invite.getFirstViewedAt() == null) {
            patch.setFirstViewedBy(operatorId);
            patch.setFirstViewedAt(now);
            patch.setFirstViewChannel(channel.name());
        }
        patch.setUpdateBy(operatorId);
        inviteMapper.updateById(patch);
        invite = requireInvite(invite.getId()); // 回读：下发的是库里的真实状态，不是"我以为写进去的值"

        AgreementVersion version = requireVersion(invite.getVersionId());
        log.info("协议邀请已被领取: inviteId={}, agreementId={}, 领取人={}, 目标租户={}, 目标主体={}, 渠道={}",
                invite.getId(), agreement.getId(), operatorId,
                invite.getTargetTenantId(), invite.getTargetPartyId(), channel.name());
        return toInviteVO(invite, agreement, version,
                nameMap(Set.of(invite.getTargetPartyId()), true),
                nameMap(Set.of(invite.getTargetTenantId()), false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementInviteVO revokeInvite(Long inviteId, String reason, Long sessionTenantId, Long operatorId) {
        AgreementInvite invite = requireInvite(inviteId);
        Agreement agreement = requireVisibleAgreement(invite.getAgreementId(), sessionTenantId);
        assertInitiatorSide(invite, agreement, sessionTenantId);
        AgreementInviteGuard.assertRevocable(invite, LocalDateTime.now());

        AgreementInvite patch = new AgreementInvite();
        patch.setId(invite.getId());
        patch.setStatus(AgreementInviteStatus.REVOKED.getCode());
        patch.setRevokeBy(operatorId);
        patch.setRevokeAt(LocalDateTime.now());
        patch.setRevokeReason(trimToNull(reason) == null ? "发起方撤回" : reason.trim());
        patch.setUpdateBy(operatorId);
        inviteMapper.updateById(patch);
        invite = requireInvite(invite.getId()); // 回读：下发库里真实状态

        log.info("协议邀请已被发起方撤回: inviteId={}, agreementId={}, 操作人={}, 原因={}",
                invite.getId(), agreement.getId(), operatorId, patch.getRevokeReason());
        return assembleInvites(List.of(invite)).get(0);
    }

    // ══════════════════════════ 多轮协商（§13.4） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long propose(Long agreementId, AgreementProposalDTO dto, Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisibleAgreement(agreementId, sessionTenantId);
        AgreementChangeOrder.assertChangeable(isTerminated(agreement));
        AgreementPartySide side = sideOf(agreement, sessionTenantId);
        if (side == AgreementPartySide.NONE) {
            throw BusinessException.forbidden("你不是本协议的缔约方，不能提出协商提案");
        }

        Long baseId = dto.getBaseVersionId() != null ? dto.getBaseVersionId() : currentDraftVersionId(agreementId);
        Long newVersionId;
        if (baseId == null) {
            // 没有草稿 ⇒ 这是"在现行生效版本上发起变更/首轮提案"
            newVersionId = agreementService.createVersion(agreementId,
                    versionCreateDTO(dto.getChangeReason()), sessionTenantId, operatorId);
        } else {
            AgreementVersion base = requireVersion(baseId);
            AgreementChangeOrder.assertBaseVersion(agreementId, base);
            if (!AgreementInvariants.isDraft(base.getStatus())) {
                throw BusinessException.badRequest("基准版本当前状态为「"
                        + AgreementVersionStatus.labelOf(base.getStatus())
                        + "」，只有「待双方确认」的版本才能被反要约修改；请刷新后重试");
            }
            // 反要约 = 原要约失效：把上一份提案置为 REJECTED（时间线保留，只是不再活跃）。
            // ⚠️ 这里**只动这一个草稿行**，绝不触碰现行生效版本与主档状态（㉛：谈成之前交易照常）。
            markRejectedByCounterOffer(base, side, operatorId);
            newVersionId = agreementService.createVersionFrom(agreementId, baseId, dto.getChangeReason(),
                    sessionTenantId, operatorId);
        }

        // 协商时间线：记下"这一版是谁提的"与协商留言
        AgreementVersion proposal = new AgreementVersion();
        proposal.setId(newVersionId);
        proposal.setProposedBySide(side.name());
        proposal.setProposedByPerson(operatorId);
        proposal.setProposalNote(trimToNull(dto.getProposalNote()));
        proposal.setUpdateBy(operatorId);
        versionMapper.updateProposal(proposal);

        log.info("协议协商提案已提交: agreementId={}, 新版本ID={}, 提案方={}, 基准版本={}, 原因={}, 留言={}",
                agreementId, newVersionId, side, baseId, dto.getChangeReason(), dto.getProposalNote());
        return newVersionId;
    }

    @Override
    public AgreementVersionDiffVO diff(Long versionId, Long againstVersionId, Long sessionTenantId) {
        AgreementVersion version = requireVersion(versionId);
        Agreement agreement = requireVisibleAgreement(version.getAgreementId(), sessionTenantId);
        AgreementVersion against;
        if (againstVersionId != null) {
            against = requireVersion(againstVersionId);
            if (!agreement.getId().equals(against.getAgreementId())) {
                throw BusinessException.badRequest("对比的基准版本不属于本协议");
            }
        } else {
            against = previousVersion(agreement.getId(), version.getVersionNo());
        }
        String label = against == null
                ? "首版（没有更早的版本可比）"
                : "上一版（第 " + against.getVersionNo() + " 版）";
        return AgreementVersionDiff.compare(agreement.getId(), label, sideOfVersion(against), sideOfVersion(version));
    }

    // ══════════════════════════ 签署（§13.6） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementSignatureVO sign(Long versionId, AgreementSignDTO dto, Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVersion(versionId);
        Agreement agreement = requireVisibleAgreement(version.getAgreementId(), sessionTenantId);
        AgreementPartySide side = sideOf(agreement, sessionTenantId);
        AgreementSignatureRules.assertSignable(version, side);
        AgreementSignatureRules.assertRepresentedParty(agreement, side, dto.getRepresentedPartyId());
        AgreementSignatureRules.assertAuthority(dto.getAuthorityBasis());

        // ⚠️ 复用既有"本方确认"链路（同一套双签痕迹），**不另造确认机制**：
        //    库里的 partyX_confirmed_* 仍是"能否置为生效"的唯一依据；签署记录是更强的留痕。
        agreementService.confirm(versionId, sessionTenantId, operatorId);

        String hash = AgreementSnapshot.sha256(version.getSnapshotJson());
        AgreementSignature row = new AgreementSignature();
        // 系统级归属位显式写 0（裁定⑥）
        row.setTenantId(0L);
        row.setAgreementId(agreement.getId());
        row.setVersionId(version.getId());
        row.setPartySide(side.name());
        row.setPartyId(dto.getRepresentedPartyId());
        row.setSignerPartyTenantId(sessionTenantId);
        row.setSignerUserId(operatorId);
        row.setSignerPersonId(dto.getPersonId());
        row.setSignerName(trimToNull(dto.getSignerName()));
        row.setAuthorityBasis(dto.getAuthorityBasis().trim());
        row.setAuthorityEvidenceNo(trimToNull(dto.getAuthorityEvidenceNo()));
        row.setSignHash(hash);
        row.setSignedAt(LocalDateTime.now());
        row.setSignChannel(trimToNull(dto.getChannel()));
        row.setSignatureType(AgreementSignatureRules.TYPE_VERSION);
        row.setRemark(trimToNull(dto.getRemark()));
        row.setCreateBy(operatorId);
        signatureMapper.insert(row);

        log.info("协议已签署: agreementId={}, versionId={}, 本方={}, 代表主体={}, 签署人账号={}, 授权依据={}, 凭据号={}",
                agreement.getId(), version.getId(), side, dto.getRepresentedPartyId(), operatorId,
                row.getAuthorityBasis(), row.getAuthorityEvidenceNo());
        return toSignatureVO(row, hash, nameMap(Set.of(row.getPartyId()), true),
                nameMap(Set.of(row.getSignerPartyTenantId()), false), version.getVersionNo());
    }

    @Override
    public List<AgreementSignatureVO> listSignatures(Long versionId, Long sessionTenantId) {
        AgreementVersion version = requireVersion(versionId);
        requireVisibleAgreement(version.getAgreementId(), sessionTenantId);
        List<AgreementSignature> rows = signatureMapper.selectList(new LambdaQueryWrapper<AgreementSignature>()
                .eq(AgreementSignature::getVersionId, versionId)
                .orderByAsc(AgreementSignature::getSignedAt)
                .orderByAsc(AgreementSignature::getId));
        String currentHash = AgreementSnapshot.sha256(version.getSnapshotJson());
        Map<Long, String> partyNames = nameMap(idsOf(rows, true), true);
        Map<Long, String> tenantNames = nameMap(idsOf(rows, false), false);
        List<AgreementSignatureVO> list = new ArrayList<>(rows.size());
        for (AgreementSignature row : rows) {
            list.add(toSignatureVO(row, currentHash, partyNames, tenantNames, version.getVersionNo()));
        }
        return list;
    }

    // ══════════════════════════ 终止（§13.7） ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementTerminationVO requestTermination(Long agreementId, AgreementTerminationCreateDTO dto,
                                                     Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisibleAgreement(agreementId, sessionTenantId);
        AgreementTerminationSource source = AgreementTerminationSource.parse(dto.getSource());
        AgreementPartySide side = sideOf(agreement, sessionTenantId);
        AgreementTerminationRules.assertRequestable(agreement, source, side, sessionTenantId);

        boolean claimBreach = Boolean.TRUE.equals(dto.getClaimCounterpartyBreach());
        if (claimBreach && trimToNull(dto.getBreachNote()) == null) {
            // 主张违约是要拿出去举证的话，必须说清依据了哪件事；否则只是一句情绪
            throw BusinessException.badRequest("你勾选了「主张对方违约」，请填写具体情形"
                    + "（如「货款逾期 30 天未付」）——这只是双方之间的主张，平台不认定违约，"
                    + "但得让记录说得清你在主张什么");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean needConfirm = source.isRequiresCounterpartyConfirm();

        AgreementTermination row = new AgreementTermination();
        // 系统级归属位显式写 0（裁定⑥）
        row.setTenantId(0L);
        row.setAgreementId(agreement.getId());
        row.setVersionId(agreement.getCurrentVersionId());
        row.setSource(source.name());
        row.setStatus(needConfirm ? AgreementTerminationStatus.PENDING.getCode()
                : AgreementTerminationStatus.CONFIRMED.getCode());
        row.setRequestedBy(operatorId);
        row.setRequestedSide(side.name());
        row.setRequestedAt(now);
        row.setBasisText(dto.getBasisText().trim());
        row.setClaimCounterpartyBreach(claimBreach);
        row.setBreachNote(trimToNull(dto.getBreachNote()));
        // 「停止履行」的时刻：不传 = 立即（⚠️ 只表示停止履行，不代表结清或免责）
        row.setStopPerformanceAt(dto.getStopPerformanceAt() == null ? now : dto.getStopPerformanceAt());
        row.setEffectiveAt(needConfirm ? null : now);
        row.setCounterpartyObjection(false);
        row.setCreateBy(operatorId);
        terminationMapper.insert(row);

        if (!needConfirm) {
            applyAgreementTerminated(agreement, row, operatorId, now);
        }
        log.info("协议终止已发起: agreementId={}, terminationId={}, 来源={}, 发起方={}, 需对方确认={}, 依据={}, 主张违约={}",
                agreement.getId(), row.getId(), source, side, needConfirm, row.getBasisText(), claimBreach);
        return assembleTerminations(List.of(row), sessionTenantId, operatorId).get(0);
    }

    @Override
    public List<AgreementTerminationVO> listTerminations(Long agreementId, Long sessionTenantId) {
        requireVisibleAgreement(agreementId, sessionTenantId);
        List<AgreementTermination> rows = terminationMapper.selectList(
                new LambdaQueryWrapper<AgreementTermination>()
                        .eq(AgreementTermination::getAgreementId, agreementId)
                        .orderByDesc(AgreementTermination::getRequestedAt)
                        .orderByDesc(AgreementTermination::getId));
        // 会话租户是发起方还是对方，由服务端判定（前端不用猜"我能不能点确认"）
        return assembleTerminations(rows, sessionTenantId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementTerminationVO confirmTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                                     Long sessionTenantId, Long operatorId) {
        AgreementTermination row = requireTermination(terminationId);
        Agreement agreement = requireVisibleAgreement(row.getAgreementId(), sessionTenantId);
        AgreementTerminationRules.assertCounterpartyActionable(row, operatorId);

        LocalDateTime now = LocalDateTime.now();
        AgreementTermination patch = new AgreementTermination();
        patch.setId(row.getId());
        patch.setStatus(AgreementTerminationStatus.CONFIRMED.getCode());
        patch.setCounterpartyActionBy(operatorId);
        patch.setCounterpartyActionAt(now);
        patch.setCounterpartyObjection(false);
        patch.setEffectiveAt(now);
        patch.setUpdateBy(operatorId);
        terminationMapper.updateById(patch);
        row = requireTermination(row.getId()); // 回读：后续"停止履行"的留痕取自库里的真实状态

        applyAgreementTerminated(agreement, row, operatorId, now);
        log.info("协议终止已被对方确认: agreementId={}, terminationId={}, 确认人={}",
                agreement.getId(), row.getId(), operatorId);
        return assembleTerminations(List.of(row), sessionTenantId, operatorId).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementTerminationVO objectTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                                    Long sessionTenantId, Long operatorId) {
        AgreementTermination row = requireTermination(terminationId);
        requireVisibleAgreement(row.getAgreementId(), sessionTenantId);
        AgreementTerminationRules.assertObjectable(row, operatorId);

        LocalDateTime now = LocalDateTime.now();
        AgreementTerminationStatus current = AgreementTerminationRules.statusOf(row);
        AgreementTermination patch = new AgreementTermination();
        patch.setId(row.getId());
        patch.setCounterpartyActionBy(operatorId);
        patch.setCounterpartyActionAt(now);
        patch.setCounterpartyObjection(true);
        patch.setObjectionReason(trimToNull(dto == null ? null : dto.getReason()) == null
                ? "对方提出异议" : dto.getReason().trim());
        patch.setUpdateBy(operatorId);
        if (current == AgreementTerminationStatus.PENDING) {
            // 待确认阶段提异议 ⇒ 协商一致的终止不成立，协议**继续有效**（主档一行都不动）
            patch.setStatus(AgreementTerminationStatus.OBJECTED.getCode());
        }
        // 已终止后提异议 ⇒ 只留痕：⚠️ 不回滚终止事实（"停止履行"已经发生，系统不假装它没发生）
        terminationMapper.updateById(patch);
        row = requireTermination(row.getId()); // 回读库里真实状态

        log.info("协议终止收到对方异议: agreementId={}, terminationId={}, 异议人={}, 当前状态={}, 原因={}",
                row.getAgreementId(), row.getId(), operatorId, row.getStatus(), patch.getObjectionReason());
        return assembleTerminations(List.of(row), sessionTenantId, operatorId).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementTerminationVO withdrawTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                                      Long sessionTenantId, Long operatorId) {
        AgreementTermination row = requireTermination(terminationId);
        requireVisibleAgreement(row.getAgreementId(), sessionTenantId);
        AgreementTerminationRules.assertWithdrawable(row, operatorId);

        AgreementTermination patch = new AgreementTermination();
        patch.setId(row.getId());
        patch.setStatus(AgreementTerminationStatus.WITHDRAWN.getCode());
        patch.setWithdrawnBy(operatorId);
        patch.setWithdrawnAt(LocalDateTime.now());
        patch.setWithdrawReason(trimToNull(dto == null ? null : dto.getReason()) == null
                ? "发起方撤回" : dto.getReason().trim());
        patch.setUpdateBy(operatorId);
        terminationMapper.updateById(patch);
        row = requireTermination(row.getId()); // 回读库里真实状态

        log.info("协议终止已由发起方撤回: agreementId={}, terminationId={}, 操作人={}, 原因={}",
                row.getAgreementId(), row.getId(), operatorId, patch.getWithdrawReason());
        return assembleTerminations(List.of(row), sessionTenantId, operatorId).get(0);
    }

    // ══════════════════════════ 内部：终止生效 ══════════════════════════

    /**
     * 把协议主档置为「已终止」= **停止履行**。
     *
     * <p>⚠️⚠️ 本方法**只做这一件事**：</p>
     * <ul>
     *   <li>不写任何版本的 snapshot / 条款 / 设定（版本是历史快照，终止不追溯）；</li>
     *   <li>不写任何单据与结算（阶段 B/C 接入后同样不许回写）；</li>
     *   <li>不记录任何"责任已了结"的东西（终止 ≠ 免责，见 {@link AgreementTerminationRules}）。</li>
     * </ul>
     * <p>主档置为 TERMINATED 的直接效果是 {@code AgreementRuntime} 不再用这份协议生成执行口径 ——
     * 这正是"停止履行"，也是唯一的效果。</p>
     */
    private void applyAgreementTerminated(Agreement agreement, AgreementTermination termination,
                                          Long operatorId, LocalDateTime now) {
        AgreementTerminationSource source = AgreementTerminationSource.of(termination.getSource());
        Agreement patch = new Agreement();
        patch.setId(agreement.getId());
        patch.setStatus(AgreementStatus.TERMINATED.getCode());
        patch.setTerminatedBy(operatorId);
        patch.setTerminatedAt(now);
        patch.setTerminateReason(terminationReason(source, termination));
        patch.setUpdateBy(operatorId);
        agreementMapper.updateById(patch);
        log.info("协议已置为终止（停止履行，不结清、不免责）: agreementId={}, 来源={}, 停止履行时刻={}",
                agreement.getId(), source == null ? termination.getSource() : source.name(),
                termination.getStopPerformanceAt());
    }

    private static String terminationReason(AgreementTerminationSource source, AgreementTermination termination) {
        String head = source == null ? termination.getSource() : source.getLabel();
        String text = head + "：" + (termination.getBasisText() == null ? "" : termination.getBasisText());
        if (Boolean.TRUE.equals(termination.getClaimCounterpartyBreach())) {
            text = text + "（主张对方违约，待双方自行协商或司法解决）";
        }
        // agreement.terminate_reason 是 VARCHAR(255)
        return text.length() <= 255 ? text : text.substring(0, 252) + "...";
    }

    // ══════════════════════════ 内部：取数（可见性一律走唯一构造处） ══════════════════════════

    /**
     * 取本会话可见的主档，否则 404。
     *
     * <p>⚠️ 协议三张新表都**不参与租户拦截器**（裁定⑥），"谁能看到"完全由这里判定。
     * 任何新方法都必须先过这一关，不许自己拼 {@code party_a_tenant_id = ? OR ...}。</p>
     */
    private Agreement requireVisibleAgreement(Long agreementId, Long sessionTenantId) {
        if (agreementId == null) {
            throw BusinessException.notFound("协议不存在或你不是本协议的任一缔约方");
        }
        Agreement agreement = agreementMapper.selectById(agreementId);
        AgreementVisibility.assertVisible(agreement, sessionTenantId);
        return agreement;
    }

    private AgreementVersion requireVersion(Long versionId) {
        AgreementVersion version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        return version;
    }

    private AgreementInvite requireInvite(Long inviteId) {
        AgreementInvite invite = inviteId == null ? null : inviteMapper.selectById(inviteId);
        if (invite == null) {
            throw BusinessException.notFound("邀请不存在");
        }
        return invite;
    }

    private AgreementTermination requireTermination(Long terminationId) {
        AgreementTermination row = terminationId == null ? null : terminationMapper.selectById(terminationId);
        if (row == null) {
            throw BusinessException.notFound("终止记录不存在");
        }
        return row;
    }

    // ══════════════════════════ 内部：协商链路 ══════════════════════════

    /** 当前**待双方确认**的草稿版本 ID（取版本号最大的那个）；没有则返回 null。 */
    private Long currentDraftVersionId(Long agreementId) {
        List<AgreementVersion> drafts = versionMapper.selectList(new LambdaQueryWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreementId)
                .eq(AgreementVersion::getStatus, AgreementVersionStatus.DRAFT.getCode())
                .orderByDesc(AgreementVersion::getVersionNo));
        return drafts.isEmpty() ? null : drafts.get(0).getId();
    }

    /** 版本号小于给定值的最近一个版本（"上一版"）；没有则 null（首版）。 */
    private AgreementVersion previousVersion(Long agreementId, Integer versionNo) {
        if (versionNo == null) {
            return null;
        }
        List<AgreementVersion> rows = versionMapper.selectList(new LambdaQueryWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreementId)
                .lt(AgreementVersion::getVersionNo, versionNo)
                .orderByDesc(AgreementVersion::getVersionNo));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 反要约：把被取代的那份草稿置为 REJECTED（"原要约失效"）。
     *
     * <p>⚠️ 时间线**不删**：这一行仍留在版本历史里，谁在第几轮提过什么一目了然。
     * ⚠️ 只动这一个草稿行 —— 现行生效版本与主档状态一行都不碰（㉛：谈成之前交易照常）。</p>
     */
    private void markRejectedByCounterOffer(AgreementVersion base, AgreementPartySide side, Long operatorId) {
        AgreementVersion patch = new AgreementVersion();
        patch.setId(base.getId());
        patch.setStatus(AgreementVersionStatus.REJECTED.getCode());
        patch.setUpdateBy(operatorId);
        String base_reason = trimToNull(base.getChangeReason());
        patch.setChangeReason((base_reason == null ? "" : base_reason + "；")
                + "已被" + sideLabel(side) + "的反要约（新版本）取代，本提案失效");
        versionMapper.updateById(patch);
        log.info("协商反要约：原提案失效（保留在时间线中）: versionId={}, 原提案方={}, 新提案方={}",
                base.getId(), base.getProposedBySide(), side.name());
    }

    private static VersionCreateDTO versionCreateDTO(String changeReason) {
        VersionCreateDTO dto = new VersionCreateDTO();
        dto.setChangeReason(changeReason);
        return dto;
    }

    // ══════════════════════════ 内部：唯一送达 ══════════════════════════

    /** 作废同一目标下仍然有效的邀请（"唯一送达"不允许两个有效链接并存）。 */
    private void revokePendingInvites(Long agreementId, Long targetTenantId, Long targetPartyId,
                                      Long operatorId, String reason) {
        List<AgreementInvite> pending = inviteMapper.selectList(new LambdaQueryWrapper<AgreementInvite>()
                .eq(AgreementInvite::getAgreementId, agreementId)
                .eq(AgreementInvite::getTargetTenantId, targetTenantId)
                .eq(AgreementInvite::getTargetPartyId, targetPartyId)
                .eq(AgreementInvite::getStatus, AgreementInviteStatus.PENDING.getCode()));
        for (AgreementInvite old : pending) {
            AgreementInvite patch = new AgreementInvite();
            patch.setId(old.getId());
            patch.setStatus(AgreementInviteStatus.REVOKED.getCode());
            patch.setRevokeBy(operatorId);
            patch.setRevokeAt(LocalDateTime.now());
            patch.setRevokeReason(reason);
            patch.setUpdateBy(operatorId);
            inviteMapper.updateById(patch);
            log.info("旧邀请已自动作废（发起方重新发起）: inviteId={}, agreementId={}", old.getId(), agreementId);
        }
    }

    /**
     * 只有**发起方那一端**才能撤回这份邀请。
     *
     * <p>发起方 = 目标方的对面（邀请上记的是"发给谁"，发起方就是另一端），
     * 因此不需要额外的列就能判定，也不会让"收到邀请的一方"把对方的邀请撤掉。</p>
     */
    private void assertInitiatorSide(AgreementInvite invite, Agreement agreement, Long sessionTenantId) {
        AgreementPartySide targetSide = AgreementPartySide.of(agreement, invite.getTargetTenantId());
        Long initiatorTenantId = targetSide == AgreementPartySide.A
                ? agreement.getPartyBTenantId() : agreement.getPartyATenantId();
        if (!sessionTenantId.equals(initiatorTenantId)) {
            throw BusinessException.forbidden("只有发起送达的一方才能撤回这份邀请");
        }
    }

    // ══════════════════════════ 内部：签署（§13.6） ══════════════════════════

    private Set<Long> idsOf(List<AgreementSignature> rows, boolean party) {
        Set<Long> ids = new LinkedHashSet<>();
        for (AgreementSignature row : rows) {
            Long v = party ? row.getPartyId() : row.getSignerPartyTenantId();
            if (v != null) {
                ids.add(v);
            }
        }
        return ids;
    }

    private Map<Long, String> nameMap(Set<Long> ids, boolean party) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<NameRow> rows = party ? agreementMapper.selectPartyNames(ids) : agreementMapper.selectTenantNames(ids);
        Map<Long, String> map = new LinkedHashMap<>();
        for (NameRow row : rows) {
            if (row.getId() != null) {
                map.putIfAbsent(row.getId(), row.getName() == null ? "" : row.getName());
            }
        }
        return map;
    }

    // ══════════════════════════ 内部：VO 组装 ══════════════════════════

    private List<AgreementInviteVO> assembleInvites(List<AgreementInvite> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> agreementIds = new LinkedHashSet<>();
        Set<Long> partyIds = new LinkedHashSet<>();
        Set<Long> tenantIds = new LinkedHashSet<>();
        Set<Long> versionIds = new LinkedHashSet<>();
        for (AgreementInvite row : rows) {
            addIfNotNull(agreementIds, row.getAgreementId());
            addIfNotNull(partyIds, row.getTargetPartyId());
            addIfNotNull(tenantIds, row.getTargetTenantId());
            addIfNotNull(versionIds, row.getVersionId());
        }
        Map<Long, Agreement> agreements = new LinkedHashMap<>();
        for (Agreement a : agreementMapper.selectBatchIds(agreementIds)) {
            agreements.put(a.getId(), a);
        }
        Map<Long, AgreementVersion> versions = new LinkedHashMap<>();
        if (!versionIds.isEmpty()) {
            for (AgreementVersion v : versionMapper.selectBatchIds(versionIds)) {
                versions.put(v.getId(), v);
            }
        }
        Map<Long, String> partyNames = nameMap(partyIds, true);
        Map<Long, String> tenantNames = nameMap(tenantIds, false);
        List<AgreementInviteVO> list = new ArrayList<>(rows.size());
        for (AgreementInvite row : rows) {
            list.add(toInviteVO(row, agreements.get(row.getAgreementId()),
                    versions.get(row.getVersionId()), partyNames, tenantNames));
        }
        return list;
    }

    /**
     * 组装邀请 VO。
     *
     * <p>⚠️ 明文 {@code token} 与 {@code shortLink} **不在这里下发** ——
     * 库中只有哈希，取不回来；它们只在 {@link #createInvite} 的响应里现给一次。</p>
     */
    private AgreementInviteVO toInviteVO(AgreementInvite row, Agreement agreement, AgreementVersion version,
                                         Map<Long, String> partyNames, Map<Long, String> tenantNames) {
        AgreementInviteVO vo = new AgreementInviteVO();
        vo.setId(row.getId());
        vo.setAgreementId(row.getAgreementId());
        if (agreement != null) {
            vo.setAgreementNo(agreement.getAgreementNo());
            vo.setAgreementTitle(agreement.getTitle());
        }
        vo.setVersionId(row.getVersionId());
        vo.setVersionNo(version == null ? null : version.getVersionNo());
        vo.setTargetPartyId(row.getTargetPartyId());
        vo.setTargetPartyName(partyNames.get(row.getTargetPartyId()));
        vo.setTargetTenantId(row.getTargetTenantId());
        vo.setTargetTenantName(tenantNames.get(row.getTargetTenantId()));
        vo.setTargetSide(row.getTargetSide());
        vo.setTargetSideLabel(sideLabel(safeSide(row.getTargetSide())));
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(AgreementInviteStatus.labelOf(row.getStatus()));
        vo.setChannel(row.getChannel());
        vo.setChannelLabel(AgreementInviteChannel.labelOf(row.getChannel()));
        vo.setExpiresAt(row.getExpiresAt());
        vo.setAcceptedBy(row.getAcceptedBy());
        vo.setAcceptedAt(row.getAcceptedAt());
        vo.setAcceptChannel(row.getAcceptChannel());
        vo.setAcceptedPartyId(row.getAcceptedPartyId());
        vo.setViewCount(row.getViewCount());
        vo.setFirstViewedBy(row.getFirstViewedBy());
        vo.setFirstViewedAt(row.getFirstViewedAt());
        vo.setFirstViewChannel(row.getFirstViewChannel());
        vo.setRevokeBy(row.getRevokeBy());
        vo.setRevokeAt(row.getRevokeAt());
        vo.setRevokeReason(row.getRevokeReason());
        vo.setCreatedBy(row.getCreateBy());
        vo.setCreatedAt(row.getCreateTime());
        vo.setTokenHint(row.getTokenHint());
        vo.setInviteCode(row.getInviteCode());
        // token / shortLink 刻意留空（只在下发那一刻由 createInvite 填）
        return vo;
    }

    private AgreementSignatureVO toSignatureVO(AgreementSignature row, String currentHash,
                                               Map<Long, String> partyNames, Map<Long, String> tenantNames,
                                               Integer versionNo) {
        AgreementSignatureVO vo = new AgreementSignatureVO();
        vo.setId(row.getId());
        vo.setAgreementId(row.getAgreementId());
        vo.setVersionId(row.getVersionId());
        vo.setVersionNo(versionNo);
        vo.setPartySide(row.getPartySide());
        vo.setPartySideLabel(sideLabel(safeSide(row.getPartySide())));
        vo.setPartyId(row.getPartyId());
        vo.setPartyName(partyNames.get(row.getPartyId()));
        vo.setSignerPartyTenantId(row.getSignerPartyTenantId());
        vo.setSignerPartyTenantName(tenantNames.get(row.getSignerPartyTenantId()));
        vo.setSignerUserId(row.getSignerUserId());
        vo.setSignerPersonId(row.getSignerPersonId());
        vo.setSignerName(row.getSignerName());
        vo.setAuthorityBasis(row.getAuthorityBasis());
        vo.setAuthorityEvidenceNo(row.getAuthorityEvidenceNo());
        vo.setSignHash(row.getSignHash());
        vo.setSignedAt(row.getSignedAt());
        vo.setSignChannel(row.getSignChannel());
        vo.setSignatureType(row.getSignatureType());
        vo.setRemark(row.getRemark());
        boolean effective = currentHash != null && currentHash.equalsIgnoreCase(row.getSignHash());
        vo.setEffective(effective);
        vo.setEffectiveNote(effective ? "对当前内容有效"
                : "这一版内容在签署之后被改过，本次签署已失效，请对最新内容重新签署");
        return vo;
    }

    private List<AgreementTerminationVO> assembleTerminations(List<AgreementTermination> rows,
                                                              Long sessionTenantId, Long operatorId) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> agreementIds = new LinkedHashSet<>();
        Set<Long> versionIds = new LinkedHashSet<>();
        for (AgreementTermination row : rows) {
            addIfNotNull(agreementIds, row.getAgreementId());
            addIfNotNull(versionIds, row.getVersionId());
        }
        Map<Long, Agreement> agreements = new LinkedHashMap<>();
        for (Agreement a : agreementMapper.selectBatchIds(agreementIds)) {
            agreements.put(a.getId(), a);
        }
        Map<Long, Integer> versionNos = new LinkedHashMap<>();
        if (!versionIds.isEmpty()) {
            for (AgreementVersion v : versionMapper.selectBatchIds(versionIds)) {
                versionNos.put(v.getId(), v.getVersionNo());
            }
        }
        List<AgreementTerminationVO> list = new ArrayList<>(rows.size());
        for (AgreementTermination row : rows) {
            Agreement agreement = agreements.get(row.getAgreementId());
            list.add(toTerminationVO(row, agreement, versionNos.get(row.getVersionId()), sessionTenantId, operatorId));
        }
        return list;
    }

    private AgreementTerminationVO toTerminationVO(AgreementTermination row, Agreement agreement, Integer versionNo,
                                                   Long sessionTenantId, Long operatorId) {
        AgreementTerminationVO vo = new AgreementTerminationVO();
        vo.setId(row.getId());
        vo.setAgreementId(row.getAgreementId());
        if (agreement != null) {
            vo.setAgreementNo(agreement.getAgreementNo());
            vo.setAgreementTitle(agreement.getTitle());
        }
        vo.setVersionId(row.getVersionId());
        vo.setVersionNo(versionNo);
        AgreementTerminationSource source = AgreementTerminationSource.of(row.getSource());
        vo.setSource(row.getSource());
        vo.setSourceLabel(source == null ? row.getSource() : source.getLabel());
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(AgreementTerminationStatus.labelOf(row.getStatus()));
        vo.setRequestedBy(row.getRequestedBy());
        vo.setRequestedSide(row.getRequestedSide());
        vo.setRequestedSideLabel(sideLabel(safeSide(row.getRequestedSide())));
        vo.setRequestedAt(row.getRequestedAt());
        vo.setBasisText(row.getBasisText());
        vo.setClaimCounterpartyBreach(row.getClaimCounterpartyBreach());
        vo.setBreachNote(row.getBreachNote());
        vo.setStopPerformanceAt(row.getStopPerformanceAt());
        vo.setEffectiveAt(row.getEffectiveAt());
        vo.setCounterpartyActionBy(row.getCounterpartyActionBy());
        vo.setCounterpartyActionAt(row.getCounterpartyActionAt());
        vo.setCounterpartyObjection(row.getCounterpartyObjection());
        vo.setObjectionReason(row.getObjectionReason());
        vo.setWithdrawnBy(row.getWithdrawnBy());
        vo.setWithdrawnAt(row.getWithdrawnAt());
        vo.setWithdrawReason(row.getWithdrawReason());
        vo.setCreatedAt(row.getCreateTime());
        // ⚠️ 固定提示：终止只表示停止履行，不结清、不免责（界面必须显示）
        vo.setExemptionNotice(AgreementTerminationRules.NO_EXEMPTION_NOTICE);

        // 能否表态由服务端判定（前端不用猜）。⚠️ 这些是**提示**，真正的把关仍在 Rules 里。
        AgreementTerminationStatus status = AgreementTerminationRules.statusOf(row);
        boolean isInitiator = operatorId != null && operatorId.equals(row.getRequestedBy());
        boolean sameSide = sessionTenantId != null && agreement != null
                && sessionTenantId.equals(sideTenant(agreement, safeSide(row.getRequestedSide())));
        vo.setCanConfirm(status == AgreementTerminationStatus.PENDING && !isInitiator && sessionTenantId != null);
        vo.setCanObject(operatorId != null && !isInitiator
                && (status == AgreementTerminationStatus.PENDING || status == AgreementTerminationStatus.CONFIRMED));
        vo.setCanWithdraw(status == AgreementTerminationStatus.PENDING && isInitiator);
        if (sameSide) {
            // 发起方自己是点不了"确认"的，这里再钉一次（与 Rules 同口径的展示层防御）
            vo.setCanConfirm(false);
        }
        return vo;
    }

    /** 组装某一版的 diff 输入（条款来自快照，设定/文字/履约方式来自版本行）。 */
    private AgreementVersionDiff.Side sideOfVersion(AgreementVersion version) {
        if (version == null) {
            return null;
        }
        Map<String, String> settingLabels = new LinkedHashMap<>();
        for (AgreementSettingDef def : runtime.definitions()) {
            if (def.getSettingKey() != null) {
                settingLabels.putIfAbsent(def.getSettingKey(),
                        def.getLabel() == null ? def.getSettingKey() : def.getLabel());
            }
        }
        return new AgreementVersionDiff.Side(
                version.getId(),
                version.getVersionNo(),
                version.getSnapshotJson(),
                settingMapper.selectList(new LambdaQueryWrapper<AgreementSetting>()
                        .eq(AgreementSetting::getVersionId, version.getId())
                        .orderByAsc(AgreementSetting::getSettingKey)),
                narrativeMapper.selectList(new LambdaQueryWrapper<AgreementNarrative>()
                        .eq(AgreementNarrative::getVersionId, version.getId())
                        .orderByAsc(AgreementNarrative::getSort)),
                fulfillmentModeMapper.selectList(new LambdaQueryWrapper<AgreementFulfillmentMode>()
                        .eq(AgreementFulfillmentMode::getVersionId, version.getId())
                        .orderByAsc(AgreementFulfillmentMode::getSort)),
                settingLabels);
    }

    // ══════════════════════════ 内部：小工具 ══════════════════════════

    private static AgreementPartySide sideOf(Agreement agreement, Long sessionTenantId) {
        return AgreementPartySide.of(agreement, sessionTenantId);
    }

    private static AgreementPartySide safeSide(String name) {
        if (name == null) {
            return AgreementPartySide.NONE;
        }
        try {
            return AgreementPartySide.valueOf(name);
        } catch (IllegalArgumentException e) {
            return AgreementPartySide.NONE;
        }
    }

    private static Long sideTenant(Agreement agreement, AgreementPartySide side) {
        if (agreement == null || side == null) {
            return null;
        }
        return switch (side) {
            case A -> agreement.getPartyATenantId();
            case B -> agreement.getPartyBTenantId();
            case NONE -> null;
        };
    }

    private static String sideLabel(AgreementPartySide side) {
        if (side == null) {
            return "未知";
        }
        return switch (side) {
            case A -> "甲方";
            case B -> "乙方";
            case NONE -> "非缔约方";
        };
    }

    private static boolean isTerminated(Agreement agreement) {
        return agreement != null && agreement.getStatus() != null
                && agreement.getStatus() == AgreementStatus.TERMINATED.getCode();
    }

    private static void addIfNotNull(Set<Long> set, Long v) {
        if (v != null) {
            set.add(v);
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
