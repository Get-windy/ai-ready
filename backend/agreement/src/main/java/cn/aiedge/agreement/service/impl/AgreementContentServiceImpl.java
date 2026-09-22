package cn.aiedge.agreement.service.impl;

import cn.aiedge.agreement.domain.AgreementInvariants;
import cn.aiedge.agreement.domain.AgreementResolvedSettings;
import cn.aiedge.agreement.domain.AgreementRuntime;
import cn.aiedge.agreement.domain.AgreementSettingRules;
import cn.aiedge.agreement.domain.AgreementSettingValue;
import cn.aiedge.agreement.domain.AgreementSnapshot;
import cn.aiedge.agreement.domain.AgreementVisibility;
import cn.aiedge.agreement.dto.AgreementContentSaveDTO;
import cn.aiedge.agreement.dto.AgreementContentSaveResultVO;
import cn.aiedge.agreement.dto.AgreementContentVO;
import cn.aiedge.agreement.dto.AgreementEffectiveSettingsVO;
import cn.aiedge.agreement.dto.AgreementFulfillmentModeVO;
import cn.aiedge.agreement.dto.AgreementNarrativeVO;
import cn.aiedge.agreement.dto.AgreementSettingDefVO;
import cn.aiedge.agreement.dto.AgreementSettingItemVO;
import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementNarrativeSection;
import cn.aiedge.agreement.enums.AgreementSettingValueType;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.agreement.enums.FulfillmentMode;
import cn.aiedge.agreement.mapper.AgreementFulfillmentModeMapper;
import cn.aiedge.agreement.mapper.AgreementMapper;
import cn.aiedge.agreement.mapper.AgreementNarrativeMapper;
import cn.aiedge.agreement.mapper.AgreementSettingMapper;
import cn.aiedge.agreement.mapper.AgreementVersionMapper;
import cn.aiedge.agreement.service.AgreementContentService;
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
import java.util.Set;

