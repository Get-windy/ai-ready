package cn.aiedge.workflow.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.workflow.entity.SysAuditRuleEntity;
import cn.aiedge.workflow.mapper.SysAuditRuleMapper;
import cn.aiedge.workflow.model.AuditRuleCatalog;
import cn.aiedge.workflow.model.AuditRuleSaveRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 审核设置（设置 → 系统配置 → 审核设置，菜单 80622）的业务实现。
 *
 * <p>对标口径（《审核设置开发文档》§8.1，ql361 实测 2026-09-18）：
 * <ul>
 *   <li>列表 = 固定的 16 类单据（{@link AuditRuleCatalog#DOC_TYPES}），**不增不减**；</li>
 *   <li>「审核设置」列 = 该单据已配置的规则条数；「摘要」列 = 规则的可读文本，
 *       格式逐字为 {@code 条件描述 + 时提交给[审批人]审核;}，多条以 {@code ;} 直接相连
 *       （例：{@code 商品低于成本价时提交给[杨生淮]审核;有赠品时提交给[杨生淮,高晓丽]审核;}）；</li>
 *   <li>未配置的单据摘要为空串（ql361 实测 16 行中仅 3 行有摘要）。</li>
 * </ul>
 *
 * <p>摘要为何由**后端**拼装：摘要是「配置的唯一可读表达」，比对/验收都要断言它；
 * 放在后端可让前端、接口验收、将来的打印/导出共用同一份口径，避免两处拼字符串各说各话。
 * 摘要**不落库**（落库会出现「规则改了摘要没改」的双真源），每次按 {@code rules} 现算。
 *
 * <p>租户口径：显式按当前会话租户过滤（镜像 {@code WorkflowServiceImpl} 的做法），
 * 而不是只依赖多租户插件 —— 因为平台超管经 {@code isTenantScopeExempt()} 整体豁免了插件注入，
 * 若不再显式收口，超管会看到所有租户的规则行、且同一单据类型出现多行。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRuleService {

    private final SysAuditRuleMapper auditRuleMapper;
    private final SysUserMapper sysUserMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 列表：返回 16 类单据的规则配置 + 条件目录（一次请求取齐，避免前端二次拉取）。
     *
     * @return {@code {"list": [...16 行...], "conditions": [{value,label}...]}}
     */
    public Map<String, Object> listAuditRules() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();

        // 一次查出本租户已落库的规则行，再按 docType 归并（16 类固定清单在前端是行序真源）
        LambdaQueryWrapper<SysAuditRuleEntity> wrapper = new LambdaQueryWrapper<SysAuditRuleEntity>()
                .in(SysAuditRuleEntity::getDocType, AuditRuleCatalog.DOC_TYPES.keySet());
        if (tenantId != null) {
            wrapper.eq(SysAuditRuleEntity::getTenantId, tenantId);
        }
        Map<String, SysAuditRuleEntity> saved = auditRuleMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(SysAuditRuleEntity::getDocType, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>(AuditRuleCatalog.DOC_TYPES.size());
        for (Map.Entry<String, String> doc : AuditRuleCatalog.DOC_TYPES.entrySet()) {
            list.add(toRow(doc.getKey(), doc.getValue(), saved.get(doc.getKey())));
        }

        List<Map<String, Object>> conditions = AuditRuleCatalog.CONDITIONS.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("value", e.getKey());
                    item.put("label", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("conditions", conditions);
        return result;
    }

    /**
     * 保存某种单据类型的审核规则（全量覆盖：前端提交的就是该单据的完整规则集）。
     *
     * @param docType 单据类型编码，必须是 16 类之一
     * @param request 规则集合（只含已启用项）
     * @return 保存后的该行（与列表行同结构，供前端回读校验）
     */
    public Map<String, Object> saveAuditRules(String docType, AuditRuleSaveRequest request) {
        String docName = AuditRuleCatalog.DOC_TYPES.get(docType);
        if (docName == null) {
            throw new BusinessException(400, "未知的单据类型: " + docType);
        }

        List<AuditRuleSaveRequest.RuleItem> normalized = normalize(request == null ? null : request.getRules());

        String json;
        try {
            json = MAPPER.writeValueAsString(normalized);
        } catch (Exception e) {
            log.error("[审核设置] 规则序列化失败: docType={}", docType, e);
            throw new BusinessException(500, "审核规则保存失败");
        }

        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        LambdaQueryWrapper<SysAuditRuleEntity> wrapper = new LambdaQueryWrapper<SysAuditRuleEntity>()
                .eq(SysAuditRuleEntity::getDocType, docType);
        if (tenantId != null) {
            wrapper.eq(SysAuditRuleEntity::getTenantId, tenantId);
        }
        List<SysAuditRuleEntity> existing = auditRuleMapper.selectList(wrapper);

        SysAuditRuleEntity entity;
        if (existing.isEmpty()) {
            entity = new SysAuditRuleEntity();
            entity.setDocType(docType);
            entity.setRules(json);
            // 显式带上租户：超管会话下插件不注入，靠这里落准归属
            if (tenantId != null) {
                entity.setTenantId(tenantId);
            }
            auditRuleMapper.insert(entity);
        } else {
            entity = existing.get(0);
            entity.setRules(json);
            auditRuleMapper.updateById(entity);
        }

        return toRow(docType, docName, entity);
    }

    // ==================== 内部实现 ====================

    /**
     * 落库前的规范化（防御式，不依赖前端校验）：
     * ① 丢掉条件编码非法的项；② 丢掉无审批人的项（没有审批人的规则等于没配）；
     * ③ 同一条件重复提交时只保留最后一条（避免摘要里出现重复条件）。
     */
    private List<AuditRuleSaveRequest.RuleItem> normalize(List<AuditRuleSaveRequest.RuleItem> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, AuditRuleSaveRequest.RuleItem> merged = new LinkedHashMap<>();
        for (AuditRuleSaveRequest.RuleItem rule : rules) {
            if (rule == null || rule.getCondition() == null) {
                continue;
            }
            String condition = rule.getCondition().trim();
            if (!AuditRuleCatalog.CONDITIONS.containsKey(condition)) {
                continue;
            }
            List<AuditRuleSaveRequest.Approver> approvers = rule.getApprovers() == null
                    ? Collections.emptyList()
                    : rule.getApprovers().stream()
                        .filter(a -> a != null && a.getUserId() != null && !a.getUserId().isBlank())
                        .collect(Collectors.toList());
            if (approvers.isEmpty()) {
                continue;
            }
            AuditRuleSaveRequest.RuleItem item = new AuditRuleSaveRequest.RuleItem();
            item.setCondition(condition);
            item.setApprovers(approvers);
            merged.put(condition, item);
        }
        return new ArrayList<>(merged.values());
    }

    /** 单行组装：把落库的 JSON 还原成「规则 + 摘要」的展示结构 */
    private Map<String, Object> toRow(String docType, String docName, SysAuditRuleEntity entity) {
        List<AuditRuleSaveRequest.RuleItem> stored = parseRules(entity == null ? null : entity.getRules());

        // 审批人姓名以 sys_user 现值为准（改名/离职后摘要不会显示旧名），取不到时回退配置时的快照
        Map<String, String> liveNames = resolveUserNames(stored);

        List<Map<String, Object>> rules = new ArrayList<>(stored.size());
        for (AuditRuleSaveRequest.RuleItem rule : stored) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("condition", rule.getCondition());
            item.put("conditionLabel", AuditRuleCatalog.CONDITIONS.get(rule.getCondition()));
            List<Map<String, Object>> approvers = new ArrayList<>();
            for (AuditRuleSaveRequest.Approver a : rule.getApprovers()) {
                Map<String, Object> approver = new LinkedHashMap<>();
                approver.put("userId", a.getUserId());
                approver.put("userName", displayName(a, liveNames));
                approvers.add(approver);
            }
            item.put("approvers", approvers);
            rules.add(item);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("docType", docType);
        row.put("docName", docName);
        row.put("configured", !rules.isEmpty());
        row.put("ruleCount", rules.size());
        row.put("summary", buildSummary(rules));
        row.put("rules", rules);
        return row;
    }

    /**
     * 摘要拼装（逐字对标 ql361）：每条规则 = {@code 条件描述 + 时提交给[审批人,审批人]审核;}
     * <p>多条规则直接首尾相接（每条自带 {@code ;} 结尾），与 ql361 实测文本一致。
     */
    private String buildSummary(List<Map<String, Object>> rules) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> rule : rules) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> approvers = (List<Map<String, Object>>) rule.get("approvers");
            String names = approvers.stream()
                    .map(a -> String.valueOf(a.get("userName")))
                    .collect(Collectors.joining(","));
            sb.append(rule.get("conditionLabel")).append("时提交给[").append(names).append("]审核;");
        }
        return sb.toString();
    }

    /**
     * 摘要里显示的审批人姓名，优先级：sys_user 现值 → 配置时的姓名快照 → 用户ID。
     * 最后一级兜底只是「不显示空白」，正常配置（从下拉里选人）不会走到。
     */
    private String displayName(AuditRuleSaveRequest.Approver approver, Map<String, String> liveNames) {
        String live = liveNames.get(approver.getUserId());
        if (live != null && !live.isBlank()) {
            return live;
        }
        String snapshot = approver.getUserName();
        if (snapshot != null && !snapshot.isBlank()) {
            return snapshot;
        }
        return approver.getUserId();
    }

    /**
     * 解析落库的规则 JSON 并过一遍 {@link #normalize}：
     * 手工改库 / 历史格式 / 条件枚举退役都可能留下「条件非法」或「没有审批人」的条目，
     * 这里统一降级（丢弃脏条目，而不是让整页 500）。
     */
    private List<AuditRuleSaveRequest.RuleItem> parseRules(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<AuditRuleSaveRequest.RuleItem> parsed =
                    MAPPER.readValue(json, new TypeReference<List<AuditRuleSaveRequest.RuleItem>>() {});
            return normalize(parsed);
        } catch (Exception e) {
            log.warn("[审核设置] 规则 JSON 解析失败，按未配置处理: {}", json, e);
            return Collections.emptyList();
        }
    }

    /** 批量解析审批人姓名（一次 IN 查询；失败时静默回退到快照，不影响列表可用） */
    private Map<String, String> resolveUserNames(List<AuditRuleSaveRequest.RuleItem> rules) {
        Set<String> ids = new LinkedHashSet<>();
        for (AuditRuleSaveRequest.RuleItem rule : rules) {
            for (AuditRuleSaveRequest.Approver a : rule.getApprovers()) {
                if (a.getUserId() != null && !a.getUserId().isBlank()) {
                    ids.add(a.getUserId());
                }
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> longIds = new HashSet<>();
        for (String id : ids) {
            try {
                longIds.add(Long.parseLong(id));
            } catch (NumberFormatException ignored) {
                // 非数字 id（理论上不会出现）交给快照兜底
            }
        }
        if (longIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<SysUser> users = sysUserMapper.selectBatchIds(longIds);
            Map<String, String> names = new LinkedHashMap<>();
            for (SysUser user : users) {
                String name = user.getRealName();
                if (name == null || name.isBlank()) {
                    name = user.getNickname();
                }
                if (name == null || name.isBlank()) {
                    name = user.getUsername();
                }
                if (name != null && !name.isBlank()) {
                    names.put(String.valueOf(user.getId()), name);
                }
            }
            return names;
        } catch (Exception e) {
            log.warn("[审核设置] 审批人姓名解析失败，回退配置时的姓名快照", e);
            return Collections.emptyMap();
        }
    }
}
