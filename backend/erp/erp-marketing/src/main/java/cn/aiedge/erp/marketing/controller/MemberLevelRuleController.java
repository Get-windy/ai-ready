package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.MemberLevelChangeRow;
import cn.aiedge.erp.marketing.entity.MemberConfig;
import cn.aiedge.erp.marketing.service.MemberConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 会员等级规则：规则读取 + 自动升级评估/执行。
 *
 * <p>分工：等级档案 CRUD 仍在资料域 `MemberLevelController`（`/api/erp/member-level`，本次已补门槛字段）；
 * 本控制器只负责**营销域的自动化行为**——按门槛评估每个会员的目标等级、批量执行升降级。</p>
 *
 * <p>口径（业界通行，Oracle Membership / 有赞会员权益）：
 * 等级门槛可比「累计消费额」或「积分/成长值」；命中**门槛最高的那一级**为目标等级；
 * 执行受《会员设置》的「会员自动升级」开关门控。</p>
 */
@Slf4j
@Tag(name = "会员等级规则")
@RestController
@RequestMapping("/api/erp/marketing/member-level")
@RequiredArgsConstructor
public class MemberLevelRuleController {

    private final JdbcTemplate jdbcTemplate;
    private final MemberConfigService memberConfigService;

    @Operation(summary = "等级规则清单（含门槛/保级周期/默认等级）")
    @SaCheckPermission("marketing:member-level:view")
    @GetMapping("/rules")
    public Result<List<Map<String, Object>>> rules() {
        Long tenantId = tenantId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, level_name, discount_rate, sort_order, status, upgrade_amount, upgrade_points, keep_months, is_default "
                        + "FROM erp_member_level WHERE deleted = 0 AND tenant_id = ? ORDER BY sort_order", tenantId);
        return Result.ok(rows);
    }

    @Operation(summary = "评估会员等级（dry-run：只算不改，返回将升级/降级的清单）")
    @SaCheckPermission("marketing:member-level:create")
    @PostMapping("/evaluate")
    public Result<Map<String, Object>> evaluate(@RequestParam(defaultValue = "500") Integer limit) {
        List<MemberLevelChangeRow> changes = evaluateInternal(Math.min(Math.max(limit, 1), 5000));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", changes.size());
        out.put("upgrade", changes.stream().filter(c -> "UPGRADE".equals(c.getAction())).count());
        out.put("downgrade", changes.stream().filter(c -> "DOWNGRADE".equals(c.getAction())).count());
        out.put("init", changes.stream().filter(c -> "INIT".equals(c.getAction())).count());
        out.put("changes", changes);
        out.put("autoUpgradeEnabled", isAutoUpgradeEnabled());
        return Result.ok(out);
    }

    @Operation(summary = "执行等级升降级（写 biz_party.member_level；受「会员自动升级」开关门控）")
    @SaCheckPermission("marketing:member-level:create")
    @PostMapping("/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> apply(@RequestParam(defaultValue = "500") Integer limit,
                                             @RequestParam(defaultValue = "false") Boolean force) {
        if (!force && !isAutoUpgradeEnabled()) {
            throw new IllegalArgumentException("《会员设置》中的「会员自动升级」为关闭状态，"
                    + "请在会员设置中开启后再执行（或显式传 force=true 覆盖本次）");
        }
        List<MemberLevelChangeRow> changes = evaluateInternal(Math.min(Math.max(limit, 1), 5000));
        int applied = 0;
        for (MemberLevelChangeRow c : changes) {
            if ("KEEP".equals(c.getAction())) continue;
            jdbcTemplate.update("UPDATE biz_party SET member_level = ?, update_time = ? WHERE id = ? AND deleted = 0",
                    c.getTargetLevel(), Timestamp.valueOf(java.time.LocalDateTime.now()), c.getPartnerId());
            applied++;
        }
        log.info("[会员等级] 批量升降级执行完成：评估 {} 人，写入 {} 人", changes.size(), applied);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("evaluated", changes.size());
        out.put("applied", applied);
        return Result.ok(out);
    }

    // ══════════════ 内部 ══════════════

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    private boolean isAutoUpgradeEnabled() {
        MemberConfig cfg = memberConfigService.getConfig();
        return cfg != null && Integer.valueOf(1).equals(cfg.getAutoUpgradeEnabled());
    }

    /**
     * 评估：会员（biz_party 有会员卡号/会员名称者）逐人算目标等级。
     * 目标等级 = 满足「累计消费额 >= upgrade_amount」或「积分 >= upgrade_points」中 **sort_order 最大**的一级；
     * 都不满足则落到默认等级（is_default=1，无则 sort_order 最小的一级）。
     */
    private List<MemberLevelChangeRow> evaluateInternal(int limit) {
        Long tenantId = tenantId();
        List<Map<String, Object>> levels = jdbcTemplate.queryForList(
                "SELECT level_name, sort_order, upgrade_amount, upgrade_points, is_default FROM erp_member_level "
                        + "WHERE deleted = 0 AND tenant_id = ? AND status = 1 ORDER BY sort_order", tenantId);
        if (levels.isEmpty()) throw new IllegalArgumentException("尚未配置会员等级规则");

        String defaultLevel = null;
        for (Map<String, Object> l : levels) {
            if (Integer.valueOf(1).equals(l.get("is_default"))) {
                defaultLevel = String.valueOf(l.get("level_name"));
                break;
            }
        }
        if (defaultLevel == null) defaultLevel = String.valueOf(levels.get(0).get("level_name"));

        List<Map<String, Object>> members = jdbcTemplate.queryForList(
                "SELECT id, party_code, party_name, member_level, member_total_consume, points FROM biz_party "
                        + "WHERE deleted = 0 AND tenant_id = ? "
                        + "AND ((member_card_no IS NOT NULL AND member_card_no <> '') "
                        + "  OR (member_name IS NOT NULL AND member_name <> '')) "
                        + "ORDER BY id LIMIT ?", tenantId, limit);

        List<MemberLevelChangeRow> out = new ArrayList<>();
        for (Map<String, Object> m : members) {
            BigDecimal consume = m.get("member_total_consume") == null ? BigDecimal.ZERO
                    : new BigDecimal(String.valueOf(m.get("member_total_consume")));
            Integer points = m.get("points") == null ? 0 : Integer.valueOf(String.valueOf(m.get("points")));
            String current = m.get("member_level") == null ? null : String.valueOf(m.get("member_level"));

            String target = defaultLevel;
            String reason = "未达任何门槛，落到默认等级「" + defaultLevel + "」";
            for (Map<String, Object> l : levels) {
                BigDecimal amountGate = l.get("upgrade_amount") == null ? null
                        : new BigDecimal(String.valueOf(l.get("upgrade_amount")));
                Integer pointsGate = l.get("upgrade_points") == null ? null
                        : Integer.valueOf(String.valueOf(l.get("upgrade_points")));
                boolean hitAmount = amountGate != null && consume.compareTo(amountGate) >= 0;
                boolean hitPoints = pointsGate != null && points >= pointsGate;
                if (hitAmount || hitPoints) {
                    target = String.valueOf(l.get("level_name"));
                    reason = hitAmount
                            ? "累计消费 " + consume.stripTrailingZeros().toPlainString() + " ≥ 门槛 " + amountGate.stripTrailingZeros().toPlainString()
                            : "积分 " + points + " ≥ 门槛 " + pointsGate;
                }
            }

            MemberLevelChangeRow row = new MemberLevelChangeRow();
            row.setPartnerId(Long.valueOf(String.valueOf(m.get("id"))));
            row.setPartyCode(m.get("party_code") == null ? null : String.valueOf(m.get("party_code")));
            row.setPartyName(m.get("party_name") == null ? null : String.valueOf(m.get("party_name")));
            row.setCurrentLevel(current);
            row.setTargetLevel(target);
            row.setTotalConsume(consume);
            row.setPoints(points);
            row.setReason(reason);
            if (current == null || current.isBlank()) {
                row.setAction("INIT");
            } else if (target.equals(current)) {
                row.setAction("KEEP");
            } else {
                row.setAction(levelOrder(levels, target) > levelOrder(levels, current) ? "UPGRADE" : "DOWNGRADE");
            }
            out.add(row);
        }
        return out;
    }

    /** 等级序号（sort_order 越大越高；未知名次返回 -1） */
    private int levelOrder(List<Map<String, Object>> levels, String levelName) {
        for (Map<String, Object> l : levels) {
            if (String.valueOf(l.get("level_name")).equals(levelName)) {
                return Integer.parseInt(String.valueOf(l.get("sort_order")));
            }
        }
        return -1;
    }
}
