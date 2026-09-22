package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.dto.AgreementVersionDiffVO;
import cn.aiedge.agreement.entity.AgreementFulfillmentMode;
import cn.aiedge.agreement.entity.AgreementNarrative;
import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.enums.AgreementNarrativeSection;
import cn.aiedge.agreement.enums.FulfillmentMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 版本差异计算 —— <b>纯函数，不依赖 Spring / 数据库</b>（§13.4「逐条 diff 可看」）。
 *
 * <p>输入是"两版各自的条款（从快照解析）+ 设定 + 文字 + 履约方式 + 字段中文名"，
 * 输出是 {@link AgreementVersionDiffVO}。因此单测可以直接喂两份内存数据，
 * 不需要起数据库 —— 差异算法本身能被逐条断言。</p>
 *
 * <h3>⚠️ 为什么 diff 读的是**快照**而不是活字典</h3>
 * 历史版本的条款必须按"当时约定了什么"来解释（快照自包含：连当时的选项名与语义都冗余存了），
 * 不能拿今天的字典去解释去年那一版 —— 字典改了，diff 结果就跟着变，举证就不可靠了。
 * 设定/文字/履约方式与版本同生共死、行本身不会被改（已生效版本只读），因此直接读行即可。
 */
public final class AgreementVersionDiff {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private AgreementVersionDiff() {
    }

    /** 参与对比的一侧（某一版的全部内容）。 */
    public static class Side {

        private final Long versionId;
        private final Integer versionNo;
        private final String snapshotJson;
        private final List<AgreementSetting> settings;
        private final List<AgreementNarrative> narratives;
        private final List<AgreementFulfillmentMode> modes;
        private final Map<String, String> settingLabels;

        public Side(Long versionId, Integer versionNo, String snapshotJson,
                    List<AgreementSetting> settings, List<AgreementNarrative> narratives,
                    List<AgreementFulfillmentMode> modes, Map<String, String> settingLabels) {
            this.versionId = versionId;
            this.versionNo = versionNo;
            this.snapshotJson = snapshotJson;
            this.settings = settings == null ? List.of() : settings;
            this.narratives = narratives == null ? List.of() : narratives;
            this.modes = modes == null ? List.of() : modes;
            this.settingLabels = settingLabels == null ? Map.of() : settingLabels;
        }
    }

    /**
     * 计算 {@code after} 相对 {@code before} 的差异（"本版 vs 上一版"）。
     *
     * @param beforeLabel 基准的人读说明（如「上一版（第 1 版）」）
     */
    public static AgreementVersionDiffVO compare(Long agreementId, String beforeLabel, Side before, Side after) {
        AgreementVersionDiffVO vo = new AgreementVersionDiffVO();
        vo.setAgreementId(agreementId);
        if (after != null) {
            vo.setVersionId(after.versionId);
            vo.setVersionNo(after.versionNo);
        }
        if (before != null) {
            vo.setAgainstVersionId(before.versionId);
            vo.setAgainstVersionNo(before.versionNo);
        }
        vo.setAgainstLabel(beforeLabel);
        vo.setSnapshotChanged(before == null || after == null
                ? Boolean.TRUE
                : !normalize(before.snapshotJson).equals(normalize(after.snapshotJson)));

        diffTerms(vo, before, after);
        diffSettings(vo, before, after);
        diffNarratives(vo, before, after);
        diffModes(vo, before, after);
        buildSummary(vo);
        return vo;
    }

    // ══════════════════════ 条款 ══════════════════════

    private static void diffTerms(AgreementVersionDiffVO vo, Side before, Side after) {
        Map<String, AgreementSnapshot.TermEntry> b = termMap(before);
        Map<String, AgreementSnapshot.TermEntry> a = termMap(after);
        for (String code : union(b.keySet(), a.keySet())) {
            AgreementSnapshot.TermEntry x = b.get(code);
            AgreementSnapshot.TermEntry y = a.get(code);
            if (x == null) {
                vo.getTerms().add(item(AgreementVersionDiffVO.ADDED, code, y.getTermName(),
                        null, null, y.getOptionCode(), termText(y)));
            } else if (y == null) {
                vo.getTerms().add(item(AgreementVersionDiffVO.REMOVED, code, x.getTermName(),
                        x.getOptionCode(), termText(x), null, null));
            } else if (!same(x.getOptionCode(), y.getOptionCode()) || !same(x.getParamValue(), y.getParamValue())) {
                vo.getTerms().add(item(AgreementVersionDiffVO.CHANGED, code,
                        y.getTermName() == null ? x.getTermName() : y.getTermName(),
                        x.getOptionCode(), termText(x), y.getOptionCode(), termText(y)));
            }
        }
    }

