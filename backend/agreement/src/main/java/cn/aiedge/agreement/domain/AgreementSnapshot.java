package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 协议版本快照（{@code agreement_version.snapshot_json}）的构造、解析与内容哈希。
 *
 * <p><b>为什么快照必须自包含</b>：司法举证要的是"当时双方同意的那一条是什么"。
 * 若快照只存 option_code，而字典后来被改了语义，就再也说不清当时约定了什么。
 * 因此快照里除选项编码外，还冗余存下**当时的选项名与语义说明**（只追加、不修改）。</p>
 *
 * <p><b>哈希口径</b>：{@code sign_hash = SHA-256(快照正文 JSON 字符串)}。
 * 双方各存一份自己的哈希（甲方/乙方哈希列分开），二者不同也无妨 ——
 * 它证明的是"我确认的是这个内容"，而不是"我们确认的是同一份文本"；
 * 后者由"同一个版本行只有一个 snapshot_json"这一结构保证。</p>
 */
public final class AgreementSnapshot {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private AgreementSnapshot() {
    }

    /**
     * 快照里的**模板来源**（"基于模板 X 起的草稿"，DOMAIN-MODEL §13.9）。
     *
     * <p>为什么要记进快照而不是只记在某个角落：司法可追溯要求能回答
     * 「这份协议的起点是哪份模板」。模板是**显式选择的起点、不是默认值** ——
     * 记下来源正是为了证明"双方是选了它、并在这一版上确认过"，
     * 而不是"系统悄悄套了一份模板"。</p>
     *
     * <p>⚠️ 记来源**不等于**模板内容成了约定：约定只以双方签署的那一版为准，
     * 取数入口 {@code AgreementRuntime} 也绝不读模板。</p>
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateSource {

        private Long templateId;

        private String templateName;

        /** PLATFORM 平台模板 / TENANT 租户模板 */
        private String scope;

