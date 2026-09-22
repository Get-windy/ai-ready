package cn.aiedge.agreement.service.impl;

import cn.aiedge.agreement.domain.AgreementInvariants;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSettingRules;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementTemplateVisibility;
import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementCreateDTO;
import cn.aiedge.agreement.dto.AgreementFromTemplateDTO;
import cn.aiedge.agreement.dto.AgreementNarrativeVO;
import cn.aiedge.agreement.dto.AgreementSettingItemVO;
import cn.aiedge.agreement.dto.AgreementTermVO;
import cn.aiedge.agreement.dto.AgreementTemplateDTO;
import cn.aiedge.agreement.dto.AgreementTemplateDetailVO;
import cn.aiedge.agreement.dto.AgreementTemplateQuery;
import cn.aiedge.agreement.dto.AgreementTemplateVO;
import cn.aiedge.agreement.dto.TermSelectDTO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementTemplate;
import cn.aiedge.agreement.entity.AgreementTemplateNarrative;
import cn.aiedge.agreement.entity.AgreementTemplateSetting;
import cn.aiedge.agreement.entity.AgreementTemplateTerm;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementNarrativeSection;
import cn.aiedge.agreement.enums.AgreementSettingValueType;
import cn.aiedge.agreement.enums.AgreementTemplateScope;
import cn.aiedge.agreement.enums.AgreementType;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.enums.FulfillmentMode;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementTermMapper;
import cn.aiedge.agreement.mapper.AgreementTermOptionMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateSettingMapper;
import cn.aiedge.agreement.mapper.AgreementTemplateTermMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementContentService;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.agreement.service.AgreementTemplateService;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 契约模板服务实现。
 *
 * <h3>为什么"从模板发起"要走一遍完整的建协议流程，而不是直接拼一个协议出来</h3>
 * 编号生成、两端校验（必须不同租户、平台协议甲方固定为系统租户）、首个草稿版本的建立、
 * 快照生成，这些口径都在 {@code AgreementService#create} 里。模板发起若自己拼一套，
 * 就会出现"从模板建的协议与手工建的协议规则不同"——这正是"两套实现打架"的起点。
 *
 * <h3>⚠️ 模板预填 ≠ 已约定</h3>
 * 预填只写在**新草稿版本**上（条款 / 设定 / 文字三段），必须由双方在那一版上确认、双签才生效。
 * 同时把**模板来源**写进该版本快照（"基于模板 X，第 N 版"）—— 司法可追溯（§13.9）。
 * {@code AgreementRuntime} 不持有任何模板 Mapper，结构上不可能读到模板（㉜ 的防线）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementTemplateServiceImpl implements AgreementTemplateService {

    /** 法务审核状态的合法取值（与条款字典同一口径）。 */
    private static final Set<String> LEGAL_STATUS = new LinkedHashSet<>(
            List.of("PENDING", "APPROVED", "REJECTED"));

    private final AgreementTemplateMapper templateMapper;
    private final AgreementTemplateTermMapper templateTermMapper;
    private final AgreementTemplateSettingMapper templateSettingMapper;
    private final AgreementTemplateNarrativeMapper templateNarrativeMapper;
    private final AgreementTermMapper termMapper;
    private final AgreementTermOptionMapper termOptionMapper;
    private final AgreementMapper agreementMapper;
    private final AgreementVersionMapper versionMapper;
    private final AgreementService agreementService;
    private final AgreementContentService contentService;
    private final AgreementRuntime runtime;

    // ══════════════════════ 查询 ══════════════════════

    @Override
    public Page<AgreementTemplateVO> page(AgreementTemplateQuery query, Long sessionTenantId, boolean platformSide) {
        long current = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long size = query.getSize() == null || query.getSize() < 1 ? 20L : Math.min(query.getSize(), 200L);

        LambdaQueryWrapper<AgreementTemplate> wrapper = new LambdaQueryWrapper<>();
        // 可见性：唯一构造处。平台可读全部（合规抽查读）；租户只读「平台模板 + 自己的」
        AgreementTemplateVisibility.apply(wrapper, sessionTenantId, platformSide);
        if (trimToNull(query.getScope()) != null) {
            wrapper.eq(AgreementTemplate::getScope, AgreementTemplateScope.parse(query.getScope()).name());
        }
        if (trimToNull(query.getAgreementType()) != null) {
            wrapper.eq(AgreementTemplate::getAgreementType, AgreementType.parse(query.getAgreementType()).name());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AgreementTemplate::getStatus, query.getStatus());
        }
        String keyword = trimToNull(query.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(AgreementTemplate::getTemplateName, keyword)
                    .or().like(AgreementTemplate::getDescription, keyword));
        }
        wrapper.orderByDesc(AgreementTemplate::getCreateTime).orderByDesc(AgreementTemplate::getId);

        Page<AgreementTemplate> page = templateMapper.selectPage(new Page<>(current, size), wrapper);
        List<AgreementTemplate> rows = page.getRecords();
        Map<Long, int[]> counts = childCounts(rows);
        List<AgreementTemplateVO> vos = new ArrayList<>(rows.size());
        for (AgreementTemplate t : rows) {
            vos.add(toVo(t, counts.get(t.getId()), sessionTenantId, platformSide));
        }
        Page<AgreementTemplateVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public AgreementTemplateDetailVO detail(Long id, Long sessionTenantId, boolean platformSide) {
        AgreementTemplate template = requireVisible(id, sessionTenantId, platformSide);
        AgreementTemplateDetailVO vo = new AgreementTemplateDetailVO();
        copyBase(template, vo, childCounts(List.of(template)).get(template.getId()), sessionTenantId, platformSide);
        vo.setSettings(toSettingItems(template.getId()));
        vo.setTerms(toTermVos(template.getId()));
        vo.setNarratives(toNarrativeVos(template.getId()));
        return vo;
    }

    // ══════════════════════ 写 ══════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgreementTemplateDTO dto, Long sessionTenantId, boolean platformSide, Long operatorId) {
        AgreementTemplateScope scope = AgreementTemplateScope.parse(dto.getScope());
        long ownerTenantId = AgreementTemplateVisibility.resolveOwnerTenantId(scope, sessionTenantId, platformSide);
        String name = requireText(dto.getTemplateName(), "请填写模板名称");
        AgreementType type = AgreementType.parse(dto.getAgreementType());
        assertNameUnique(scope, ownerTenantId, name, null);

        AgreementTemplate template = new AgreementTemplate();
        template.setTenantId(ownerTenantId);
        template.setScope(scope.name());
        template.setTemplateName(name);
        template.setAgreementType(type.name());
        template.setDescription(trimToNull(dto.getDescription()));
        template.setLegalReviewStatus(normalizeLegalStatus(dto.getLegalReviewStatus()));
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        template.setCreateBy(operatorId);
        template.setUpdateBy(operatorId);
        templateMapper.insert(template);

        writeChildren(template, dto, operatorId);
        log.info("契约模板已新建: id={}, 级别={}, 归属租户={}, 名称={}, 操作人={}",
                template.getId(), scope.name(), ownerTenantId, name, operatorId);
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AgreementTemplateDTO dto, Long sessionTenantId, boolean platformSide, Long operatorId) {
        AgreementTemplate exists = requireVisible(id, sessionTenantId, platformSide);
        // 写口径与读口径**不对称**：平台能读租户模板（合规抽查）但不能改（见 AgreementTemplateVisibility）
        AgreementTemplateVisibility.assertManageable(exists, sessionTenantId, platformSide);

        AgreementTemplateScope scope = AgreementTemplateScope.of(exists.getScope());
        if (trimToNull(dto.getScope()) != null) {
            AgreementTemplateScope newScope = AgreementTemplateScope.parse(dto.getScope());
            if (newScope != scope) {
                // 改级别等于换归属（平台模板归属位是 0、租户模板是本租户），会让"谁的模板"说不清
                throw BusinessException.badRequest("模板级别不可修改；如需另一级别的模板，请新建");
            }
        }
        AgreementTemplate patch = new AgreementTemplate();
        patch.setId(id);
        patch.setUpdateBy(operatorId);
        if (trimToNull(dto.getTemplateName()) != null) {
            String name = dto.getTemplateName().trim();
            assertNameUnique(scope, exists.getTenantId(), name, id);
            patch.setTemplateName(name);
        }
        if (trimToNull(dto.getAgreementType()) != null) {
            patch.setAgreementType(AgreementType.parse(dto.getAgreementType()).name());
        }
        if (dto.getDescription() != null) {
            patch.setDescription(trimToNull(dto.getDescription()));
        }
        if (trimToNull(dto.getLegalReviewStatus()) != null) {
            patch.setLegalReviewStatus(normalizeLegalStatus(dto.getLegalReviewStatus()));
        }
        if (dto.getStatus() != null) {
            patch.setStatus(dto.getStatus());
        }
        templateMapper.updateById(patch);

        // 三份清单：不传 = 不动；传了（含空数组）= 整份覆盖
        AgreementTemplate templateView = templateMapper.selectById(id);
        writeChildren(templateView, dto, operatorId);
        log.info("契约模板已更新: id={}, 名称={}（改动只影响以后发起，已发起的协议不受影响）", id, templateView.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long sessionTenantId, boolean platformSide) {
        AgreementTemplate exists = requireVisible(id, sessionTenantId, platformSide);
        AgreementTemplateVisibility.assertManageable(exists, sessionTenantId, platformSide);
        templateTermMapper.delete(new LambdaQueryWrapper<AgreementTemplateTerm>()
                .eq(AgreementTemplateTerm::getTemplateId, id));
        templateSettingMapper.delete(new LambdaQueryWrapper<AgreementTemplateSetting>()
                .eq(AgreementTemplateSetting::getTemplateId, id));
        templateNarrativeMapper.delete(new LambdaQueryWrapper<AgreementTemplateNarrative>()
                .eq(AgreementTemplateNarrative::getTemplateId, id));
        templateMapper.deleteById(id);
        log.info("契约模板已删除（软删）: id={}, 名称={}；已基于它发起的协议不受影响", id, exists.getTemplateName());
    }

    // ══════════════════════ 从模板发起 ══════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long templateId, AgreementFromTemplateDTO dto, Long sessionTenantId, boolean platformSide,
                      Long operatorId) {
        AgreementTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("模板不存在或不属于你所在的租户");
        }
        // 能读到它（平台模板 / 自己的租户模板）就能用它 —— 读不到时按"不存在"回话
        AgreementTemplateVisibility.assertVisible(template, sessionTenantId, platformSide);
        if (template.getStatus() != null && template.getStatus() != 1) {
            throw BusinessException.badRequest("模板「" + template.getTemplateName() + "」已停用，不能用于发起新契约");
        }
        if (LEGAL_STATUS.contains("REJECTED") && "REJECTED".equalsIgnoreCase(template.getLegalReviewStatus())) {
            throw BusinessException.badRequest("模板「" + template.getTemplateName()
                    + "」未通过法务审核（已被判定含不合法条款），不能用于发起契约");
        }

        // ① 建主档 + 首个草稿版本（编号、两端校验、快照一律复用既有流程）
        AgreementCreateDTO createDTO = new AgreementCreateDTO();
        createDTO.setAgreementType(trimToNull(dto.getAgreementType()) == null
                ? template.getAgreementType() : dto.getAgreementType());
        createDTO.setPartyAId(dto.getPartyAId());
        createDTO.setPartyATenantId(dto.getPartyATenantId());
        createDTO.setPartyBId(dto.getPartyBId());
        createDTO.setPartyBTenantId(dto.getPartyBTenantId());
        createDTO.setTitle(dto.getTitle());
        createDTO.setEffectiveFrom(dto.getEffectiveFrom());
        createDTO.setEffectiveTo(dto.getEffectiveTo());
        Long agreementId = agreementService.create(createDTO, sessionTenantId, operatorId);

        // ② 找到刚建立的草稿版本（create 保证每个协议必有一个草稿版本）与主档实体（快照要它的编号/两端）
        AgreementVersion draft = requireDraftVersion(agreementId);
        Agreement agreement = agreementMapper.selectById(agreementId);
        if (agreement == null) {
            throw BusinessException.badRequest("新建的协议读取失败，模板内容无法预填");
        }

        // ③ 预填条款（来自模板），并校验条款仍在字典里、启用、且未被判定违法
        List<AgreementTermOption> options = termOptionMapper.selectList(new LambdaQueryWrapper<AgreementTermOption>());
        List<AgreementTerm> terms = writeTemplateTerms(agreementId, draft.getId(), template.getId(), options, operatorId);

        // ④ 预填设定与文字，并写履约方式集合（复用内容层：校验口径只有一处）
        AgreementContentSaveDTO content = new AgreementContentSaveDTO();
        content.setSettings(templateSettingDtos(template.getId()));
        content.setNarratives(templateNarrativeDtos(template.getId()));
        content.setFulfillmentModes(templateFulfillmentModes(template));
        contentService.saveContent(draft.getId(), content, sessionTenantId, operatorId);

        // ⑤ 重写快照：带上**模板来源**（"基于模板 X，第 N 版"）—— 司法可追溯（§13.9）
        int versionNo = draft.getVersionNo() == null ? 1 : draft.getVersionNo();
        AgreementTemplateScope scope = AgreementTemplateScope.of(template.getScope());
        AgreementSnapshot.TemplateSource source = AgreementSnapshot.templateSource(
                template.getId(), template.getTemplateName(), template.getScope(),
                scope == null ? null : scope.getLabel(), versionNo);
        String snapshot = AgreementSnapshot.build(agreement, versionNo,
                terms, options, draft.getEffectiveFrom(), draft.getEffectiveTo(), source);
        AgreementVersion patch = new AgreementVersion();
        patch.setId(draft.getId());
        patch.setSnapshotJson(snapshot);
        patch.setEffectiveFrom(draft.getEffectiveFrom());
        patch.setEffectiveTo(draft.getEffectiveTo());
        patch.setUpdateBy(operatorId);
        int rows = versionMapper.rewriteDraftSnapshot(patch);
        if (rows == 0) {
            throw BusinessException.badRequest("草稿版本状态已变化，模板内容未能写入；请重新发起");
        }

        log.info("已从模板发起契约: agreementId={}, templateId={}, 模板={}, 来源已记入第 {} 版快照；"
                        + "⚠️ 预填不等于已约定，仍需双方在本次这一版上确认签署",
                agreementId, template.getId(), template.getTemplateName(), versionNo);
        return agreementId;
    }

    // ── 内部：模板子表写入 ──

    private void writeChildren(AgreementTemplate template, AgreementTemplateDTO dto, Long operatorId) {
        if (dto.getSettings() != null) {
            templateSettingMapper.delete(new LambdaQueryWrapper<AgreementTemplateSetting>()
                    .eq(AgreementTemplateSetting::getTemplateId, template.getId()));
            Map<String, AgreementSettingDef> defs = defsByKey();
            int sort = 0;
            Set<String> seen = new LinkedHashSet<>();
            for (AgreementContentSaveDTO.SettingItem item : dto.getSettings()) {
                String key = trimToNull(item.getSettingKey());
                if (key == null) {
                    continue;
                }
                if (!seen.add(key)) {
                    throw BusinessException.badRequest("模板里的字段「" + key + "」重复提交了多个取值");
                }
                AgreementSettingDef def = defs.get(key);
                AgreementSettingRules.assertDefUsable(def, key);
                // 「履约方式集合」是集合语义（逗号分隔的多种方式），不能按单值枚举解析
                AgreementSettingRules.TypedValue typed = AgreementRuntime.FULFILLMENT_MODES_KEY.equals(key)
                        ? parseFulfillmentModesValue(item.getValue())
                        : AgreementSettingRules.parse(def, item.getValue());
                if (typed.isEmpty()) {
                    continue;
                }
                AgreementTemplateSetting row = new AgreementTemplateSetting();
                row.setTenantId(template.getTenantId());
                row.setTemplateId(template.getId());
                row.setSettingKey(key);
                row.setValueType(typed.valueType());
                row.setValueText(typed.text());
                row.setValueNumber(typed.number());
                row.setValueBool(typed.bool());
                row.setValueDate(typed.date());
                row.setSort(sort++);
                templateSettingMapper.insert(row);
            }
        }
        if (dto.getTerms() != null) {
            templateTermMapper.delete(new LambdaQueryWrapper<AgreementTemplateTerm>()
                    .eq(AgreementTemplateTerm::getTemplateId, template.getId()));
            List<AgreementTermOption> options = termOptionMapper.selectList(new LambdaQueryWrapper<AgreementTermOption>());
            List<AgreementTerm> probe = new ArrayList<>();
            int sort = 0;
            Set<String> seen = new LinkedHashSet<>();
            for (TermSelectDTO select : dto.getTerms()) {
                String termCode = trimToNull(select.getTermCode());
                String optionCode = trimToNull(select.getOptionCode());
                if (termCode == null || optionCode == null) {
                    throw BusinessException.badRequest("模板里的条款「" + termCode + "」没有选择具体选项；"
                            + "未约定的条款请不要提交该行");
                }
                if (!seen.add(termCode)) {
                    throw BusinessException.badRequest("模板里的条款「" + termCode + "」重复提交了多个选项");
                }
                AgreementTerm probeTerm = new AgreementTerm();
                probeTerm.setTermCode(termCode);
                probeTerm.setOptionCode(optionCode);
                probeTerm.setParamValue(trimToNull(select.getParamValue()));
                probe.add(probeTerm);
            }
            // 合法性统一走协议条款的那一套校验（字典存在 + 启用 + 未被判定违法）
            AgreementInvariants.assertTermsSelectable(probe, options);
            for (AgreementTerm probeTerm : probe) {
                AgreementTemplateTerm row = new AgreementTemplateTerm();
                row.setTenantId(template.getTenantId());
                row.setTemplateId(template.getId());
                row.setTermCode(probeTerm.getTermCode());
                row.setOptionCode(probeTerm.getOptionCode());
                row.setParamValue(probeTerm.getParamValue());
                row.setSort(sort++);
                templateTermMapper.insert(row);
            }
        }
        if (dto.getNarratives() != null) {
            templateNarrativeMapper.delete(new LambdaQueryWrapper<AgreementTemplateNarrative>()
                    .eq(AgreementTemplateNarrative::getTemplateId, template.getId()));
            int sort = 0;
            Set<String> seen = new LinkedHashSet<>();
            for (AgreementContentSaveDTO.NarrativeItem item : dto.getNarratives()) {
                AgreementNarrativeSection section = AgreementNarrativeSection.parse(item.getSectionCode());
                if (!seen.add(section.name())) {
                    throw BusinessException.badRequest("模板里的文字条款「" + section.getLabel() + "」重复提交了多段内容");
                }
                String text = trimToNull(item.getContentText());
                if (text == null) {
                    continue;
                }
                AgreementTemplateNarrative row = new AgreementTemplateNarrative();
                row.setTenantId(template.getTenantId());
                row.setTemplateId(template.getId());
                row.setSectionCode(section.name());
                row.setSectionTitle(section.getLabel());
                row.setContentText(text);
                row.setSort(sort++);
                templateNarrativeMapper.insert(row);
            }
        }
    }

    /** 把模板里的条款预填到草稿版本上（**不是**"已约定"，仍需双方确认）。 */
    private List<AgreementTerm> writeTemplateTerms(Long agreementId, Long versionId, Long templateId,
                                                   List<AgreementTermOption> options, Long operatorId) {
        List<AgreementTerm> terms = new ArrayList<>();
        for (AgreementTemplateTerm row : templateTermMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateTerm>()
                        .eq(AgreementTemplateTerm::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateTerm::getSort))) {
            AgreementTerm term = new AgreementTerm();
            term.setAgreementId(agreementId);
            term.setVersionId(versionId);
            term.setTermCode(row.getTermCode());
            term.setOptionCode(row.getOptionCode());
            term.setParamValue(row.getParamValue());
            terms.add(term);
        }
        if (terms.isEmpty()) {
            return terms;
        }
        AgreementInvariants.assertTermsSelectable(terms, options);
        for (AgreementTerm term : terms) {
            termMapper.insert(term);
        }
        return terms;
    }

    private List<AgreementContentSaveDTO.SettingItem> templateSettingDtos(Long templateId) {
        List<AgreementContentSaveDTO.SettingItem> list = new ArrayList<>();
        for (AgreementTemplateSetting row : templateSettingMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateSetting>()
                        .eq(AgreementTemplateSetting::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateSetting::getSort))) {
            AgreementContentSaveDTO.SettingItem item = new AgreementContentSaveDTO.SettingItem();
            item.setSettingKey(row.getSettingKey());
            item.setValue(asInputText(row.getValueType(), row.getValueText(), row.getValueNumber(),
                    row.getValueBool(), row.getValueDate()));
            list.add(item);
        }
        return list;
    }

    private List<AgreementContentSaveDTO.NarrativeItem> templateNarrativeDtos(Long templateId) {
        List<AgreementContentSaveDTO.NarrativeItem> list = new ArrayList<>();
        for (AgreementTemplateNarrative row : templateNarrativeMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateNarrative>()
                        .eq(AgreementTemplateNarrative::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateNarrative::getSort))) {
            AgreementContentSaveDTO.NarrativeItem item = new AgreementContentSaveDTO.NarrativeItem();
            item.setSectionCode(row.getSectionCode());
            item.setContentText(row.getContentText());
            list.add(item);
        }
        return list;
    }

    /**
     * 模板里的履约方式集合。
     *
     * <p>模板表没有单独的履约方式子表（§13.11 未列），因此履约方式作为**设定项
     * {@code FULFILLMENT_MODES}** 存在模板设定里 —— 与协议侧"集合落在专用表"不同，
     * 这里只做预填，最终以协议自身的 {@code agreement_fulfillment_mode} 为准。</p>
     */
    private List<String> templateFulfillmentModes(AgreementTemplate template) {
        for (AgreementTemplateSetting row : templateSettingMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateSetting>()
                        .eq(AgreementTemplateSetting::getTemplateId, template.getId()))) {
            if (!"FULFILLMENT_MODES".equals(row.getSettingKey())) {
                continue;
            }
            List<String> modes = new ArrayList<>();
            if (row.getValueText() == null || row.getValueText().isBlank()) {
                return modes;
            }
            for (String raw : row.getValueText().split(",")) {
                FulfillmentMode mode = FulfillmentMode.of(raw);
                if (mode != null) {
                    modes.add(mode.name());
                }
            }
            return modes;
        }
        return List.of();
    }

    // ── 内部：读 ──

    private AgreementTemplate requireVisible(Long id, Long sessionTenantId, boolean platformSide) {
        AgreementTemplate template = id == null ? null : templateMapper.selectById(id);
        AgreementTemplateVisibility.assertVisible(template, sessionTenantId, platformSide);
        return template;
    }

    private AgreementVersion requireDraftVersion(Long agreementId) {
        List<AgreementVersion> drafts = versionMapper.selectList(new LambdaQueryWrapper<AgreementVersion>()
                .eq(AgreementVersion::getAgreementId, agreementId)
                .eq(AgreementVersion::getStatus, AgreementVersionStatus.DRAFT.getCode()));
        if (drafts.isEmpty()) {
            throw BusinessException.badRequest("新建的协议没有可编辑的草稿版本，模板内容无法预填");
        }
        // 先复制再排序：部分实现返回不可变列表，直接 sort 会抛 UnsupportedOperationException
        List<AgreementVersion> sorted = new ArrayList<>(drafts);
        sorted.sort((a, b) -> Integer.compare(
                b.getVersionNo() == null ? 0 : b.getVersionNo(),
                a.getVersionNo() == null ? 0 : a.getVersionNo()));
        return sorted.get(0);
    }

    // ── 内部：校验与小工具 ──

    private void assertNameUnique(AgreementTemplateScope scope, long tenantId, String name, Long excludeId) {
        LambdaQueryWrapper<AgreementTemplate> wrapper = new LambdaQueryWrapper<AgreementTemplate>()
                .eq(AgreementTemplate::getScope, scope.name())
                .eq(AgreementTemplate::getTenantId, tenantId)
                .eq(AgreementTemplate::getTemplateName, name);
        if (excludeId != null) {
            wrapper.ne(AgreementTemplate::getId, excludeId);
        }
        Long count = templateMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.badRequest("同一级别下已存在名为「" + name + "」的模板，请换一个名称");
        }
    }

    private String normalizeLegalStatus(String status) {
        String s = trimToNull(status);
        if (s == null) {
            // 表单没填时默认「待审」而不是「已通过」—— 不能凭空替法务点头
            return "PENDING";
        }
        String upper = s.toUpperCase();
        if (!LEGAL_STATUS.contains(upper)) {
            throw BusinessException.badRequest("法务审核状态「" + status + "」不支持；可选：PENDING / APPROVED / REJECTED");
        }
        return upper;
    }

    private Map<String, AgreementSettingDef> defsByKey() {
        Map<String, AgreementSettingDef> map = new LinkedHashMap<>();
        for (AgreementSettingDef def : runtime.definitions()) {
            if (def.getSettingKey() != null) {
                map.putIfAbsent(def.getSettingKey(), def);
            }
        }
        return map;
    }

    private Map<Long, int[]> childCounts(List<AgreementTemplate> templates) {
        Map<Long, int[]> counts = new LinkedHashMap<>();
        if (templates == null || templates.isEmpty()) {
            return counts;
        }
        List<Long> ids = new ArrayList<>();
        for (AgreementTemplate t : templates) {
            counts.put(t.getId(), new int[]{0, 0, 0});
            ids.add(t.getId());
        }
        for (AgreementTemplateSetting row : templateSettingMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateSetting>().in(AgreementTemplateSetting::getTemplateId, ids))) {
            int[] c = counts.get(row.getTemplateId());
            if (c != null) {
                c[0]++;
            }
        }
        for (AgreementTemplateTerm row : templateTermMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateTerm>().in(AgreementTemplateTerm::getTemplateId, ids))) {
            int[] c = counts.get(row.getTemplateId());
            if (c != null) {
                c[1]++;
            }
        }
        for (AgreementTemplateNarrative row : templateNarrativeMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateNarrative>().in(AgreementTemplateNarrative::getTemplateId, ids))) {
            int[] c = counts.get(row.getTemplateId());
            if (c != null) {
                c[2]++;
            }
        }
        return counts;
    }

    private AgreementTemplateVO toVo(AgreementTemplate t, int[] counts, Long sessionTenantId, boolean platformSide) {
        AgreementTemplateVO vo = new AgreementTemplateVO();
        copyBase(t, vo, counts, sessionTenantId, platformSide);
        return vo;
    }

    private void copyBase(AgreementTemplate t, AgreementTemplateVO vo, int[] counts,
                          Long sessionTenantId, boolean platformSide) {
        vo.setId(t.getId());
        vo.setScope(t.getScope());
        AgreementTemplateScope scope = AgreementTemplateScope.of(t.getScope());
        vo.setScopeLabel(scope == null ? t.getScope() : scope.getLabel());
        vo.setTenantId(t.getTenantId());
        vo.setTemplateName(t.getTemplateName());
        vo.setAgreementType(t.getAgreementType());
        AgreementType type = safeType(t.getAgreementType());
        vo.setAgreementTypeLabel(type == null ? t.getAgreementType() : type.getLabel());
        vo.setDescription(t.getDescription());
        vo.setLegalReviewStatus(t.getLegalReviewStatus());
        vo.setLegalReviewStatusLabel(legalLabel(t.getLegalReviewStatus()));
        vo.setStatus(t.getStatus());
        vo.setStatusLabel(t.getStatus() != null && t.getStatus() == 1 ? "启用" : "停用");
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        if (counts != null) {
            vo.setSettingCount(counts[0]);
            vo.setTermCount(counts[1]);
            vo.setNarrativeCount(counts[2]);
        }
        // 能不能改由服务端算好（读权限与写权限不对称，前端不该自己猜）
        try {
            AgreementTemplateVisibility.assertManageable(t, sessionTenantId, platformSide);
            vo.setManageable(true);
        } catch (BusinessException e) {
            vo.setManageable(false);
            vo.setManageableHint(e.getMessage());
        }
    }

    private List<AgreementSettingItemVO> toSettingItems(Long templateId) {
        Map<String, AgreementSettingDef> defs = defsByKey();
        List<AgreementSettingItemVO> list = new ArrayList<>();
        for (AgreementTemplateSetting row : templateSettingMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateSetting>()
                        .eq(AgreementTemplateSetting::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateSetting::getSort))) {
            AgreementSettingDef def = defs.get(row.getSettingKey());
            AgreementSettingItemVO vo = new AgreementSettingItemVO();
            vo.setSettingKey(row.getSettingKey());
            vo.setLabel(def == null ? row.getSettingKey() : def.getLabel());
            vo.setValueType(row.getValueType());
            AgreementSettingValueType type = AgreementSettingRules.parseType(row.getValueType());
            vo.setValueTypeLabel(type == null ? row.getValueType() : type.getLabel());
            // ⚠️ 模板项**不是**"已约定"：状态与文案都不下"已约定"，避免界面误导用户
            vo.setStateLabel("模板预填（发起后仍需双方在协议里确认）");
            vo.setValue(asInputText(row.getValueType(), row.getValueText(), row.getValueNumber(),
                    row.getValueBool(), row.getValueDate()));
            vo.setDisplayValue(display(row));
            vo.setRequired(def == null ? null : def.getRequired());
            vo.setConsumerPoint(def == null ? null : def.getConsumerPoint());
            vo.setConsumerPointLabel(def == null ? null
                    : cn.aiedge.agreement.domain.AgreementRuntime.ConsumerPoint.labelOf(def.getConsumerPoint()));
            vo.setSemantics(def == null ? null : def.getSemantics());
            vo.setOptions(def == null ? List.of() : AgreementSettingRules.options(def));
            list.add(vo);
        }
        return list;
    }

    private List<AgreementTermVO> toTermVos(Long templateId) {
        List<AgreementTermOption> options = termOptionMapper.selectList(new LambdaQueryWrapper<AgreementTermOption>());
        List<AgreementTermVO> list = new ArrayList<>();
        for (AgreementTemplateTerm row : templateTermMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateTerm>()
                        .eq(AgreementTemplateTerm::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateTerm::getSort))) {
            AgreementTermOption option = findOption(options, row.getTermCode(), row.getOptionCode());
            AgreementTermVO vo = new AgreementTermVO();
            vo.setTermCode(row.getTermCode());
            vo.setTermName(option == null ? row.getTermCode() : option.getOptionLabel());
            vo.setOptionCode(row.getOptionCode());
            vo.setOptionLabel(option == null ? row.getOptionCode() : option.getOptionLabel());
            vo.setSemantics(option == null ? null : option.getSemantics());
            vo.setNeedsParam(option == null ? null : option.getNeedsParam());
            vo.setParamValue(row.getParamValue());
            vo.setRequired(option == null ? null : option.getRequired());
            list.add(vo);
        }
        return list;
    }

    private List<AgreementNarrativeVO> toNarrativeVos(Long templateId) {
        List<AgreementNarrativeVO> list = new ArrayList<>();
        for (AgreementTemplateNarrative row : templateNarrativeMapper.selectList(
                new LambdaQueryWrapper<AgreementTemplateNarrative>()
                        .eq(AgreementTemplateNarrative::getTemplateId, templateId)
                        .orderByAsc(AgreementTemplateNarrative::getSort))) {
            AgreementNarrativeVO vo = new AgreementNarrativeVO();
            vo.setSectionCode(row.getSectionCode());
            vo.setSectionTitle(row.getSectionTitle() == null
                    ? AgreementNarrativeSection.labelOf(row.getSectionCode()) : row.getSectionTitle());
            vo.setContentText(row.getContentText());
            vo.setContentHash(AgreementSnapshot.sha256(row.getContentText()));
            // 文字条款永不自动执行：模板里也一样要显式告知（§13.2）
            vo.setAutoExecutable(AgreementNarrativeSection.AUTO_EXECUTABLE);
            vo.setManualNotice(AgreementNarrativeSection.MANUAL_NOTICE);
            vo.setSort(row.getSort());
            list.add(vo);
        }
        return list;
    }

    /** 履约方式集合的模板值：逗号分隔，逐个校验合法后归一成编码串（Text 类型存）。 */
    private static AgreementSettingRules.TypedValue parseFulfillmentModesValue(String raw) {
        String v = trimToNull(raw);
        if (v == null) {
            return new AgreementSettingRules.TypedValue(AgreementSettingValueType.TEXT.name(),
                    null, null, null, null);
        }
        List<String> codes = new ArrayList<>();
        for (String one : v.split(",")) {
            String mode = trimToNull(one);
            if (mode != null) {
                codes.add(FulfillmentMode.parse(mode).name());
            }
        }
        if (codes.isEmpty()) {
            return new AgreementSettingRules.TypedValue(AgreementSettingValueType.TEXT.name(),
                    null, null, null, null);
        }
        return new AgreementSettingRules.TypedValue(AgreementSettingValueType.TEXT.name(),
                String.join(",", codes), null, null, null);
    }

    private static AgreementTermOption findOption(List<AgreementTermOption> options, String termCode, String optionCode) {
        for (AgreementTermOption o : options) {
            if (o.getTermCode() != null && o.getTermCode().equals(termCode)
                    && o.getOptionCode() != null && o.getOptionCode().equals(optionCode)) {
                return o;
            }
        }
        return null;
    }

    private static String display(AgreementTemplateSetting row) {
        AgreementSettingValueType type = AgreementSettingRules.parseType(row.getValueType());
        if (type == null) {
            return "";
        }
        return switch (type) {
            case TEXT, ENUM -> row.getValueText() == null ? "" : row.getValueText();
            case BOOL -> Boolean.TRUE.equals(row.getValueBool()) ? "是" : "否";
            case NUMBER -> row.getValueNumber() == null ? "" : row.getValueNumber().stripTrailingZeros().toPlainString();
            case DURATION -> row.getValueNumber() == null ? ""
                    : row.getValueNumber().stripTrailingZeros().toPlainString() + " 天";
            case DATE -> row.getValueDate() == null ? "" : row.getValueDate().toLocalDate().toString();
        };
    }

    private static String asInputText(String valueType, String text, java.math.BigDecimal number,
                                      Boolean bool, java.time.LocalDateTime date) {
        AgreementSettingValueType type = AgreementSettingRules.parseType(valueType);
        if (type == null) {
            return text;
        }
        return switch (type) {
            case TEXT, ENUM -> text;
            case BOOL -> bool == null ? null : String.valueOf(bool);
            case NUMBER, DURATION -> number == null ? null : number.stripTrailingZeros().toPlainString();
            case DATE -> date == null ? null
                    : date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        };
    }

    /** 安全解析协议类型：脏数据不该让整个列表页 500（返回 null 由调用方显示原值）。 */
    private static AgreementType safeType(String name) {
        if (name == null) {
            return null;
        }
        for (AgreementType t : AgreementType.values()) {
            if (t.name().equalsIgnoreCase(name.trim())) {
                return t;
            }
        }
        return null;
    }

    private static String legalLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toUpperCase()) {
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "判定违法";
            default -> "待审";
        };
    }

    private static String requireText(String value, String message) {
        String v = trimToNull(value);
        if (v == null) {
            throw BusinessException.badRequest(message);
        }
        return v;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