    private static Map<String, AgreementSnapshot.TermEntry> termMap(Side side) {
        Map<String, AgreementSnapshot.TermEntry> map = new LinkedHashMap<>();
        if (side == null) {
            return map;
        }
        for (AgreementSnapshot.TermEntry e : AgreementSnapshot.readTerms(side.snapshotJson)) {
            if (e.getTermCode() != null) {
                map.put(e.getTermCode(), e);
            }
        }
        return map;
    }

    /** 条款的展示文本：选项名（有参数则带上参数）。 */
    private static String termText(AgreementSnapshot.TermEntry e) {
        if (e == null) {
            return null;
        }
        String label = e.getOptionLabel() == null ? e.getOptionCode() : e.getOptionLabel();
        return hasText(e.getParamValue()) ? label + "（" + e.getParamValue() + "）" : label;
    }

    // ══════════════════════ 字段设定 ══════════════════════

    private static void diffSettings(AgreementVersionDiffVO vo, Side before, Side after) {
        Map<String, String> b = settingMap(before);
        Map<String, String> a = settingMap(after);
        for (String key : union(b.keySet(), a.keySet())) {
            String x = b.get(key);
            String y = a.get(key);
            String label = labelOf(after, before, key);
            if (x == null) {
                vo.getSettings().add(item(AgreementVersionDiffVO.ADDED, key, label, null, "未约定", null, y));
            } else if (y == null) {
                vo.getSettings().add(item(AgreementVersionDiffVO.REMOVED, key, label, null, x, null, "未约定"));
            } else if (!x.equals(y)) {
                vo.getSettings().add(item(AgreementVersionDiffVO.CHANGED, key, label, null, x, null, y));
            }
        }
    }

    /** 设定值的规范化文本："类型:人类可读值"；**未约定不是空字符串，而是没有任何一行**。 */
    private static Map<String, String> settingMap(Side side) {
        Map<String, String> map = new LinkedHashMap<>();
        if (side == null) {
            return map;
        }
        for (AgreementSetting row : side.settings) {
            if (row.getSettingKey() == null) {
                continue;
            }
            map.put(row.getSettingKey(), valueText(row));
        }
        return map;
    }

    private static String valueText(AgreementSetting row) {
        if (row == null) {
            return null;
        }
        if (Boolean.TRUE.equals(row.getValueBool())) {
            return "是";
        }
        if (Boolean.FALSE.equals(row.getValueBool())) {
            return "否";
        }
        if (row.getValueNumber() != null) {
            return strip(row.getValueNumber());
        }
        if (row.getValueDate() != null) {
            return format(row.getValueDate());
        }
        return row.getValueText() == null ? "" : row.getValueText();
    }

    private static String labelOf(Side after, Side before, String key) {
        String label = after == null ? null : after.settingLabels.get(key);
        if (label == null && before != null) {
            label = before.settingLabels.get(key);
        }
        return label == null ? key : label;
    }

    // ══════════════════════ 文字条款 ══════════════════════

    private static void diffNarratives(AgreementVersionDiffVO vo, Side before, Side after) {
        Map<String, AgreementNarrative> b = narrativeMap(before);
        Map<String, AgreementNarrative> a = narrativeMap(after);
        for (String code : union(b.keySet(), a.keySet())) {
            AgreementNarrative x = b.get(code);
            AgreementNarrative y = a.get(code);
            String label = narrativeLabel(y == null ? x : y, code);
            if (x == null) {
                vo.getNarratives().add(item(AgreementVersionDiffVO.ADDED, code, label, null, null, null, brief(y.getContentText())));
            } else if (y == null) {
                vo.getNarratives().add(item(AgreementVersionDiffVO.REMOVED, code, label, null, brief(x.getContentText()), null, null));
            } else if (!same(x.getContentHash(), y.getContentHash())) {
                // 文字条款**永不自动执行**，diff 只报"这段文字改过了"（末尾附一句中文说明），不解释法律含义
                vo.getNarratives().add(item(AgreementVersionDiffVO.CHANGED, code, label,
                        null, brief(x.getContentText()), null, brief(y.getContentText())));
            }
        }
    }

    private static Map<String, AgreementNarrative> narrativeMap(Side side) {
        Map<String, AgreementNarrative> map = new LinkedHashMap<>();
        if (side == null) {
            return map;
        }
        for (AgreementNarrative row : side.narratives) {
            if (row.getSectionCode() != null) {
                map.put(row.getSectionCode(), row);
            }
        }
        return map;
    }