/**
 * 协议内容层实现。
 *
 * <h3>实现要点（每一处都对应一条硬要求）</h3>
 * <ol>
 *   <li><b>系统级归属位显式写 0</b>：这些表与协议主档同为系统级（裁定⑥）。
 *       不显式写，MyBatis-Plus 的填充处理器会塞入**会话租户**，另一端租户立刻读不到内容。</li>
 *   <li><b>读取三态由 {@code AgreementRuntime} 装配</b>：本类不自己拼"已约定/未约定"的判定，
 *       避免同一个语义出现第二份实现（第二份迟早与第一份不一致）。</li>
 *   <li><b>文字条款的哈希随内容重算</b>：内容变了哈希就变，旧确认自然对不上，
 *       这是"确认之后被改过"能被发现的依据。</li>
 *   <li><b>内容变化即清空双签痕迹</b>：双签针对的是内容，内容变了原确认失效。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementContentServiceImpl implements AgreementContentService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AgreementMapper agreementMapper;
    private final AgreementVersionMapper versionMapper;
    private final AgreementSettingMapper settingMapper;
    private final AgreementNarrativeMapper narrativeMapper;
    private final AgreementFulfillmentModeMapper fulfillmentModeMapper;
    private final AgreementRuntime runtime;

    // ══════════════════════ 读 ══════════════════════

    @Override
    public List<AgreementSettingDefVO> settingDefs() {
        List<AgreementSettingDefVO> list = new ArrayList<>();
        for (AgreementSettingDef def : runtime.definitions()) {
            list.add(toDefVO(def));
        }
        return list;
    }

    @Override
    public AgreementContentVO content(Long versionId, Long sessionTenantId) {
        AgreementVersion version = requireVisibleVersion(versionId, sessionTenantId);
        Agreement agreement = agreementMapper.selectById(version.getAgreementId());

        List<AgreementSettingDef> defs = runtime.definitions();
        Map<String, AgreementSettingValue> values = runtime.valuesOf(version.getId());

        AgreementContentVO vo = new AgreementContentVO();
        vo.setAgreementId(version.getAgreementId());
        vo.setVersionId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionStatus(versionStatusName(version.getStatus()));
        vo.setVersionStatusLabel(AgreementVersionStatus.labelOf(version.getStatus()));
        boolean editable = AgreementInvariants.isDraft(version.getStatus());
        vo.setEditable(editable);
        vo.setEditableHint(editable ? null
                : "本版已生效或已归档，内容不可修改；要改内容请「发起变更」生成新版本，由双方重新确认");

        Map<String, AgreementSetting> rows = rowsOf(version.getId());
        List<AgreementSettingItemVO> items = new ArrayList<>(values.size());
        List<String> missingKeys = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();
        List<String> agreedKeys = new ArrayList<>();
        for (AgreementSettingDef def : defs) {
            AgreementSettingValue v = values.get(def.getSettingKey());
            if (v == null) {
                continue;
            }
            items.add(toItemVO(v, rows.get(def.getSettingKey()), def));
            if (v.isAgreed()) {
                agreedKeys.add(def.getSettingKey());
            } else if (Boolean.TRUE.equals(def.getRequired())) {
                missingKeys.add(def.getSettingKey());
                missingNames.add(def.getLabel() == null ? def.getSettingKey() : def.getLabel());
            }
        }
        vo.setSettings(items);
        vo.setAgreedSettingKeys(agreedKeys);
        vo.setMissingRequiredSettings(missingKeys);
        vo.setMissingRequiredSettingNames(missingNames);

        vo.setNarratives(toNarrativeVOList(listNarratives(version.getId())));
        vo.setFulfillmentModes(toModeVOList(listModes(version.getId())));
        List<AgreementSettingDefVO> defVos = new ArrayList<>(defs.size());
        for (AgreementSettingDef def : defs) {
            defVos.add(toDefVO(def));
        }
        vo.setSettingDefs(defVos);

        AgreementSnapshot.TemplateSource source =
                AgreementSnapshot.readTemplateSource(version.getSnapshotJson());
        vo.setTemplateSourceText(source == null ? null : source.getText());
        if (source != null && agreement != null) {
            log.debug("协议版本含模板来源留痕: agreementId={}, versionId={}, 来源={}",
                    agreement.getId(), version.getId(), source.getText());
        }
        return vo;
    }

    @Override
    public AgreementEffectiveSettingsVO effective(Long agreementId, LocalDateTime businessTime, Long sessionTenantId) {
        Agreement agreement = requireVisibleAgreement(agreementId, sessionTenantId);
        // 执行口径与查看口径的区别见 AgreementRuntime#resolveByAgreement 的注释：
        // 这里传了业务时点，因此是"那一刻按哪一版"的严格口径。
        AgreementResolvedSettings resolved = runtime.resolveByAgreement(agreement.getId(), businessTime);

        AgreementEffectiveSettingsVO vo = new AgreementEffectiveSettingsVO();
        vo.setVersionFound(resolved.isVersionFound());
        vo.setAbsenceReason(resolved.getAbsenceReason());
        vo.setAgreementId(resolved.getAgreementId());
        vo.setVersionId(resolved.getVersionId());
        vo.setVersionNo(resolved.getVersionNo());
        vo.setBusinessTime(format(resolved.getBusinessTime()));
        vo.setEffectiveFrom(format(resolved.getEffectiveFrom()));
        vo.setEffectiveTo(format(resolved.getEffectiveTo()));

        List<AgreementSettingItemVO> items = new ArrayList<>();
        List<AgreementSettingDef> defs = resolved.getDefinitions();
        Map<String, AgreementSetting> rows = resolved.isVersionFound()
                ? rowsOf(resolved.getVersionId()) : Map.of();
        for (AgreementSettingDef def : defs) {
            AgreementSettingValue v = resolved.getValues().get(def.getSettingKey());
            if (v == null) {
                continue;
            }
            items.add(toItemVO(v, rows.get(def.getSettingKey()), def));
        }
        vo.setSettings(items);

        List<AgreementFulfillmentModeVO> modeVos = new ArrayList<>();
        for (FulfillmentMode mode : resolved.getFulfillmentModes()) {
            AgreementFulfillmentModeVO m = new AgreementFulfillmentModeVO();
            m.setMode(mode.name());
            m.setModeLabel(mode.getLabel());
            modeVos.add(m);
        }
        vo.setFulfillmentModes(modeVos);
        return vo;
    }

    // ══════════════════════ 写 ══════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgreementContentSaveResultVO saveContent(Long versionId, AgreementContentSaveDTO dto,
                                                    Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVisibleVersion(versionId, sessionTenantId);
        // 不变量 1：已生效版本只读（改内容只能走"发起变更"→新版本→双方重新确认，㉛）
        AgreementInvariants.assertVersionMutable(version);

        String before = canonicalContent(version.getId());
        // 履约方式集合：既可用专门的 fulfillmentModes 数组提交，也可用设定项 FULFILLMENT_MODES
        // （形如 "DROP_SHIP,TRANSIT_STOCK"）提交 —— 两者取其一，设定项优先。
        // 这是**集合语义**，值永远落在 agreement_fulfillment_mode（一版多行并存），
        // agreement_setting 里不存行，避免出现"两个地方各存一份、以哪个为准"的问题。
        List<String> modesFromSettings = modesFromSettingItems(dto.getSettings());
        List<String> modesInput = modesFromSettings != null ? modesFromSettings : dto.getFulfillmentModes();
        List<String> savedModes = writeModes(version, modesInput);
        int savedSettings = writeSettings(version, dto.getSettings());
        int savedNarratives = writeNarratives(version, dto.getNarratives());
        String after = canonicalContent(version.getId());

        if (!before.equals(after)) {
            // 内容变了 ⇒ 双方原确认失效。复用 rewriteDraftSnapshot 的**结构性防线**
            // （SQL 里带 status = 0，已生效版本写不进去，受影响行数会是 0 而不是静默改掉）。
            AgreementVersion patch = new AgreementVersion();
            patch.setId(version.getId());
            patch.setSnapshotJson(version.getSnapshotJson());
            patch.setEffectiveFrom(version.getEffectiveFrom());
            patch.setEffectiveTo(version.getEffectiveTo());
            patch.setUpdateBy(operatorId);
            int rows = versionMapper.rewriteDraftSnapshot(patch);
            if (rows == 0) {
                throw BusinessException.badRequest("该版本已不是草稿状态，内容未能保存；请刷新后重新发起变更");
            }
            log.info("协议内容已更新（原双方确认痕迹已清空，需重新确认）: versionId={}, 设定项={}, 文字段落={}, 履约方式={}",
                    version.getId(), savedSettings, savedNarratives, savedModes);
        } else {
            log.info("协议内容保存：内容无变化，未改动确认痕迹. versionId={}", version.getId());
        }

        AgreementContentSaveResultVO result = new AgreementContentSaveResultVO();
        result.setAgreementId(version.getAgreementId());
        result.setVersionId(version.getId());
        result.setSavedSettingCount(savedSettings);
        result.setSavedNarrativeCount(savedNarratives);
        result.setFulfillmentModes(savedModes);
        List<AgreementSettingDef> defs = runtime.definitions();
        List<String> missingKeys = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();
        Map<String, AgreementSettingValue> values = runtime.valuesOf(version.getId());
        for (AgreementSettingDef def : defs) {
            if (!Boolean.TRUE.equals(def.getRequired())) {
                continue;
            }
            AgreementSettingValue v = values.get(def.getSettingKey());
            if (v == null || !v.isAgreed()) {
                missingKeys.add(def.getSettingKey());
                missingNames.add(def.getLabel() == null ? def.getSettingKey() : def.getLabel());
            }
        }
        result.setMissingRequiredSettings(missingKeys);
        result.setMissingRequiredSettingNames(missingNames);
        result.setNarrativeNotice(AgreementNarrativeSection.MANUAL_NOTICE);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTemplateSource(Long versionId, AgreementSnapshot.TemplateSource source,
                                   Long sessionTenantId, Long operatorId) {
        AgreementVersion version = requireVisibleVersion(versionId, sessionTenantId);
        AgreementInvariants.assertVersionMutable(version);
        if (source == null) {
            return;
        }
        // 在**现有快照**上补一段来源留痕（不重建正文，避免把内容层的字段顺序/取值改动）：
        // 用 Jackson 重新序列化，字段顺序由原有的 JSON 决定，因此哈希的变化只来自新增的这一段。
        String updated = AgreementSnapshot.withTemplateSource(version.getSnapshotJson(), source);
        AgreementVersion patch = new AgreementVersion();
        patch.setId(version.getId());
        patch.setSnapshotJson(updated);
        patch.setEffectiveFrom(version.getEffectiveFrom());
        patch.setEffectiveTo(version.getEffectiveTo());
        patch.setUpdateBy(operatorId);
        int rows = versionMapper.rewriteDraftSnapshot(patch);
        if (rows == 0) {
            throw BusinessException.badRequest("该版本已不是草稿状态，模板来源未能写入；请刷新后重新发起");
        }
        log.info("协议版本已记录模板来源（司法可追溯）: versionId={}, 来源={}", versionId, source.getText());
    }

    // ══════════════════════ 写入子步骤 ══════════════════════

    /** 写履约方式集合：整份覆盖（一版多行并存，故先软删再插）。 */
    private List<String> writeModes(AgreementVersion version, List<String> modes) {
        fulfillmentModeMapper.delete(new LambdaQueryWrapper<AgreementFulfillmentMode>()
                .eq(AgreementFulfillmentMode::getVersionId, version.getId()));
        List<String> labels = new ArrayList<>();
        if (modes == null || modes.isEmpty()) {
            return labels;
        }
        Set<FulfillmentMode> parsed = new LinkedHashSet<>();
        for (String raw : modes) {
            parsed.add(FulfillmentMode.parse(raw));
        }
        int sort = 0;
        for (FulfillmentMode mode : parsed) {
            AgreementFulfillmentMode row = new AgreementFulfillmentMode();
            row.setTenantId(0L);
            row.setAgreementId(version.getAgreementId());
            row.setVersionId(version.getId());
            row.setMode(mode.name());
            row.setSort(sort++);
            fulfillmentModeMapper.insert(row);
            labels.add(mode.getLabel());
        }
        return labels;
    }

    /** 写字段设定版：整份覆盖；值留空 = 撤回该项约定（回到"未约定"）。 */
    private int writeSettings(AgreementVersion version, List<AgreementContentSaveDTO.SettingItem> items) {
        settingMapper.delete(new LambdaQueryWrapper<AgreementSetting>()
                .eq(AgreementSetting::getVersionId, version.getId()));
        if (items == null || items.isEmpty()) {
            return 0;
        }
        Map<String, AgreementSettingDef> defs = defsByKey();
        Set<String> seen = new LinkedHashSet<>();
        int saved = 0;
        for (AgreementContentSaveDTO.SettingItem item : items) {
            String key = trimToNull(item.getSettingKey());
            if (key == null) {
                continue;
            }
            if (AgreementRuntime.FULFILLMENT_MODES_KEY.equals(key)) {
                // 集合语义：值由 writeModes 落到 agreement_fulfillment_mode，这里不写行（见 saveContent 注释）
                continue;
            }
            if (!seen.add(key)) {
                throw BusinessException.badRequest("字段「" + key + "」重复提交了多个取值，请只提交一次");
            }
            AgreementSettingDef def = defs.get(key);
            AgreementSettingRules.assertDefUsable(def, key);
            AgreementSettingRules.TypedValue typed = AgreementSettingRules.parse(def, item.getValue());
            if (typed.isEmpty()) {
                // 值为空 = 用户撤回了这一项约定。**不写行**，于是它保持"未约定"，
                // 下游取值时会明确报"未约定"，而不是拿到一个被兜底过的值（㉜）。
                continue;
            }
            AgreementSetting row = new AgreementSetting();
            row.setTenantId(0L);
            row.setAgreementId(version.getAgreementId());
            row.setVersionId(version.getId());
            row.setSettingKey(key);
            row.setValueType(typed.valueType());
            row.setValueText(typed.text());
            row.setValueNumber(typed.number());
            row.setValueBool(typed.bool());
            row.setValueDate(typed.date());
            row.setRemark(trimToNull(item.getRemark()));
            settingMapper.insert(row);
            saved++;
        }
        return saved;
    }

    /** 写文字条款：整份覆盖；正文留空 = 不写这一段。 */
    private int writeNarratives(AgreementVersion version, List<AgreementContentSaveDTO.NarrativeItem> items) {
        narrativeMapper.delete(new LambdaQueryWrapper<AgreementNarrative>()
                .eq(AgreementNarrative::getVersionId, version.getId()));
        if (items == null || items.isEmpty()) {
            return 0;
        }
        Set<String> seen = new LinkedHashSet<>();
        int saved = 0;
        int sort = 0;
        for (AgreementContentSaveDTO.NarrativeItem item : items) {
            AgreementNarrativeSection section = AgreementNarrativeSection.parse(item.getSectionCode());
            if (!seen.add(section.name())) {
                throw BusinessException.badRequest("文字条款「" + section.getLabel() + "」重复提交了多段内容，请合并为一段");
            }
            String text = trimToNull(item.getContentText());
            if (text == null) {
                continue;
            }
            AgreementNarrative row = new AgreementNarrative();
            row.setTenantId(0L);
            row.setAgreementId(version.getAgreementId());
            row.setVersionId(version.getId());
            row.setSectionCode(section.name());
            row.setSectionTitle(section.getLabel());
            row.setContentText(text);
            // 哈希随正文重算：正文改了，旧确认对不上，这是"确认之后被改过"能被发现的依据
            row.setContentHash(AgreementSnapshot.sha256(text));
            row.setSort(sort++);
            narrativeMapper.insert(row);
            saved++;
        }
        return saved;
    }

    /**
     * 从设定项里取"履约方式集合"的提交值（逗号分隔）。
     *
     * @return {@code null} = 本次没有用设定项提交它（改用专门的 fulfillmentModes 数组）；
     *         空列表 = 显式清空（撤回这套履约方式约定）
     */
    private List<String> modesFromSettingItems(List<AgreementContentSaveDTO.SettingItem> items) {
        if (items == null) {
            return null;
        }
        for (AgreementContentSaveDTO.SettingItem item : items) {
            if (!AgreementRuntime.FULFILLMENT_MODES_KEY.equals(trimToNull(item.getSettingKey()))) {
                continue;
            }
            String value = trimToNull(item.getValue());
            if (value == null) {
                // 显式留空 = 撤回履约方式约定（回到"未约定"，而不是"随便用哪种"）
                return List.of();
            }
            List<String> modes = new ArrayList<>();
            for (String raw : value.split(",")) {
                String one = trimToNull(raw);
                if (one != null) {
                    modes.add(FulfillmentMode.parse(one).name());
                }
            }
            return modes;
        }
        return null;
    }

    /** 内容指纹：用来判断"这次保存到底改了没有"（没改就不清空确认痕迹，避免白让用户重签）。 */
    private String canonicalContent(Long versionId) {
        StringBuilder sb = new StringBuilder();
        for (AgreementSetting row : settingMapper.selectList(new LambdaQueryWrapper<AgreementSetting>()
                .eq(AgreementSetting::getVersionId, versionId)
                .orderByAsc(AgreementSetting::getSettingKey))) {
            sb.append("S|").append(row.getSettingKey()).append('=').append(row.getValueType()).append(':')
                    .append(row.getValueText()).append(':').append(row.getValueNumber()).append(':')
                    .append(row.getValueBool()).append(':').append(row.getValueDate())
                    .append(':').append(row.getRemark()).append('\n');
        }
        for (AgreementNarrative row : listNarratives(versionId)) {
            sb.append("N|").append(row.getSectionCode()).append('=').append(row.getContentHash()).append('\n');
        }
        for (AgreementFulfillmentMode row : listModes(versionId)) {
            sb.append("M|").append(row.getMode()).append('\n');
        }
        return sb.toString();
    }

    // ══════════════════════ 内部：取数与组装 ══════════════════════

    /** 取版本并做可见性判定（可见性唯一构造在 {@code AgreementVisibility}，裁定⑥）。 */
    private AgreementVersion requireVisibleVersion(Long versionId, Long sessionTenantId) {
        AgreementVersion version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        requireVisibleAgreement(version.getAgreementId(), sessionTenantId);
        return version;
    }

    private Agreement requireVisibleAgreement(Long agreementId, Long sessionTenantId) {
        if (agreementId == null) {
            throw BusinessException.notFound("协议不存在或你不是本协议的任一缔约方");
        }
        Agreement agreement = agreementMapper.selectById(agreementId);
        // 看不到就当"不存在"（404）：协议存在性本身也是敏感信息，见 AgreementVisibility
        AgreementVisibility.assertVisible(agreement, sessionTenantId);
        return agreement;
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

    private Map<String, AgreementSetting> rowsOf(Long versionId) {
        Map<String, AgreementSetting> map = new LinkedHashMap<>();
        for (AgreementSetting row : settingMapper.selectList(new LambdaQueryWrapper<AgreementSetting>()
                .eq(AgreementSetting::getVersionId, versionId))) {
            if (row.getSettingKey() != null) {
                map.put(row.getSettingKey(), row);
            }
        }
        return map;
    }

    private List<AgreementNarrative> listNarratives(Long versionId) {
        return narrativeMapper.selectList(new LambdaQueryWrapper<AgreementNarrative>()
                .eq(AgreementNarrative::getVersionId, versionId)
                .orderByAsc(AgreementNarrative::getSort)
                .orderByAsc(AgreementNarrative::getSectionCode));
    }

    private List<AgreementFulfillmentMode> listModes(Long versionId) {
        return fulfillmentModeMapper.selectList(new LambdaQueryWrapper<AgreementFulfillmentMode>()
                .eq(AgreementFulfillmentMode::getVersionId, versionId)
                .orderByAsc(AgreementFulfillmentMode::getSort));
    }

    // ── VO 组装 ──

    private AgreementSettingDefVO toDefVO(AgreementSettingDef def) {
        AgreementSettingDefVO vo = new AgreementSettingDefVO();
        vo.setId(def.getId());
        vo.setSettingKey(def.getSettingKey());
        vo.setLabel(def.getLabel());
        vo.setValueType(def.getValueType());
        AgreementSettingValueType type = AgreementSettingRules.parseType(def.getValueType());
        vo.setValueTypeLabel(type == null ? def.getValueType() : type.getLabel());
        vo.setOptions(AgreementSettingRules.options(def));
        vo.setRequired(def.getRequired());
        vo.setConsumerPoint(def.getConsumerPoint());
        vo.setConsumerPointLabel(AgreementRuntime.ConsumerPoint.labelOf(def.getConsumerPoint()));
        AgreementRuntime.ConsumerPoint cp = AgreementRuntime.ConsumerPoint.of(def.getConsumerPoint());
        vo.setConsumerSemantics(cp == null ? null : cp.getSemantics());
        vo.setSemantics(def.getSemantics());
        vo.setSort(def.getSort());
        vo.setStatus(def.getStatus());
        return vo;
    }

    private AgreementSettingItemVO toItemVO(AgreementSettingValue value, AgreementSetting row,
                                            AgreementSettingDef def) {
        AgreementSettingItemVO vo = new AgreementSettingItemVO();
        vo.setSettingKey(value.getSettingKey());
        vo.setLabel(value.getLabel());
        vo.setState(value.getState().name());
        vo.setStateLabel(value.getState().getLabel());
        vo.setValueType(value.getValueType() == null ? null : value.getValueType().name());
        vo.setValueTypeLabel(value.getValueType() == null ? null : value.getValueType().getLabel());
        vo.setValue(rawValueText(value));
        vo.setDisplayValue(value.displayValue());
        vo.setRequired(def == null ? null : def.getRequired());
        vo.setConsumerPoint(value.getConsumerPoint());
        vo.setConsumerPointLabel(AgreementRuntime.ConsumerPoint.labelOf(value.getConsumerPoint()));
        vo.setSemantics(value.getSemantics());
        vo.setRemark(row == null ? null : row.getRemark());
        vo.setOptions(value.getEnumOptions());
        return vo;
    }

    /** 表单回填用的字符串（DURATION / NUMBER 去掉多余尾零；DATE 用 ISO 本地格式）。 */
    private String rawValueText(AgreementSettingValue value) {
        if (!value.isAgreed() || value.getValueType() == null) {
            return null;
        }
        return switch (value.getValueType()) {
            case DURATION, NUMBER -> value.rawValue() == null ? null
                    : ((java.math.BigDecimal) value.rawValue()).stripTrailingZeros().toPlainString();
            case BOOL -> String.valueOf(value.rawValue());
            case DATE -> {
                LocalDateTime d = (LocalDateTime) value.rawValue();
                yield d == null ? null : d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            case TEXT, ENUM -> (String) value.rawValue();
        };
    }

    private List<AgreementNarrativeVO> toNarrativeVOList(List<AgreementNarrative> rows) {
        List<AgreementNarrativeVO> list = new ArrayList<>(rows.size());
        for (AgreementNarrative row : rows) {
            AgreementNarrativeVO vo = new AgreementNarrativeVO();
            vo.setId(row.getId());
            vo.setSectionCode(row.getSectionCode());
            vo.setSectionTitle(row.getSectionTitle() == null
                    ? AgreementNarrativeSection.labelOf(row.getSectionCode()) : row.getSectionTitle());
            vo.setContentText(row.getContentText());
            vo.setContentHash(row.getContentHash());
            // ⚠️ 永远下发 false + 一句中文说明：文字条款系统不会自动执行（§13.2 硬要求）
            vo.setAutoExecutable(AgreementNarrativeSection.AUTO_EXECUTABLE);
            vo.setManualNotice(AgreementNarrativeSection.MANUAL_NOTICE);
            vo.setPartyAConfirmedBy(row.getPartyAConfirmedBy());
            vo.setPartyAConfirmedAt(row.getPartyAConfirmedAt());
            vo.setPartyBConfirmedBy(row.getPartyBConfirmedBy());
            vo.setPartyBConfirmedAt(row.getPartyBConfirmedAt());
            vo.setSort(row.getSort());
            list.add(vo);
        }
        return list;
    }

    private List<AgreementFulfillmentModeVO> toModeVOList(List<AgreementFulfillmentMode> rows) {
        List<AgreementFulfillmentModeVO> list = new ArrayList<>(rows.size());
        for (AgreementFulfillmentMode row : rows) {
            FulfillmentMode mode = FulfillmentMode.of(row.getMode());
            if (mode == null) {
                log.warn("协议履约方式出现未知取值，已跳过展示: versionId={}, mode={}", row.getVersionId(), row.getMode());
                continue;
            }
            AgreementFulfillmentModeVO vo = new AgreementFulfillmentModeVO();
            vo.setMode(mode.name());
            vo.setModeLabel(mode.getLabel());
            vo.setScopeNote(row.getScopeNote());
            vo.setSort(row.getSort());
            list.add(vo);
        }
        return list;
    }

    private static String versionStatusName(Integer code) {
        AgreementVersionStatus s = AgreementVersionStatus.of(code);
        return s == null ? null : s.name();
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TS);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