        /** 人类可读的一句话，如「基于模板「标准代销模板」起草（平台模板），第 1 版」 */
        private String text;
    }

    /** 快照里的一条条款（自包含：编码 + 当时的名称 + 当时的语义说明 + 参数）。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermEntry {
        private String termCode;
        private String termName;
        private String optionCode;
        private String optionLabel;
        private String semantics;
        private String needsParam;
        private String paramValue;
        private Boolean required;
    }

    /**
     * 由主档 + 条款选择 + 字典，生成完整快照 JSON。
     *
     * @param terms    该版本的条款选择（未删除）
     * @param options  这些条款对应的字典选项（按 termCode + optionCode 索引），用于冗余存名称与语义
     */
    public static String build(Agreement agreement,
                               int versionNo,
                               List<AgreementTerm> terms,
                               List<AgreementTermOption> options,
                               LocalDateTime effectiveFrom,
                               LocalDateTime effectiveTo) {
        return build(agreement, versionNo, terms, options, effectiveFrom, effectiveTo, null);
    }

    /**
     * 同上，另把**模板来源**写进快照（"基于模板 X，第 N 版"）—— 司法可追溯（§13.9）。
     *
     * @param templateSource 从模板发起时传入；从零起草传 {@code null}（此时输出与不带来源的老口径一致）
     */
    public static String build(Agreement agreement,
                               int versionNo,
                               List<AgreementTerm> terms,
                               List<AgreementTermOption> options,
                               LocalDateTime effectiveFrom,
                               LocalDateTime effectiveTo,
                               TemplateSource templateSource) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("agreementId", agreement.getId());
        root.put("agreementNo", agreement.getAgreementNo());
        root.put("agreementType", agreement.getAgreementType());
        root.put("title", agreement.getTitle());
        root.put("versionNo", versionNo);
        root.put("partyAId", agreement.getPartyAId());
        root.put("partyATenantId", agreement.getPartyATenantId());
        root.put("partyBId", agreement.getPartyBId());
        root.put("partyBTenantId", agreement.getPartyBTenantId());
        root.put("effectiveFrom", effectiveFrom == null ? null : effectiveFrom.toLocalDate().format(DAY));
        root.put("effectiveTo", effectiveTo == null ? null : effectiveTo.toLocalDate().format(DAY));
        // ⚠️ 故意**不放** generatedAt / 版本行 id 这类随时间变化的字段：
        //   快照必须是"同输入同结果"的确定函数，否则 sign_hash 每次重算都不一样，
        //   "双方确认的是不是当前这份内容"就无法核对（内容被偷改也发现不了）。
        //   生成时间看 agreement_version.create_time，那才是权威记录。

        // 模板来源（"基于模板 X 起的草稿"）：只在从模板发起时写入。
        // ⚠️ 它是**来源留痕**，不是"模板内容算约定"——约定只以双方签署的这一版为准（§13.9）。
        if (templateSource != null) {
            ObjectNode t = root.putObject("templateSource");
            t.put("templateId", templateSource.getTemplateId());
            t.put("templateName", templateSource.getTemplateName());
            t.put("scope", templateSource.getScope());
            t.put("text", templateSource.getText());
        }

        ArrayNode arr = root.putArray("terms");
        for (AgreementTerm t : terms) {
            AgreementTermOption opt = findOption(options, t.getTermCode(), t.getOptionCode());
            ObjectNode n = arr.addObject();
            n.put("termCode", t.getTermCode());
            n.put("termName", opt == null ? null : opt.getOptionLabel());
            n.put("optionCode", t.getOptionCode());
            // 冗余存下"当时的"名称与语义：字典日后被改，也不影响这一版的举证
            n.put("optionLabel", opt == null ? null : opt.getOptionLabel());
            n.put("semantics", opt == null ? null : opt.getSemantics());
            n.put("needsParam", opt == null ? null : opt.getNeedsParam());
            n.put("paramValue", t.getParamValue());
            n.put("required", opt == null ? null : opt.getRequired());
        }
        return root.toString();
    }

    /** 从快照 JSON 里读出条款清单（前端也按同一形状解析：{@code snapshot.terms}）。 */
    public static List<TermEntry> readTerms(String snapshotJson) {
        List<TermEntry> list = new ArrayList<>();
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return list;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = MAPPER.readTree(snapshotJson);
            com.fasterxml.jackson.databind.JsonNode terms = root.get("terms");
            if (terms == null || !terms.isArray()) {
                return list;
            }
            for (com.fasterxml.jackson.databind.JsonNode n : terms) {
                list.add(new TermEntry(
                        text(n, "termCode"),
                        text(n, "termName"),
                        text(n, "optionCode"),
                        text(n, "optionLabel"),
                        text(n, "semantics"),
                        text(n, "needsParam"),
                        text(n, "paramValue"),
                        n.hasNonNull("required") ? n.get("required").asBoolean() : null));
            }
        } catch (Exception e) {
            // 快照是已落库的举证材料，解析失败不能静默吞掉语义 → 抛非法状态，由上层记 ERROR
            throw new IllegalStateException("协议快照解析失败（快照可能被外部改写）", e);
        }
        return list;
    }

    /**
     * 构造模板来源（顺带生成那句人读的话）。
     *
     * @param scopeLabel "平台模板" / "租户模板"（调用方给中文，避免本类依赖枚举包外的东西）
     */
    public static TemplateSource templateSource(Long templateId, String templateName, String scope,
                                                String scopeLabel, int versionNo) {
        TemplateSource source = new TemplateSource();
        source.setTemplateId(templateId);
        source.setTemplateName(templateName);
        source.setScope(scope);
        source.setText("基于模板「" + (templateName == null ? "" : templateName) + "」（"
                + (scopeLabel == null ? "" : scopeLabel) + "）起草，第 " + versionNo + " 版");
        return source;
    }

    /**
     * 在**已有快照**上补写/覆盖模板来源，其余内容原样保留。
     *
     * <p>用途：模板发起的流程里，条款/设定都已落好之后再把来源写进快照 ——
     * 这样"基于模板 X"是**留痕**，而不是重建正文（重建正文会顺手动到已落好的取值）。</p>
     */
    public static String withTemplateSource(String snapshotJson, TemplateSource source) {
        if (snapshotJson == null || snapshotJson.isBlank() || source == null) {
            return snapshotJson;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode parsed = MAPPER.readTree(snapshotJson);
            if (!(parsed instanceof ObjectNode root)) {
                throw new IllegalStateException("协议快照不是 JSON 对象，无法写入模板来源");
            }
            ObjectNode t = root.putObject("templateSource");
            t.put("templateId", source.getTemplateId());
            t.put("templateName", source.getTemplateName());
            t.put("scope", source.getScope());
            t.put("text", source.getText());
            return root.toString();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("协议快照解析失败（快照可能被外部改写）", e);
        }
    }

    /** 从快照里读出模板来源；没有记录（从零起草）时返回 {@code null}。 */
    public static TemplateSource readTemplateSource(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = MAPPER.readTree(snapshotJson);
            com.fasterxml.jackson.databind.JsonNode t = root.get("templateSource");
            if (t == null || t.isNull()) {
                return null;
            }
            return new TemplateSource(
                    t.hasNonNull("templateId") ? t.get("templateId").asLong() : null,
                    text(t, "templateName"), text(t, "scope"), text(t, "text"));
        } catch (Exception e) {
            throw new IllegalStateException("协议快照解析失败（快照可能被外部改写）", e);
        }
    }

    /** 内容哈希：SHA-256(快照正文)，十六进制小写。 */
    public static String sha256(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算协议内容哈希失败", e);
        }
    }

    private static String text(com.fasterxml.jackson.databind.JsonNode n, String field) {
        return n.hasNonNull(field) ? n.get(field).asText() : null;
    }

    private static AgreementTermOption findOption(List<AgreementTermOption> options, String termCode, String optionCode) {
        if (options == null || termCode == null || optionCode == null) {
            return null;
        }
        for (AgreementTermOption o : options) {
            if (termCode.equals(o.getTermCode()) && optionCode.equals(o.getOptionCode())) {
                return o;
            }
        }
        return null;
    }
}