    private static String narrativeLabel(AgreementNarrative row, String code) {
        if (row != null && hasText(row.getSectionTitle())) {
            return row.getSectionTitle();
        }
        return AgreementNarrativeSection.labelOf(code);
    }

    /** 正文摘要：diff 只给一眼能看懂的短文本（全文在版本详情里看）。 */
    private static String brief(String text) {
        if (text == null) {
            return null;
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= 60 ? t : t.substring(0, 60) + "…";
    }

    // ══════════════════════ 履约方式集合 ══════════════════════

    private static void diffModes(AgreementVersionDiffVO vo, Side before, Side after) {
        Map<String, String> b = modeMap(before);
        Map<String, String> a = modeMap(after);
        for (String code : union(b.keySet(), a.keySet())) {
            if (!b.containsKey(code)) {
                vo.getFulfillmentModes().add(item(AgreementVersionDiffVO.ADDED, code, a.get(code), null, null, code, a.get(code)));
            } else if (!a.containsKey(code)) {
                vo.getFulfillmentModes().add(item(AgreementVersionDiffVO.REMOVED, code, b.get(code), code, b.get(code), null, null));
            }
        }
    }

    private static Map<String, String> modeMap(Side side) {
        Map<String, String> map = new LinkedHashMap<>();
        if (side == null) {
            return map;
        }
        for (AgreementFulfillmentMode row : side.modes) {
            if (row.getMode() == null) {
                continue;
            }
            FulfillmentMode mode = FulfillmentMode.of(row.getMode());
            map.put(row.getMode(), mode == null ? row.getMode() : mode.getLabel());
        }
        return map;
    }

    // ══════════════════════ 装配 ══════════════════════

    private static void buildSummary(AgreementVersionDiffVO vo) {
        int terms = vo.getTerms().size();
        int settings = vo.getSettings().size();
        int narratives = vo.getNarratives().size();
        int modes = vo.getFulfillmentModes().size();
        if (terms + settings + narratives + modes == 0) {
            vo.getSummary().add("与" + safe(vo.getAgainstLabel()) + "相比，条款、设定、文字、履约方式都没有变化");
            return;
        }
        vo.getSummary().add("条款变化 " + terms + " 处、设定变化 " + settings + " 处、文字条款变化 "
                + narratives + " 处、履约方式变化 " + modes + " 处");
        if (terms > 0) {
            vo.getSummary().add("条款：" + changeText(vo.getTerms()));
        }
        if (settings > 0) {
            vo.getSummary().add("设定：" + changeText(vo.getSettings()));
        }
        if (modes > 0) {
            vo.getSummary().add("履约方式：" + changeText(vo.getFulfillmentModes()));
        }
        if (narratives > 0) {
            vo.getSummary().add("文字条款改动只做留痕，系统不会自动执行（争议解决、保密等由双方人工履行）");
        }
    }

    private static String changeText(List<AgreementVersionDiffVO.Item> items) {
        int added = 0;
        int removed = 0;
        int changed = 0;
        for (AgreementVersionDiffVO.Item i : items) {
            if (AgreementVersionDiffVO.ADDED.equals(i.getChangeType())) {
                added++;
            } else if (AgreementVersionDiffVO.REMOVED.equals(i.getChangeType())) {
                removed++;
            } else {
                changed++;
            }
        }
        List<String> parts = new ArrayList<>();
        if (added > 0) {
            parts.add("新增 " + added);
        }
        if (removed > 0) {
            parts.add("删除 " + removed);
        }
        if (changed > 0) {
            parts.add("修改 " + changed);
        }
        return String.join("、", parts);
    }

    private static AgreementVersionDiffVO.Item item(String changeType, String code, String label,
                                                    String beforeCode, String beforeText,
                                                    String afterCode, String afterText) {
        AgreementVersionDiffVO.Item item = new AgreementVersionDiffVO.Item();
        item.setChangeType(changeType);
        item.setChangeTypeLabel(AgreementVersionDiffVO.labelOf(changeType));
        item.setCode(code);
        item.setLabel(label == null || label.isBlank() ? code : label);
        item.setBeforeCode(beforeCode);
        item.setBeforeText(beforeText);
        item.setAfterCode(afterCode);
        item.setAfterText(afterText);
        return item;
    }

    /** 顺序稳定的并集（先基准里出现过的顺序，再新增的）—— diff 结果要可复现，不能用 HashSet 的顺序。 */
    private static Set<String> union(Set<String> before, Set<String> after) {
        Set<String> all = new LinkedHashSet<>(before);
        all.addAll(after);
        return all;
    }

    private static String strip(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TS);
    }

    private static String normalize(String s) {
        return s == null ? "" : s;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "基准版本" : s;
    }
}
