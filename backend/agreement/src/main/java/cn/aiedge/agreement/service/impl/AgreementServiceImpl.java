package cn.aiedge.agreement.service.impl;

import cn.aiedge.agreement.domain.AgreementChangeOrder;
import cn.aiedge.agreement.domain.AgreementInvariants;
import cn.aiedge.agreement.domain.AgreementListConditions;
import cn.aiedge.agreement.domain.AgreementPartySide;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementVisibility;
import cn.aiedge.agreement.dto.AgreementCreateDTO;
import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.dto.AgreementSaveResultVO;
import cn.aiedge.agreement.dto.AgreementTermVO;
import cn.aiedge.agreement.dto.AgreementUpdateDTO;
import cn.aiedge.agreement.dto.AgreementVO;
import cn.aiedge.agreement.dto.AgreementVersionVO;
import cn.aiedge.agreement.dto.NameRow;
import cn.aiedge.agreement.dto.TermSelectDTO;
import cn.aiedge.agreement.dto.VersionCreateDTO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementType;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementTermMapper;
import cn.aiedge.agreement.mapper.AgreementTermOptionMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.serial.BizNumberGeneratorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 协议服务实现。
 *
 * <h3>⚠️ 跨租户推单能力本期**默认关闭**（裁定④）</h3>
 * 只有存在覆盖双方且 {@code ACTIVE} 的协议时才放行跨租户推单（fail-closed），
 * 且协议**未约定**某条款时不自动执行任何赔偿/扣款、挂人工，不给平台默认值。
 * <b>本期（阶段 A）不实现推单链路</b> —— 推单属阶段 C，依赖 §4 的服务间验签投递；
 * 这里只固化"默认关闭"的口径，避免后人误以为"有协议主档就能推单"。
 *
 * <h3>可见性</h3>
 * 一律走 {@link AgreementVisibility}（唯一构造处）。本类里**不许**出现
 * 手写的 {@code party_a_tenant_id = ? OR party_b_tenant_id = ?}（裁定⑥）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    /** 号段业务类型：复用系统唯一号段服务（biz_number_sequence，行锁 + 按日重置）。 */
    private static final String BIZ_TYPE = "AGREEMENT";

    /** 协议编号号段所属租户：与 biz_number_sequence 现有全部行的既成口径一致（全局流水不按租户切分）。 */
    private static final Long SEQ_TENANT = 1L;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AgreementMapper agreementMapper;
    private final AgreementVersionMapper versionMapper;
    private final AgreementTermMapper termMapper;
    private final AgreementTermOptionMapper optionMapper;
    /**
     * 内容层三张表：只在"发起变更/反要约时把基准版的内容复制到新版本"这一处用到。
     *
     * <p>为什么服务层要直接持有它们：内容层是"可执行内核"（§13.1），
     * 变更若不带上设定/文字/履约方式，新版本一上来就是"什么都没约定"。
     * 复制逻辑放在本类（版本是怎么来的，就由谁负责把内容带过来），
     * 而不是让调用方各自复制一遍。</p>
     */
    private final AgreementSettingMapper settingMapper;
    private final AgreementNarrativeMapper narrativeMapper;
    private final AgreementFulfillmentModeMapper fulfillmentModeMapper;
    private final BizNumberGeneratorService bizNumberGeneratorService;

    // ══════════════════════════ 查询 ══════════════════════════

    @Override
    public Page<AgreementVO> page(AgreementQuery query, Long sessionTenantId) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 20L : Math.min(query.getSize(), 200L);

        // 可见性（唯一构造处，裁定⑥）+ 类型范围（agreementScope / agreementType）都在这里组装：
        // ⚠️ 必须是 **SQL 条件**，不能取回后在内存里筛 —— 分页拦截器按 Wrapper 条件算 count，
        //    内存筛选会让 total 与显示行数不一致（本次修复的正是这一点）。
        LambdaQueryWrapper<Agreement> wrapper = AgreementListConditions.build(query, sessionTenantId);
        if (trimToNull(query.getStatus()) != null) {
            wrapper.eq(Agreement::getStatus, requireStatus(query.getStatus()).getCode());
        }
        String keyword = trimToNull(query.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(Agreement::getAgreementNo, keyword)
                    .or().like(Agreement::getTitle, keyword));
        }
        wrapper.orderByDesc(Agreement::getCreateTime).orderByDesc(Agreement::getId);

        Page<Agreement> page = agreementMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AgreementVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVoList(page.getRecords()));
        return result;
    }

    @Override
    public AgreementVO detail(Long id, Long sessionTenantId) {
        Agreement agreement = requireVisible(id, sessionTenantId);
        AgreementVO vo = toVoList(List.of(agreement)).get(0);
        if (agreement.getCurrentVersionId() != null) {
            AgreementVersion version = versionMapper.selectById(agreement.getCurrentVersionId());
            if (version != null) {
                vo.setCurrentVersion(toVersionVO(version));
            }
        }
        return vo;
    }

    @Override
    public List<AgreementVersionVO> listVersions(Long id, Long sessionTenantId) {
        // 先过可见性：否则任何租户都能凭 id 读别家协议的版本与快照（举证材料，最敏感）
        requireVisible(id, sessionTenantId);
        List<AgreementVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>()
                        .eq(AgreementVersion::getAgreementId, id)
                        .orderByDesc(AgreementVersion::getVersionNo));
        List<AgreementVersionVO> list = new ArrayList<>(versions.size());
        for (AgreementVersion v : versions) {
            list.add(toVersionVO(v));
        }
        return list;
    }

    @Override
    public AgreementVersionVO versionDetail(Long versionId, Long sessionTenantId) {
        AgreementVersion version = requireVersion(versionId);
        requireVisible(version.getAgreementId(), sessionTenantId);
        return toVersionVO(version);
    }

    @Override
    public Optional<AgreementTerm> findAgreedTerm(Long versionId, String termCode, Long sessionTenantId) {
        AgreementVersion version = requireVersion(versionId);
        requireVisible(version.getAgreementId(), sessionTenantId);
        List<AgreementTerm> terms = listTerms(versionId);
        // 未约定 ⇒ 返回空。**绝不**回落到平台默认值（§3.4.4d1）。
        return AgreementInvariants.findAgreedTerm(terms, termCode);
    }

    // ══════════════════════════ 写入 ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgreementCreateDTO dto, Long sessionTenantId, Long operatorId) {
        if (sessionTenantId == null) {
            throw BusinessException.unauthorized("无法确定当前登录租户，请重新登录后再操作");
        }
        AgreementType type = requireType(dto.getAgreementType());
        validateParties(type, dto.getPartyAId(), dto.getPartyATenantId(), dto.getPartyBId(), dto.getPartyBTenantId());
        checkPeriod(toStartOfDay(dto.getEffectiveFrom()), toEndOfDay(dto.getEffectiveTo()));

        Agreement agreement = new Agreement();
        // ⚠️ 主档是**系统级**：tenant_id 必须显式为 0。
        //    不写会让 MetaObjectHandler 填入**会话租户**，另一端租户立刻查不到这份协议（裁定⑥）。
        agreement.setTenantId(0L);
        agreement.setAgreementNo(bizNumberGeneratorService.nextNumber(BIZ_TYPE, SEQ_TENANT, "zh_CN"));
        agreement.setAgreementType(type.name());
        agreement.setTitle(dto.getTitle().trim());
        agreement.setPartyAId(dto.getPartyAId());
        agreement.setPartyATenantId(dto.getPartyATenantId());
        agreement.setPartyBId(dto.getPartyBId());
        agreement.setPartyBTenantId(dto.getPartyBTenantId());
        agreement.setStatus(AgreementStatus.DRAFT.getCode());
        agreement.setEffectiveFrom(toStartOfDay(dto.getEffectiveFrom()));
        agreement.setEffectiveTo(toEndOfDay(dto.getEffectiveTo()));
        agreement.setCreateBy(operatorId);
        agreementMapper.insert(agreement);

        // 每个协议必有一个 DRAFT 版本（v1）：条款写在版本上，主档只是索引与状态。
        AgreementVersion version = new AgreementVersion();
        version.setAgreementId(agreement.getId());
        version.setVersionNo(1);
        version.setStatus(AgreementVersionStatus.DRAFT.getCode());
        version.setEffectiveFrom(agreement.getEffectiveFrom());
        version.setEffectiveTo(agreement.getEffectiveTo());
        version.setCreateBy(operatorId);
        // 协商时间线（§13.4）：首次起草也算"第一轮提案"，必须记下是谁提的 ——
        // 否则对方反要约之后，时间线的第一格没有归属，看不出链条的起点。
        version.setProposedBySide(AgreementPartySide.of(agreement, sessionTenantId).name());
        version.setProposedByPerson(operatorId);
        version.setSnapshotJson(AgreementSnapshot.build(agreement, 1, List.of(), List.of(),
                agreement.getEffectiveFrom(), agreement.getEffectiveTo()));
        versionMapper.insertVersion(version);

        log.info("协议草稿已创建: id={}, no={}, type={}, 甲方租户={}, 乙方租户={}, 创建租户={}",
                agreement.getId(), agreement.getAgreementNo(), type.name(),
                agreement.getPartyATenantId(), agreement.getPartyBTenantId(), sessionTenantId);
        return agreement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementSaveResultVO update(Long id, AgreementUpdateDTO dto, Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisible(id, sessionTenantId);
        if (!isStatus(agreement, AgreementStatus.DRAFT)) {
            throw BusinessException.badRequest("只有「洽谈中」的协议才能修改；已生效的协议要改请「发起变更」，"
                    + "由双方重新确认后生成新版本（现行协议在谈成之前继续有效）");
        }

        // ── 主档字段 ──
        LocalDateTime from = dto.getEffectiveFrom() == null ? agreement.getEffectiveFrom()
                : toStartOfDay(dto.getEffectiveFrom());
        LocalDateTime to = dto.getEffectiveTo() == null ? agreement.getEffectiveTo()
                : toEndOfDay(dto.getEffectiveTo());
        checkPeriod(from, to);
        String title = trimToNull(dto.getTitle()) == null ? agreement.getTitle() : dto.getTitle().trim();
        agreement.setTitle(title);
        agreement.setEffectiveFrom(from);
        agreement.setEffectiveTo(to);
        agreement.setUpdateBy(operatorId);
        agreementMapper.updateById(agreement);

        // ── 条款：写在指定 DRAFT 版本上（不传 versionId 则写当前草稿版本）──
        List<AgreementTermOption> options = allOptions();
        AgreementVersion version = resolveDraftVersion(agreement, dto.getVersionId());
        List<AgreementTerm> terms = writeTerms(agreement, version, dto.getTerms(), operatorId, options);

        // ── 快照同步：内容变了才重写，且重写会清空双方确认痕迹（双签针对内容）──
        String newSnapshot = AgreementSnapshot.build(agreement, version.getVersionNo(), terms, options, from, to);
        if (!newSnapshot.equals(version.getSnapshotJson())) {
            AgreementVersion patch = new AgreementVersion();
            patch.setId(version.getId());
            patch.setSnapshotJson(newSnapshot);
            patch.setEffectiveFrom(from);
            patch.setEffectiveTo(to);
            patch.setUpdateBy(operatorId);
            int rows = versionMapper.rewriteDraftSnapshot(patch);
            if (rows == 0) {
                // 理论上不可达（前面已判 DRAFT）；真发生说明并发下有别的请求把它置生效了
                throw BusinessException.badRequest("该版本已不是草稿状态，条款未能保存；请刷新后重新发起变更");
            }
            version.setSnapshotJson(newSnapshot);
            version.setPartyAConfirmedBy(null);
            version.setPartyAConfirmedAt(null);
            version.setPartyASignHash(null);
            version.setPartyBConfirmedBy(null);
            version.setPartyBConfirmedAt(null);
            version.setPartyBSignHash(null);
            log.info("协议草稿条款已更新（原确认痕迹已清空，需重新确认）: agreementId={}, versionId={}, 条款数={}",
                    agreement.getId(), version.getId(), terms.size());
        }

        // ── 回执：让用户一眼看到"还差哪些必填项" ──
        Set<String> requiredCodes = AgreementInvariants.requiredTermCodes(options);
        List<String> missing = AgreementInvariants.missingRequiredTermCodes(terms, requiredCodes);
        List<String> missingParams = AgreementInvariants.missingParams(terms, options);
        AgreementSaveResultVO result = new AgreementSaveResultVO();
        result.setId(agreement.getId());
        result.setVersionId(version.getId());
        result.setMissingRequiredTerms(missing);
        result.setMissingRequiredTermNames(termNames(options, missing));
        result.setMissingParams(missingParams);
        result.setReadyToActivate(missing.isEmpty() && missingParams.isEmpty()
                && version.getPartyAConfirmedAt() != null && version.getPartyBConfirmedAt() != null);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long sessionTenantId) {
        Agreement agreement = requireVisible(id, sessionTenantId);
        if (!isStatus(agreement, AgreementStatus.DRAFT)) {
            throw BusinessException.badRequest("只有「洽谈中」的协议才能删除；已生效的协议请走终止流程，"
                    + "保留版本快照以备举证");
        }
        List<AgreementVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<AgreementVersion>().eq(AgreementVersion::getAgreementId, id));
        for (AgreementVersion v : versions) {
            termMapper.delete(new LambdaQueryWrapper<AgreementTerm>().eq(AgreementTerm::getVersionId, v.getId()));
            versionMapper.deleteById(v.getId());
        }
        agreementMapper.deleteById(id);
        log.info("协议草稿已删除: id={}, 版本数={}", id, versions.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(Long id, VersionCreateDTO dto, Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisible(id, sessionTenantId);
        AgreementChangeOrder.assertChangeable(isStatus(agreement, AgreementStatus.TERMINATED));
        if (isStatus(agreement, AgreementStatus.DRAFT)
                && agreement.getCurrentVersionId() == null) {
            throw BusinessException.badRequest("本协议还没有生效版本，请直接在当前草稿版本上约定条款");
        }
        assertNoOtherDraft(id);

        // 阶段修改（履约中变更）：以**现行生效版本**为基准
        AgreementVersion source = agreement.getCurrentVersionId() == null
                ? null : versionMapper.selectById(agreement.getCurrentVersionId());
        return insertCopiedVersion(agreement, source, dto.getChangeReason().trim(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersionFrom(Long id, Long sourceVersionId, String changeReason,
                                  Long sessionTenantId, Long operatorId) {
        Agreement agreement = requireVisible(id, sessionTenantId);
        AgreementChangeOrder.assertChangeable(isStatus(agreement, AgreementStatus.TERMINATED));
        assertNoOtherDraft(id);

        // 反要约（§13.4）：以**对方提的那一版**为基准改，而不是以现行生效版本为基准 ——
        // 多轮协商里"在上一份提案上改"才是真实对话；拿现行生效版重来会把上一轮的让步丢掉。
        AgreementVersion base = requireVersion(sourceVersionId);
        AgreementChangeOrder.assertBaseVersion(id, base);
        if (changeReason == null || changeReason.isBlank()) {
            throw BusinessException.badRequest("请填写变更原因：这一版改了什么、为什么要改");
        }
        return insertCopiedVersion(agreement, base, changeReason.trim(), operatorId);
    }

    // ══════════════════════════ 双签与生效 ══════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long versionId, Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVersion(versionId);
        Agreement agreement = requireVisible(version.getAgreementId(), sessionTenantId);
        if (!AgreementInvariants.isDraft(version.getStatus())) {
            throw BusinessException.badRequest("该版本当前状态为「" + AgreementVersionStatus.labelOf(version.getStatus())
                    + "」，不能确认签署");
        }
        AgreementPartySide side = AgreementPartySide.of(agreement, sessionTenantId);
        if (side == AgreementPartySide.NONE) {
            // 可见性已保证是其中一端，这里只是纵深防御（例如主档两端被并发改动）
            throw BusinessException.forbidden("你不是本协议的缔约方，不能代为确认");
        }
        // 双方确认的是**同一份内容**：签名哈希取当前快照正文，生效时会再核对一次
        String hash = AgreementSnapshot.sha256(version.getSnapshotJson());
        AgreementVersion patch = new AgreementVersion();
        patch.setId(version.getId());
        patch.setUpdateBy(operatorId);
        if (side == AgreementPartySide.A) {
            patch.setPartyAConfirmedBy(operatorId);
            patch.setPartyAConfirmedAt(LocalDateTime.now());
            patch.setPartyASignHash(hash);
        } else {
            patch.setPartyBConfirmedBy(operatorId);
            patch.setPartyBConfirmedAt(LocalDateTime.now());
            patch.setPartyBSignHash(hash);
        }
        versionMapper.updateById(patch);
        log.info("协议版本本方已确认: agreementId={}, versionId={}, 本方={}, 确认人={}",
                agreement.getId(), versionId, side, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long versionId, String reason, Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVersion(versionId);
        Agreement agreement = requireVisible(version.getAgreementId(), sessionTenantId);
        AgreementInvariants.assertRejectable(version);

        AgreementVersion patch = new AgreementVersion();
        patch.setId(version.getId());
        patch.setStatus(AgreementVersionStatus.REJECTED.getCode());
        patch.setUpdateBy(operatorId);
        if (trimToNull(reason) != null) {
            String base = trimToNull(version.getChangeReason());
            patch.setChangeReason((base == null ? "" : base + "；") + "否决意见：" + reason.trim());
        }
        versionMapper.updateById(patch);

        // ⚠️⚠️ 这里**故意什么都不做**：不碰 agreement.status、不碰 currentVersionId、
        //   不碰既有的 ACTIVE 版本。变更谈成之前，交易必须仍按现行版本执行（§3.4.4d2）。
        //   这是本模块最容易做错的一处，tools/verify-agreement.cjs 在真机上钉死它。
        log.info("协议版本被否决: agreementId={}, versionId={}, 否决人={}, 原因={}（现行版本继续有效）",
                agreement.getId(), versionId, operatorId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(Long versionId, Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVersion(versionId);
        Agreement agreement = requireVisible(version.getAgreementId(), sessionTenantId);
        List<AgreementTermOption> options = allOptions();
        List<AgreementTerm> terms = listTerms(versionId);
        Set<String> requiredCodes = AgreementInvariants.requiredTermCodes(options);

        // 不变量 3：必填条款没选完不许生效；不变量 2：双签缺一不可（assertActivatable 一并管）
        List<String> missingTerms = AgreementInvariants.missingRequiredTermCodes(terms, requiredCodes);
        List<String> missingParams = AgreementInvariants.missingParams(terms, options);
        AgreementInvariants.assertActivatable(version, missingTerms, missingParams);

        // 双方确认的必须是**当前这份**内容：否则会出现"A 确认后被改条款仍算确认齐全"
        String hash = AgreementSnapshot.sha256(version.getSnapshotJson());
        if (!hash.equals(version.getPartyASignHash()) || !hash.equals(version.getPartyBSignHash())) {
            throw BusinessException.badRequest("双方确认的内容与当前条款不一致（确认之后条款被修改过），"
                    + "请双方对最新条款重新确认后再置为生效");
        }

        // 不变量 6：同一对主体之间同时只能有一份生效协议（避免对同一段合作给出互相矛盾的约定）
        assertNoOtherActiveAgreement(agreement);

        // 旧 ACTIVE → SUPERSEDED（必须先旧后新：uk_agreement_version_active 只允许一行 status=1）
        versionMapper.update(null, new LambdaUpdateWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreement.getId())
                .eq(AgreementVersion::getStatus, AgreementVersionStatus.ACTIVE.getCode())
                .set(AgreementVersion::getStatus, AgreementVersionStatus.SUPERSEDED.getCode())
                .set(AgreementVersion::getUpdateBy, operatorId));

        AgreementVersion patch = new AgreementVersion();
        patch.setId(version.getId());
        patch.setStatus(AgreementVersionStatus.ACTIVE.getCode());
        patch.setUpdateBy(operatorId);
        versionMapper.updateById(patch);

        // 主档指向新版本并置为生效
        Agreement patchAgreement = new Agreement();
        patchAgreement.setId(agreement.getId());
        patchAgreement.setCurrentVersionId(version.getId());
        patchAgreement.setStatus(AgreementStatus.ACTIVE.getCode());
        patchAgreement.setEffectiveFrom(version.getEffectiveFrom());
        patchAgreement.setEffectiveTo(version.getEffectiveTo());
        patchAgreement.setUpdateBy(operatorId);
        agreementMapper.updateById(patchAgreement);

        // ⚠️ 变更不追溯（§3.4.4d2 规定 4）：本方法**只写协议自己的三张表**，
        //   不回写任何已有单据/结算。阶段 B/C 接单据后，一律取"下单时刻生效的那一版"，
        //   不得因为新版本生效去改历史单据。
        log.info("协议已生效: agreementId={}, versionId={}, 版本号={}, 操作人={}",
                agreement.getId(), version.getId(), version.getVersionNo(), operatorId);
    }

    // ══════════════════════════ 内部 ══════════════════════════

    /** 取得本会话可见的主档，否则 404（存在性本身也是敏感信息，见 {@link AgreementVisibility}）。 */
    private Agreement requireVisible(Long id, Long sessionTenantId) {
        if (id == null) {
            throw BusinessException.notFound("协议不存在或你不是本协议的任一缔约方");
        }
        Agreement agreement = agreementMapper.selectById(id);
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

    /** 找出条款要写入的那个 DRAFT 版本：优先用传入的 versionId，否则用当前唯一的草稿版本。 */
    private AgreementVersion resolveDraftVersion(Agreement agreement, Long versionId) {
        AgreementVersion version;
        if (versionId != null) {
            version = requireVersion(versionId);
            if (!agreement.getId().equals(version.getAgreementId())) {
                throw BusinessException.badRequest("传入的版本不属于本协议");
            }
        } else {
            List<AgreementVersion> drafts = versionMapper.selectList(new LambdaQueryWrapper<AgreementVersion>()
                    .eq(AgreementVersion::getAgreementId, agreement.getId())
                    .eq(AgreementVersion::getStatus, AgreementVersionStatus.DRAFT.getCode())
                    .orderByDesc(AgreementVersion::getVersionNo));
            if (drafts.isEmpty()) {
                throw BusinessException.badRequest("本协议没有可编辑的草稿版本，请先「发起变更」");
            }
            version = drafts.get(0);
        }
        // 不变量 1：已生效版本只读（对 ACTIVE 写 snapshot/条款一律被拒）
        AgreementInvariants.assertVersionMutable(version);
        return version;
    }

    /**
     * 同时只允许有一个未定稿的草稿版本，避免"两个草稿各自谈"导致每次生效都推翻对方。
     *
     * <p>反要约（§13.4）也走这条约束：对方修改 = 新建 DRAFT 版本之前，
     * 上一份提案必须先被置为 REJECTED（"原要约失效"），因此时间线上永远只有一个活跃草稿。</p>
     */
    private void assertNoOtherDraft(Long agreementId) {
        Long draftCount = versionMapper.selectCount(new LambdaQueryWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreementId)
                .eq(AgreementVersion::getStatus, AgreementVersionStatus.DRAFT.getCode()));
        if (draftCount != null && draftCount > 0) {
            throw BusinessException.badRequest("已有一个待双方确认的草稿版本，请先把它谈定（置生效或被否决）再发起新的变更");
        }
    }

    /**
     * 从基准版本复制出一个新的 DRAFT 版本（**发起变更与反要约共用这一条链路**）。
     *
     * <p>变更是"在现状上改"，不是从空白重新谈：条款、字段设定、文字条款、履约方式集合
     * 一并从基准版复制过来，双方在新版本上只改有分歧的那几项。</p>
     *
     * <p>同时落下**变更单语义**（§13.7）：{@code origin_version_id} 记录本版从哪一版改出来，
     * {@code no_retroactive_note} 写明"本变更不追溯已发生的单据与结算"。
     * 首次签订（基准为空）时两者都为空。</p>
     */
    private Long insertCopiedVersion(Agreement agreement, AgreementVersion source,
                                     String changeReason, Long operatorId) {
        Long id = agreement.getId();
        int nextNo = nextVersionNo(id);

        AgreementVersion version = new AgreementVersion();
        version.setAgreementId(id);
        version.setVersionNo(nextNo);
        version.setStatus(AgreementVersionStatus.DRAFT.getCode());
        version.setChangeReason(changeReason);
        version.setEffectiveFrom(agreement.getEffectiveFrom());
        version.setEffectiveTo(agreement.getEffectiveTo());
        version.setCreateBy(operatorId);
        version.setOriginVersionId(source == null ? null : source.getId());
        version.setNoRetroactiveNote(source == null ? null : AgreementChangeOrder.NO_RETROACTIVE_NOTE);
        version.setSnapshotJson(source == null
                ? AgreementSnapshot.build(agreement, nextNo, List.of(), List.of(),
                        agreement.getEffectiveFrom(), agreement.getEffectiveTo())
                : source.getSnapshotJson());
        versionMapper.insertVersion(version);

        if (source != null) {
            copyTerms(agreement, source, version, operatorId);
            copyContent(agreement, source, version);
        }

        log.info("协议发起变更：agreementId={}, 新版本号={}, 版本ID={}, 基准版本={}, 原因={}（现行版本继续生效）",
                id, nextNo, version.getId(), source == null ? "无（首次）" : source.getId(), changeReason);
        return version.getId();
    }

    /** 复制条款选择（并重算快照：版本号与版本内条款 id 都变了）。 */
    private void copyTerms(Agreement agreement, AgreementVersion source, AgreementVersion version, Long operatorId) {
        for (AgreementTerm old : listTerms(source.getId())) {
            AgreementTerm copy = new AgreementTerm();
            copy.setAgreementId(agreement.getId());
            copy.setVersionId(version.getId());
            copy.setTermCode(old.getTermCode());
            copy.setOptionCode(old.getOptionCode());
            copy.setParamValue(old.getParamValue());
            termMapper.insert(copy);
        }
        List<AgreementTerm> newTerms = listTerms(version.getId());
        String snapshot = AgreementSnapshot.build(agreement, version.getVersionNo(), newTerms, allOptions(),
                version.getEffectiveFrom(), version.getEffectiveTo());
        AgreementVersion patch = new AgreementVersion();
        patch.setId(version.getId());
        patch.setSnapshotJson(snapshot);
        patch.setEffectiveFrom(version.getEffectiveFrom());
        patch.setEffectiveTo(version.getEffectiveTo());
        patch.setUpdateBy(operatorId);
        versionMapper.rewriteDraftSnapshot(patch);
    }

    /**
     * 复制**内容层**（字段设定版 / 文字版 / 履约方式集合）。
     *
     * <p>⚠️ 为什么必须复制：内容层是"可执行内核"（§13.1）——它才是下游真正消费的东西。
     * 若变更只复制条款而不复制设定，新版本一上来就是"什么都没约定"，
     * 生效后订单路由立刻取不到账期/佣金而挂人工，等于每次变更都把生意停一遍。
     * "变更是'在现状上改'，不是从空白重新谈"这句注释里的口径，对内容层同样成立。</p>
     *
     * <p>复制的是**取值本身**，不是"已由双方确认"这一状态：新版本仍需双方重新确认与签署
     * （复制过来的确认痕迹一律不复制，见下方逐字段说明）。</p>
     */
    private void copyContent(Agreement agreement, AgreementVersion source, AgreementVersion version) {
        for (AgreementSetting old : settingMapper.selectList(new LambdaQueryWrapper<AgreementSetting>()
                .eq(AgreementSetting::getVersionId, source.getId()))) {
            AgreementSetting copy = new AgreementSetting();
            copy.setTenantId(0L);
            copy.setAgreementId(agreement.getId());
            copy.setVersionId(version.getId());
            copy.setSettingKey(old.getSettingKey());
            copy.setValueType(old.getValueType());
            copy.setValueText(old.getValueText());
            copy.setValueNumber(old.getValueNumber());
            copy.setValueBool(old.getValueBool());
            copy.setValueDate(old.getValueDate());
            copy.setRemark(old.getRemark());
            settingMapper.insert(copy);
        }
        for (AgreementFulfillmentMode old : fulfillmentModeMapper.selectList(
                new LambdaQueryWrapper<AgreementFulfillmentMode>()
                        .eq(AgreementFulfillmentMode::getVersionId, source.getId()))) {
            AgreementFulfillmentMode copy = new AgreementFulfillmentMode();
            copy.setTenantId(0L);
            copy.setAgreementId(agreement.getId());
            copy.setVersionId(version.getId());
            copy.setMode(old.getMode());
            copy.setScopeNote(old.getScopeNote());
            copy.setSort(old.getSort());
            fulfillmentModeMapper.insert(copy);
        }
        for (AgreementNarrative old : narrativeMapper.selectList(new LambdaQueryWrapper<AgreementNarrative>()
                .eq(AgreementNarrative::getVersionId, source.getId()))) {
            AgreementNarrative copy = new AgreementNarrative();
            copy.setTenantId(0L);
            copy.setAgreementId(agreement.getId());
            copy.setVersionId(version.getId());
            copy.setSectionCode(old.getSectionCode());
            copy.setSectionTitle(old.getSectionTitle());
            copy.setContentText(old.getContentText());
            copy.setContentHash(old.getContentHash());
            copy.setSort(old.getSort());
            // ⚠️ 刻意**不复制** partyAConfirmedBy/At 与 partyBConfirmedBy/At：
            //    文字条款的"确认"是双方对**这一段文字**的表态，复制到新版本上等于替双方预先认了新版本
            narrativeMapper.insert(copy);
        }
    }

    private int nextVersionNo(Long agreementId) {
        List<AgreementVersion> all = versionMapper.selectList(new LambdaQueryWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreementId));
        return all.stream().map(AgreementVersion::getVersionNo)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(0) + 1;
    }

    private List<AgreementTerm> listTerms(Long versionId) {
        return termMapper.selectList(new LambdaQueryWrapper<AgreementTerm>()
                .eq(AgreementTerm::getVersionId, versionId)
                .orderByAsc(AgreementTerm::getTermCode));
    }

    /** 全量字典（条数少、结构稳定；协议模块所有校验都基于它，避免多次查库）。 */
    private List<AgreementTermOption> allOptions() {
        return optionMapper.selectList(new LambdaQueryWrapper<AgreementTermOption>()
                .orderByAsc(AgreementTermOption::getTermCode)
                .orderByAsc(AgreementTermOption::getSort));
    }

    /**
     * 写入某草稿版本的条款（**整份覆盖**）：先软删该版本已有条款，再插入本次选择。
     *
     * <p>整份覆盖而不是增量合并：前端把某一项清空时，语义应当是"回到未约定"，
     * 而不是"保持原值"。这正是"未约定就不自动执行"的前提。</p>
     */
    private List<AgreementTerm> writeTerms(Agreement agreement, AgreementVersion version,
                                           List<TermSelectDTO> selects, Long operatorId,
                                           List<AgreementTermOption> options) {
        termMapper.delete(new LambdaQueryWrapper<AgreementTerm>()
                .eq(AgreementTerm::getVersionId, version.getId()));

        List<AgreementTerm> result = new ArrayList<>();
        if (selects == null || selects.isEmpty()) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (TermSelectDTO s : selects) {
            String termCode = trimToNull(s.getTermCode());
            String optionCode = trimToNull(s.getOptionCode());
            if (termCode == null || optionCode == null) {
                // 前端只提交"已选"的行；出现只有类别没有选项的行说明数据不完整，明确报错而不是静默丢弃
                throw BusinessException.badRequest("条款「" + termCode + "」没有选择具体选项；未约定的条款请不要提交该行");
            }
            if (!seen.add(termCode)) {
                throw BusinessException.badRequest("条款「" + termCode + "」重复提交了多个选项");
            }
            AgreementTerm term = new AgreementTerm();
            term.setAgreementId(agreement.getId());
            term.setVersionId(version.getId());
            term.setTermCode(termCode);
            term.setOptionCode(optionCode);
            term.setParamValue(trimToNull(s.getParamValue()));
            result.add(term);
        }
        // 合法性：必须来自字典、且启用、且未被判定违法（§3.4.4d / §3.4.4d0）
        AgreementInvariants.assertTermsSelectable(result, options);
        for (AgreementTerm term : result) {
            termMapper.insert(term);
        }
        log.debug("协议条款已写入: agreementId={}, versionId={}, 条数={}, 操作人={}",
                agreement.getId(), version.getId(), result.size(), operatorId);
        return result;
    }

    /** 不变量 6：同一对主体之间只能有一份生效协议。 */
    private void assertNoOtherActiveAgreement(Agreement agreement) {
        if (agreement.getPartyAId() == null || agreement.getPartyBId() == null) {
            // 消费者单方承诺的乙方是「不特定消费者」（partyBId 为空）：一个租户可以同时有多份
            // 不同口径的公开承诺，因此不纳入本约束。见类注释与 README 的说明。
            return;
        }
        Long count = agreementMapper.selectCount(new LambdaQueryWrapper<Agreement>()
                .ne(Agreement::getId, agreement.getId())
                .eq(Agreement::getPartyAId, agreement.getPartyAId())
                .eq(Agreement::getPartyBId, agreement.getPartyBId())
                .eq(Agreement::getStatus, AgreementStatus.ACTIVE.getCode()));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("同一对主体之间已经有一份生效协议，不能同时存在两份；"
                    + "请先终止或变更原协议");
        }
    }

    /** 主档状态比较（{@code status} 是可空 Integer，直接 == 会拆箱 NPE）。 */
    private static boolean isStatus(Agreement agreement, AgreementStatus expected) {
        return agreement.getStatus() != null && agreement.getStatus() == expected.getCode();
    }

    private void validateParties(AgreementType type, Long partyAId, Long partyATenantId,
                                 Long partyBId, Long partyBTenantId) {
        if (partyAId == null || partyATenantId == null) {
            throw BusinessException.badRequest("请选择甲方主体与其所属租户");
        }
        if (type.isConsumerFacing()) {
            // 消费者单方承诺：乙方为不特定消费者，不需要（也不允许）指定主体
            if (partyBId != null || partyBTenantId != null) {
                throw BusinessException.badRequest("消费者单方承诺的乙方是「不特定消费者」，不需要指定乙方主体");
            }
            return;
        }
        if (partyBId == null || partyBTenantId == null) {
            throw BusinessException.badRequest("请选择乙方主体与其所属租户");
        }
        if (partyATenantId.equals(partyBTenantId)) {
            // 两端必须属于不同租户：本模块处理的是「租户之间」与「租户与平台」的约定。
            // 同租户内的双方约定属于另一类载体（购销框架/合同，§12.6 边界表 → 阶段 D 纳入）。
            // 而且两端同租户时，"本方是谁"无从判定（会话租户同时匹配两端）⇒ 双签永远无法完成，
            // 协议会卡在草稿上。与其留个看起来像 bug 的死胡同，不如建档时就说不清。
            throw BusinessException.badRequest("协议的甲方与乙方必须属于**不同租户**：本模块处理的是租户之间"
                    + "与租户与平台之间的约定。同一租户内两个主体之间的约定请用购销框架/合同");
        }
        if (partyAId.equals(partyBId)) {
            throw BusinessException.badRequest("甲方与乙方不能是同一个主体");
        }
        if (type == AgreementType.PLATFORM_SERVICE && !Long.valueOf(1L).equals(partyATenantId)) {
            // 平台协议甲方 = 平台主体 + 系统租户 1（§十二 12.1 表格）
            throw BusinessException.badRequest("平台服务协议的甲方必须固定为平台方（系统租户 1）");
        }
    }

    private void checkPeriod(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw BusinessException.badRequest("生效开始日期不能晚于结束日期");
        }
    }

    /** 解析协议类型：文案与校验口径统一在 {@link AgreementType#parse(String)}（一处，不各写一遍）。 */
    private AgreementType requireType(String type) {
        return AgreementType.parse(type);
    }

    private AgreementStatus requireStatus(String status) {
        String s = trimToNull(status);
        if (s == null) {
            throw BusinessException.badRequest("协议状态不能为空");
        }
        for (AgreementStatus v : AgreementStatus.values()) {
            if (v.name().equalsIgnoreCase(s)) {
                return v;
            }
        }
        throw BusinessException.badRequest("协议状态「" + status + "」不支持；可选：DRAFT / ACTIVE / SUSPENDED / TERMINATED");
    }

    // ── 视图组装 ──

    private List<AgreementVO> toVoList(List<Agreement> agreements) {
        if (agreements == null || agreements.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> partyIds = new LinkedHashSet<>();
        Set<Long> tenantIds = new LinkedHashSet<>();
        Set<Long> versionIds = new LinkedHashSet<>();
        for (Agreement a : agreements) {
            addIfNotNull(partyIds, a.getPartyAId());
            addIfNotNull(partyIds, a.getPartyBId());
            addIfNotNull(tenantIds, a.getPartyATenantId());
            addIfNotNull(tenantIds, a.getPartyBTenantId());
            addIfNotNull(versionIds, a.getCurrentVersionId());
        }
        Map<Long, String> partyNames = names(partyIds, true);
        Map<Long, String> tenantNames = names(tenantIds, false);
        Map<Long, Integer> versionNos = new HashMap<>();
        if (!versionIds.isEmpty()) {
            for (AgreementVersion v : versionMapper.selectBatchIds(versionIds)) {
                versionNos.put(v.getId(), v.getVersionNo());
            }
        }
        List<AgreementVO> list = new ArrayList<>(agreements.size());
        for (Agreement a : agreements) {
            AgreementVO vo = new AgreementVO();
            vo.setId(a.getId());
            vo.setAgreementNo(a.getAgreementNo());
            vo.setAgreementType(a.getAgreementType());
            AgreementType type = safeType(a.getAgreementType());
            vo.setAgreementTypeLabel(type == null ? a.getAgreementType() : type.getLabel());
            vo.setTitle(a.getTitle());
            vo.setStatus(statusName(a.getStatus()));
            vo.setStatusLabel(AgreementStatus.labelOf(a.getStatus()));
            vo.setCurrentVersionId(a.getCurrentVersionId());
            vo.setCurrentVersionNo(versionNos.get(a.getCurrentVersionId()));
            vo.setPartyAId(a.getPartyAId());
            vo.setPartyATenantId(a.getPartyATenantId());
            vo.setPartyAName(partyNames.get(a.getPartyAId()));
            vo.setPartyATenantName(tenantNames.get(a.getPartyATenantId()));
            vo.setPartyBId(a.getPartyBId());
            vo.setPartyBTenantId(a.getPartyBTenantId());
            vo.setPartyBName(partyNames.get(a.getPartyBId()));
            vo.setPartyBTenantName(tenantNames.get(a.getPartyBTenantId()));
            vo.setEffectiveFrom(formatDay(a.getEffectiveFrom()));
            vo.setEffectiveTo(formatDay(a.getEffectiveTo()));
            vo.setTerminatedBy(a.getTerminatedBy());
            vo.setTerminatedAt(a.getTerminatedAt());
            vo.setTerminateReason(a.getTerminateReason());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            list.add(vo);
        }
        return list;
    }

    private Map<Long, String> names(Set<Long> ids, boolean party) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<NameRow> rows = party ? agreementMapper.selectPartyNames(ids) : agreementMapper.selectTenantNames(ids);
        return rows.stream().filter(r -> r.getId() != null)
                .collect(Collectors.toMap(NameRow::getId, r -> r.getName() == null ? "" : r.getName(), (x, y) -> x));
    }

    /** 版本 → VO：条款由**快照**解析（自包含），因此字典日后被改也不影响历史版本的展示与举证。 */
    private AgreementVersionVO toVersionVO(AgreementVersion v) {
        AgreementVersionVO vo = new AgreementVersionVO();
        vo.setId(v.getId());
        vo.setAgreementId(v.getAgreementId());
        vo.setVersionNo(v.getVersionNo());
        vo.setStatus(versionStatusName(v.getStatus()));
        vo.setStatusLabel(AgreementVersionStatus.labelOf(v.getStatus()));
        vo.setChangeReason(v.getChangeReason());
        // 协商时间线 + 变更单语义（§13.4 / §13.7）：版本列表就是"谁在哪一轮提了什么"的账本
        vo.setProposedBySide(v.getProposedBySide());
        AgreementPartySide side = safeSide(v.getProposedBySide());
        vo.setProposedBySideLabel(side == null ? null : switch (side) {
            case A -> "甲方";
            case B -> "乙方";
            case NONE -> "非缔约方";
        });
        vo.setProposedByPerson(v.getProposedByPerson());
        vo.setProposalNote(v.getProposalNote());
        vo.setOriginVersionId(v.getOriginVersionId());
        vo.setNoRetroactiveNote(v.getNoRetroactiveNote());
        vo.setPartyAConfirmedBy(v.getPartyAConfirmedBy());
        vo.setPartyAConfirmedAt(v.getPartyAConfirmedAt());
        vo.setPartyBConfirmedBy(v.getPartyBConfirmedBy());
        vo.setPartyBConfirmedAt(v.getPartyBConfirmedAt());
        vo.setSnapshotJson(v.getSnapshotJson());
        vo.setEffectiveFrom(formatDay(v.getEffectiveFrom()));
        vo.setEffectiveTo(formatDay(v.getEffectiveTo()));
        vo.setCreatedBy(v.getCreateBy());
        vo.setCreatedAt(v.getCreateTime());
        vo.setUpdateTime(v.getUpdateTime());
        List<AgreementTermVO> terms = new ArrayList<>();
        for (AgreementSnapshot.TermEntry e : AgreementSnapshot.readTerms(v.getSnapshotJson())) {
            AgreementTermVO t = new AgreementTermVO();
            t.setAgreementId(v.getAgreementId());
            t.setVersionId(v.getId());
            t.setTermCode(e.getTermCode());
            t.setTermName(e.getTermName());
            t.setOptionCode(e.getOptionCode());
            t.setOptionLabel(e.getOptionLabel());
            t.setSemantics(e.getSemantics());
            t.setNeedsParam(e.getNeedsParam());
            t.setParamValue(e.getParamValue());
            t.setRequired(e.getRequired());
            terms.add(t);
        }
        vo.setTerms(terms);
        return vo;
    }

    private List<String> termNames(List<AgreementTermOption> options, List<String> termCodes) {
        Map<String, String> map = new LinkedHashMap<>();
        for (AgreementTermOption o : options) {
            map.putIfAbsent(o.getTermCode(), o.getOptionLabel());
        }
        List<String> names = new ArrayList<>();
        for (String code : termCodes) {
            names.add(map.getOrDefault(code, code));
        }
        return names;
    }

    private static void addIfNotNull(Set<Long> set, Long v) {
        if (v != null) {
            set.add(v);
        }
    }

    private static String statusName(Integer code) {
        AgreementStatus s = AgreementStatus.of(code);
        return s == null ? null : s.name();
    }

    private static String versionStatusName(Integer code) {
        AgreementVersionStatus s = AgreementVersionStatus.of(code);
        return s == null ? null : s.name();
    }

    /** 提案方解析：库里出现枚举外的取值时返回 null（展示层不因此报错）。 */
    private static AgreementPartySide safeSide(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return AgreementPartySide.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static AgreementType safeType(String name) {
        if (name == null) {
            return null;
        }
        for (AgreementType t : AgreementType.values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    private static LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }

    private static String formatDay(LocalDateTime time) {
        return time == null ? null : time.toLocalDate().format(DAY);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
