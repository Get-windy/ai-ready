package cn.aiedge.erp.marketing.analytics.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.marketing.analytics.dto.CommissionAnalyticsQuery;
import cn.aiedge.erp.marketing.analytics.service.CommissionAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业绩提成中心（菜单 80443）· 六视图实现。
 *
 * <p><b>结算状态机</b>（与《业绩提成中心开发文档》§4 一致）：
 * {@code erp_commission_record.status} = DRAFT（待确认）/ CONFIRMED（已确认）/ PAID（已结算）/ CANCELLED（已取消）；
 * 「已结 = PAID，未结 = DRAFT + CONFIRMED」。</p>
 *
 * <p><b>结算顺序硬约束</b>（对标页面提示条逐字）：「若存在某月份提成未结算，此后月份提成将不显示」——
 * 本实现取当年<b>最早出现未结提成的月份</b>为断点，其后月份一律返回 null（前端显示 `-`），
 * 且 {@code /settle} 拒绝跨过未结月份结算。</p>
 *
 * <p><b>提成矩阵数据源</b>：人员名单 = 已妥投配送任务（dms_task.status IN (5,6)）的配送员 ∪ 提成记录的推荐人；
 * 月份金额 = {@code erp_commission_record.commission_amount}（归属月 = COALESCE(confirm_time, create_time)）。</p>
 *
 * <p><b>业绩概览 / 业绩明细的行源</b>（2026-09-18 修正）：不再只读 {@code dms_task_item}，而是
 * {@code dms_task → dms_task_doc → 上游单据明细}（按 {@code doc_type} 分派：1 销售出库 / 2 销售退货 /
 * 3 调拨），{@code dms_task_item} 仅作为「无上游单据的临时配送单」的兜底。原因见 {@link #PERF_CTE}。</p>
 *
 * <p><b>缺口登记（已核实无数据源 → 返回 null，前端显示 `-`，不填 0 冒充）</b>：</p>
 * <ul>
 *   <li>{@code erp_commission_record} <b>无 rule_id</b>（已核实 information_schema 全部列）→
 *       提成构成 / 方案汇总提成的「提成方案名称 / 提成类型 / 提成规则 / 方案描述」四列无来源；
 *       提成金额（commission_amount）与配送员/职务（dms_rider）有来源。</li>
 *   <li>业绩概览「最优路线（Km）」：无「派单时冻结的规划距离快照」列（业界口径见方法内注释）</li>
 *   <li>业绩概览「配送毛利总额」：成本侧覆盖率 0/3（dms_settlement_item 0 行 / 油耗无配送员维度 /
 *       过路费·保险·折旧无源），接部分成本会产出失真毛利 → 整项留空</li>
 *   <li>业绩明细「退货数量 / 退货金额 / 赠品数量」：上游出库、退货明细有源（gift / is_gift 行级标志）；
 *       但调拨明细与兜底的 {@code dms_task_item} 无退货/赠品列 → 这些行的商品级口径为 NULL</li>
 * </ul>
 *
 * <p><b>多租户</b>：全部 JdbcTemplate 手写 SQL，每条均显式带 {@code tenant_id = ?}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionAnalyticsServiceImpl implements CommissionAnalyticsService {

    /** 已妥投的配送任务状态：已签收 / 已完成（排除 已取消 7 与 异常 8） */
    private static final String DELIVERED = "t.status IN (5,6)";

    private static final String[] MONTH_KEYS = {
        "m01", "m02", "m03", "m04", "m05", "m06", "m07", "m08", "m09", "m10", "m11", "m12"
    };

    private final JdbcTemplate jdbcTemplate;

    // ════════════════════════════════════════════════════════════════
    //  公共工具
    // ════════════════════════════════════════════════════════════════

    /**
     * 当前登录会话租户；**取不到时明确拒绝**，不回落到任何默认租户。
     *
     * <p>⚠️ 2026-09-23 修复：原实现为 {@code return t == null ? 1L : t}。这是本仓明令要根除的高危写法
     * （见 {@code DocQueryController#currentTenantId} 的注释）：解析不出租户时"回落 1"，
     * 等于**把系统租户（平台自身）的数据展示给另一个租户的用户**。
     * 本类端点均带 {@code @SaCheckLogin}，正常必然解析得出租户。</p>
     */
    private static Long tenantId() {
        Long t = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (t == null) {
            throw new BusinessException(401, "无法确定当前租户，请重新登录");
        }
        return t;
    }

    private static BigDecimal dec(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal money(Object v) {
        return dec(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean hasText(String v) {
        return StringUtils.hasText(v);
    }

    private static Map<String, Object> pageOf(List<Map<String, Object>> all, Integer pageNo, Integer size) {
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int sz = size == null || size < 1 ? 20 : size;
        int total = all.size();
        int from = Math.min((page - 1) * sz, total);
        int to = Math.min(from + sz, total);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", new ArrayList<>(all.subList(from, to)));
        out.put("total", total);
        out.put("page", page);
        out.put("size", sz);
        out.put("pages", (int) Math.ceil(total / (double) sz));
        return out;
    }

    private static Map<String, Object> sumAll(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v == null) {
                    return;
                }
                if (v instanceof BigDecimal) {
                    s.merge(k, v, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
                } else if (v instanceof Integer || v instanceof Long) {
                    s.merge(k, dec(v), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
                }
            });
        }
        return s;
    }

    /** 配送员主数据（按 id / 按姓名双索引），用于回填 职务 / 编号 / 部门 */
    private record RiderMeta(String duty, String riderNo, String deptName) {
    }

    private Map<String, RiderMeta> riderIndex() {
        Map<String, RiderMeta> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, user_id, real_name, rider_no, dept_name, rider_type FROM dms_rider"
                + " WHERE deleted = 0 AND tenant_id = ?", tenantId());
        for (Map<String, Object> r : rows) {
            RiderMeta meta = new RiderMeta(dutyText(r.get("rider_type")),
                r.get("rider_no") == null ? null : r.get("rider_no").toString(),
                r.get("dept_name") == null ? null : r.get("dept_name").toString());
            if (r.get("id") != null) {
                out.put("id:" + r.get("id"), meta);
            }
            if (r.get("user_id") != null) {
                out.put("uid:" + r.get("user_id"), meta);
            }
            if (r.get("real_name") != null) {
                out.put("nm:" + r.get("real_name"), meta);
            }
        }
        return out;
    }

    private static String dutyText(Object riderType) {
        int v = riderType instanceof Number n ? n.intValue() : 1;
        return switch (v) {
            case 1 -> "企业员工";
            case 2 -> "众包兼职";
            case 3 -> "外部平台配送员";
            case 4 -> "社会车辆司机";
            default -> "配送员";
        };
    }

    private static String roleValue(String roleKey) {
        if (!hasText(roleKey) || "全部".equals(roleKey)) {
            return null;
        }
        return switch (roleKey) {
            case "企业员工" -> "1";
            case "众包兼职" -> "2";
            case "外部平台配送员" -> "3";
            case "社会车辆司机" -> "4";
            default -> null;
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab1/Tab2：配送员 / 每月提成（年度矩阵）
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> riderMatrix(CommissionAnalyticsQuery query) {
        CommissionAnalyticsQuery q = query == null ? new CommissionAnalyticsQuery() : query;
        int year = q.getYear() == null ? LocalDate.now().getYear() : q.getYear();
        Map<String, RiderMeta> riders = riderIndex();

        // 人员名单：已妥投配送任务的配送员 ∪ 提成记录的推荐人
        Map<String, String> names = new LinkedHashMap<>();
        String taskSql = "SELECT COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,''), '未指定') AS nm,"
            + " MAX(COALESCE(t.rider_id, t.deliveryman_id)) AS rid"
            + " FROM dms_task t WHERE t.deleted = 0 AND t.tenant_id = ? AND " + DELIVERED
            + " GROUP BY 1";
        for (Map<String, Object> r : jdbcTemplate.queryForList(taskSql, tenantId())) {
            String nm = r.get("nm") == null ? "未指定" : r.get("nm").toString();
            if (hasText(q.getRiderName()) && !nm.contains(q.getRiderName().trim())) {
                continue;
            }
            names.put(nm, r.get("rid") == null ? null : r.get("rid").toString());
        }
        String refSql = "SELECT COALESCE(u.real_name, u.nickname, '未指定') AS nm, MAX(c.referrer_id) AS rid"
            + " FROM erp_commission_record c"
            + " LEFT JOIN sys_user u ON u.id = c.referrer_id AND u.deleted = 0"
            + " WHERE c.deleted = 0 AND c.tenant_id = ?"
            + " AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?"
            + " GROUP BY 1";
        for (Map<String, Object> r : jdbcTemplate.queryForList(refSql, tenantId(), year)) {
            String nm = r.get("nm") == null ? "未指定" : r.get("nm").toString();
            if (hasText(q.getRiderName()) && !nm.contains(q.getRiderName().trim())) {
                continue;
            }
            names.putIfAbsent(nm, r.get("rid") == null ? null : r.get("rid").toString());
        }

        // 角色过滤
        String role = roleValue(q.getRoleKey());
        if (role != null) {
            Set<String> keep = new LinkedHashSet<>();
            for (String nm : names.keySet()) {
                RiderMeta meta = metaOf(riders, nm, names.get(nm));
                if (meta != null && roleOf(meta.duty()).equals(role)) {
                    keep.add(nm);
                }
            }
            names.keySet().retainAll(keep);
        }

        // 月度提成金额
        Map<String, BigDecimal[]> amountByRider = new LinkedHashMap<>();
        String amtSql = "SELECT COALESCE(u.real_name, u.nickname, '未指定') AS nm,"
            + " EXTRACT(MONTH FROM COALESCE(c.confirm_time, c.create_time))::int AS m,"
            + " COALESCE(SUM(c.commission_amount),0) AS amt"
            + " FROM erp_commission_record c"
            + " LEFT JOIN sys_user u ON u.id = c.referrer_id AND u.deleted = 0"
            + " WHERE c.deleted = 0 AND c.tenant_id = ? AND c.status <> 'CANCELLED'"
            + " AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?"
            + " GROUP BY 1,2";
        for (Map<String, Object> r : jdbcTemplate.queryForList(amtSql, tenantId(), year)) {
            String nm = r.get("nm") == null ? "未指定" : r.get("nm").toString();
            int m = r.get("m") instanceof Number n ? n.intValue() : 0;
            if (m < 1 || m > 12) {
                continue;
            }
            amountByRider.computeIfAbsent(nm, k -> newZeroMonths())[m - 1] = money(r.get("amt"));
        }

        // 结算顺序约束：最早出现未结（DRAFT/CONFIRMED）提成的月份 → 其后月份不显示
        int blockedFrom = 13;
        String unsettledSql = "SELECT EXTRACT(MONTH FROM COALESCE(c.confirm_time, c.create_time))::int AS m"
            + " FROM erp_commission_record c"
            + " WHERE c.deleted = 0 AND c.tenant_id = ? AND c.status IN ('DRAFT','CONFIRMED')"
            + " AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?"
            + " GROUP BY 1 ORDER BY 1 LIMIT 1";
        List<Map<String, Object>> unsettled = jdbcTemplate.queryForList(unsettledSql, tenantId(), year);
        if (!unsettled.isEmpty() && unsettled.get(0).get("m") instanceof Number n) {
            blockedFrom = n.intValue();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : names.entrySet()) {
            RiderMeta meta = metaOf(riders, e.getKey(), e.getValue());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("riderName", e.getKey());
            m.put("duty", meta == null ? null : meta.duty());
            BigDecimal[] amounts = amountByRider.getOrDefault(e.getKey(), newZeroMonths());
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < 12; i++) {
                int month = i + 1;
                if (month >= blockedFrom && blockedFrom <= 12) {
                    // 断点月起（含断点月本身仍显示，其后月份不显示）：未结算月显示原值，其后为 null
                    m.put(MONTH_KEYS[i], month == blockedFrom ? amounts[i] : null);
                    if (month == blockedFrom) {
                        total = total.add(amounts[i]);
                    }
                    continue;
                }
                m.put(MONTH_KEYS[i], amounts[i]);
                total = total.add(amounts[i]);
            }
            m.put("total", money(total));
            rows.add(m);
        }
        rows.sort(Comparator.comparing((Map<String, Object> x) -> dec(x.get("total"))).reversed());

        Map<String, Object> out = pageOf(rows, q.getPage(), q.getSize());
        out.put("year", year);
        out.put("settledThrough", blockedFrom >= 13 ? 12 : blockedFrom - 1);
        out.put("blockedFrom", blockedFrom >= 13 ? null : blockedFrom);
        Map<String, Object> summary = sumAll(rows);
        summary.put("riderName", null);
        summary.put("duty", null);
        out.put("summary", summary);
        return out;
    }

    private static BigDecimal[] newZeroMonths() {
        BigDecimal[] a = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            a[i] = BigDecimal.ZERO;
        }
        return a;
    }

    private static RiderMeta metaOf(Map<String, RiderMeta> riders, String name, String riderId) {
        if (riderId != null) {
            RiderMeta byId = riders.get("id:" + riderId);
            if (byId == null) {
                byId = riders.get("uid:" + riderId);
            }
            if (byId != null) {
                return byId;
            }
        }
        return riders.get("nm:" + name);
    }

    private static String roleOf(String duty) {
        if (duty == null) {
            return "";
        }
        return switch (duty) {
            case "企业员工" -> "1";
            case "众包兼职" -> "2";
            case "外部平台配送员" -> "3";
            case "社会车辆司机" -> "4";
            default -> "";
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab3：提成构成（7 列）
    // ════════════════════════════════════════════════════════════════

    private static final String COMPOSITION_SELECT =
        "SELECT COALESCE(u.real_name, u.nickname, '未指定') AS \"riderName\","
        + " NULL AS \"duty\","
        + " NULL AS \"planName\","
        + " NULL AS \"commissionType\","
        + " NULL AS \"commissionRule\","
        + " COALESCE(SUM(c.commission_amount),0) AS \"commissionAmount\","
        + " NULL AS \"planDesc\""
        + " FROM erp_commission_record c"
        + " LEFT JOIN sys_user u ON u.id = c.referrer_id AND u.deleted = 0"
        + " WHERE c.deleted = 0 AND c.tenant_id = ? AND c.status <> 'CANCELLED'";

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> composition(CommissionAnalyticsQuery query) {
        CommissionAnalyticsQuery q = query == null ? new CommissionAnalyticsQuery() : query;
        StringBuilder sql = new StringBuilder(COMPOSITION_SELECT);
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        if (hasText(q.getRiderName())) {
            sql.append(" AND COALESCE(u.real_name, u.nickname, '') LIKE ?");
            params.add("%" + q.getRiderName().trim() + "%");
        }
        if (q.getYear() != null) {
            sql.append(" AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?");
            params.add(q.getYear());
        }
        sql.append(" GROUP BY 1,2,3,4,5,7 ORDER BY 6 DESC");
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, RiderMeta> riders = riderIndex();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql.toString(), params.toArray())) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            RiderMeta meta = metaOf(riders, String.valueOf(r.get("riderName")), null);
            m.put("duty", meta == null ? null : meta.duty());
            m.put("commissionAmount", money(r.get("commissionAmount")));
            if (Boolean.TRUE.equals(q.getHideZero()) && dec(r.get("commissionAmount")).signum() == 0) {
                continue;
            }
            rows.add(m);
        }
        Map<String, Object> out = pageOf(rows, q.getPage(), q.getSize());
        out.put("summary", sumAll(rows));
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab4：方案汇总提成（5 列）
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> planSummary(CommissionAnalyticsQuery query) {
        CommissionAnalyticsQuery q = query == null ? new CommissionAnalyticsQuery() : query;
        // ⚠️ erp_commission_record 无 rule_id（已核实 information_schema）→ 方案维度四列无来源，如实返回 null
        StringBuilder sql = new StringBuilder(
            "SELECT NULL AS \"planName\", NULL AS \"commissionType\", NULL AS \"commissionRule\","
                + " COALESCE(SUM(c.commission_amount),0) AS \"commissionAmount\", NULL AS \"planDesc\""
                + " FROM erp_commission_record c"
                + " WHERE c.deleted = 0 AND c.tenant_id = ? AND c.status <> 'CANCELLED'");
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        if (q.getYear() != null) {
            sql.append(" AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?");
            params.add(q.getYear());
        }
        sql.append(" HAVING COALESCE(SUM(c.commission_amount),0) <> 0");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql.toString(), params.toArray())) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("commissionAmount", money(r.get("commissionAmount")));
            rows.add(m);
        }
        Map<String, Object> out = pageOf(rows, q.getPage(), q.getSize());
        out.put("summary", sumAll(rows));
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab5：业绩概览（21 列）
    // ════════════════════════════════════════════════════════════════

    /**
     * 业绩行源公共 CTE 前缀：{@code WITH pack AS (…), perf_line_raw AS (…), perf_line AS (…)}。
     *
     * <p><b>为什么不能只读 {@code dms_task_item}</b>：配送域实行「口径分流」（见 dms 模块
     * {@code TaskService} 的「口径分流」注释）—— ① 有来源上游单据（销售出库单等）的配送单，
     * 表头发货数量/金额/重量/体积一律由上游单据聚合，<b>商品明细不写 dms_task_item</b>，
     * 查看时穿透上游单据；② 无来源单据的临时配送单才保留 dms_task_item（定位为装载/货物描述）。
     * 实测 {@code dms_task_item} 的 product_id 全为 NULL，而 {@code dms_task_doc} 指向真实上游单据，
     * 故「只读 dms_task_item」在主流场景恒为空集 —— 行源必须改为
     * {@code dms_task → dms_task_doc → 上游单据明细}，并把 {@code dms_task_item} 作为兜底。</p>
     *
     * <p><b>上游单据分派</b>（{@code dms_task_doc.doc_type}，取值口径见 DmsTaskDoc 实体）：
     * 1 = 销售出库单 → {@code erp_sale_outbound / erp_sale_outbound_item}；
     * 2 = 销售退货单 → {@code erp_sale_return_doc / erp_sale_return_doc_item}；
     * 3 = 调拨单 → {@code erp_stock_transfer / erp_stock_transfer_item}。
     * 上游关联优先按 {@code doc_id}（主键），{@code doc_id} 为空时退回 {@code doc_no}（实测多数行为空、
     * 仅少数行有值，故两条路都要留）。上游单据不再按 status 过滤：配送单已妥投即证明货已发出，
     * 上游单据状态只反映其自身流程进度。</p>
     *
     * <p><b>不填 0 的纪律</b>：退货/赠品/成本/包装各带一个 {@code *_known} 布尔列，聚合时用
     * {@code BOOL_AND} 判定「该分组内是否每一行都有可测来源」——只要有任一行无源，整列返回 NULL
     * （未知）而非 0（测得的零）。0 会让毛利虚高、让比率看似有效实则错误。</p>
     */
    private static final String PERF_CTE =
        "WITH pack AS ("
            // 包装分档取 erp_product_unit.unit_type 的 SMALL/MEDIUM/LARGE 枚举（已有真实换算率与单位名），
            // 不新建包装表、不加冗余打包列（库里 19 张单据明细表的 small/mid/big_pack 实测全 0，是死列，勿复制该模式）。
            // 实测同一 product_id + unit_type 存在完全重复行 → 这里用聚合去重。
            + " SELECT u.product_id,"
            + " MIN(u.conversion_rate) FILTER (WHERE u.unit_type = 'SMALL')  AS small_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.unit_type = 'SMALL')  AS small_name,"
            + " MIN(u.conversion_rate) FILTER (WHERE u.unit_type = 'MEDIUM') AS mid_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.unit_type = 'MEDIUM') AS mid_name,"
            + " MIN(u.conversion_rate) FILTER (WHERE u.unit_type = 'LARGE')  AS big_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.unit_type = 'LARGE')  AS big_name,"
            // 兜底①：unit_type = 'UNIT_4'（实测 960，第 4 档）在 LARGE 缺失时充当大包装
            + " MIN(u.conversion_rate) FILTER (WHERE u.unit_type = 'UNIT_4') AS unit4_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.unit_type = 'UNIT_4') AS unit4_name,"
            // 兜底②：unit_type 为 NULL 但换算率 > 1 的「无名档位」（实测率 1~4）在 MEDIUM 缺失时充当包装档
            + " MIN(u.conversion_rate) FILTER (WHERE u.unit_type IS NULL AND u.conversion_rate > 1) AS loose_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.unit_type IS NULL AND u.conversion_rate > 1) AS loose_name,"
            // 兜底③：基本单位（小包装在实测数据里恒为换算率 1 的基本单位）
            + " MIN(u.conversion_rate) FILTER (WHERE u.is_base_unit = 1)     AS base_rate,"
            + " MAX(u.unit_name)       FILTER (WHERE u.is_base_unit = 1)     AS base_name"
            + " FROM erp_product_unit u WHERE u.deleted = 0 AND u.tenant_id = ?"
            + " GROUP BY u.product_id),"
        + " perf_line_raw AS ("
            // ── 链路 A（主流）：配送单 → 上游单据 → 上游单据明细 ──
            // 退货口径：退货在业务层是「独立正向单据 + 挂回原单」，负数只在财务层；因此退货行的
            // 发货数量/发货金额记 0，退货数量/退货金额记正向值（不写负数，避免污染发货口径）。
            // 赠品口径：行级标志列 + 单价 0（本项目 erp_sale_order_item.gift / erp_sale_return_doc_item.is_gift 先例），
            // 不靠 price_unit = 0 反推。
            + " SELECT COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,''), '未指定') AS rider_name,"
            + " COALESCE(t.rider_id, t.deliveryman_id) AS rider_id,"
            + " t.source_bill_no,"
            + " u.product_id, u.product_code, u.product_name, u.unit,"
            + " u.quantity, u.amount, u.weight, u.volume,"
            + " u.cost_price, u.cost_amount, u.cost_known,"
            + " u.return_qty, u.return_amount, u.return_known,"
            + " u.gift_qty, u.gift_known,"
            + " p.small_unit,"
            + " COALESCE(pk.small_rate, pk.base_rate) AS small_rate,"
            + " COALESCE(pk.mid_rate, pk.loose_rate)  AS mid_rate,"
            + " COALESCE(pk.big_rate, pk.unit4_rate)  AS big_rate,"
            + " COALESCE(pk.small_name, pk.base_name) AS base_unit_name,"
            + " COALESCE(pk.mid_name, pk.loose_name)  AS mid_name,"
            + " COALESCE(pk.big_name, pk.unit4_name)  AS big_name"
            + " FROM dms_task t"
            + " JOIN dms_task_doc d ON d.task_id = t.id AND d.deleted = 0 AND d.tenant_id = t.tenant_id"
            + " JOIN ("
            // doc_type = 1：销售出库单明细（发货）
            + "   SELECT d1.id AS doc_row_id,"
            + "     oi.product_id, oi.product_code, oi.product_name, oi.product_unit AS unit,"
            + "     oi.outbound_quantity AS quantity, oi.line_amount AS amount, oi.weight, oi.volume,"
            + "     oi.cost_price,"
            + "     COALESCE(oi.cost_amount, oi.cost_price * oi.outbound_quantity) AS cost_amount,"
            + "     (oi.cost_price IS NOT NULL) AS cost_known,"
            + "     NULL::numeric AS return_qty, NULL::numeric AS return_amount, FALSE AS return_known,"
            + "     CASE WHEN COALESCE(oi.gift, FALSE) THEN oi.outbound_quantity ELSE 0 END AS gift_qty, TRUE AS gift_known"
            + "   FROM dms_task_doc d1"
            + "   JOIN erp_sale_outbound o ON o.deleted = 0 AND o.tenant_id = d1.tenant_id"
            + "     AND ((d1.doc_id IS NOT NULL AND o.id = d1.doc_id)"
            + "          OR (d1.doc_id IS NULL AND o.outbound_no = d1.doc_no))"
            + "   JOIN erp_sale_outbound_item oi ON oi.outbound_id = o.id AND oi.deleted = 0"
            + "     AND oi.tenant_id = o.tenant_id"
            + "   WHERE d1.deleted = 0 AND d1.doc_type = 1 AND d1.tenant_id = ?"
            + "   UNION ALL"
            // doc_type = 2：销售退货单明细（退货；正向数量 + 独立单据类型）
            + "   SELECT d1.id,"
            + "     ri.product_id, ri.product_code, ri.product_name, ri.unit,"
            + "     ri.return_quantity, ri.line_amount, ri.weight, ri.volume,"
            + "     ri.ref_cost_price,"
            + "     COALESCE(ri.ref_cost_amount, ri.ref_cost_price * ri.return_quantity),"
            + "     (ri.ref_cost_price IS NOT NULL),"
            + "     ri.return_quantity, ri.line_amount, TRUE,"
            + "     CASE WHEN COALESCE(ri.is_gift, FALSE) THEN ri.return_quantity ELSE 0 END, TRUE"
            + "   FROM dms_task_doc d1"
            + "   JOIN erp_sale_return_doc r ON r.deleted = 0 AND r.tenant_id = d1.tenant_id"
            + "     AND ((d1.doc_id IS NOT NULL AND r.id = d1.doc_id)"
            + "          OR (d1.doc_id IS NULL AND r.return_doc_no = d1.doc_no))"
            + "   JOIN erp_sale_return_doc_item ri ON ri.return_doc_id = r.id AND ri.deleted = 0"
            + "     AND ri.tenant_id = r.tenant_id"
            + "   WHERE d1.deleted = 0 AND d1.doc_type = 2 AND d1.tenant_id = ?"
            + "   UNION ALL"
            // doc_type = 3：调拨单明细（无退货/赠品语义 → return_known/gift_known = FALSE，整列留 NULL）
            + "   SELECT d1.id,"
            + "     ti.product_id, ti.product_code, ti.product_name, ti.product_unit,"
            + "     ti.quantity, ti.line_amount, ti.weight, ti.volume,"
            + "     ti.unit_cost,"
            + "     COALESCE(ti.cost_amount, ti.unit_cost * ti.quantity),"
            + "     (ti.unit_cost IS NOT NULL),"
            + "     NULL::numeric, NULL::numeric, FALSE,"
            + "     NULL::numeric, FALSE"
            + "   FROM dms_task_doc d1"
            + "   JOIN erp_stock_transfer tr ON tr.deleted = 0 AND tr.tenant_id = d1.tenant_id"
            + "     AND ((d1.doc_id IS NOT NULL AND tr.id = d1.doc_id)"
            + "          OR (d1.doc_id IS NULL AND tr.transfer_no = d1.doc_no))"
            + "   JOIN erp_stock_transfer_item ti ON ti.transfer_id = tr.id AND ti.deleted = 0"
            + "     AND ti.tenant_id = tr.tenant_id"
            + "   WHERE d1.deleted = 0 AND d1.doc_type = 3 AND d1.tenant_id = ?"
            + " ) u ON u.doc_row_id = d.id"
            + " LEFT JOIN pack pk ON pk.product_id = u.product_id"
            + " LEFT JOIN erp_product p ON p.id = u.product_id AND p.deleted = 0 AND p.tenant_id = t.tenant_id"
            + " WHERE t.deleted = 0 AND t.tenant_id = ?";

    /** 链路 B（兜底）：无上游单据的临时配送单，才用本单商品明细 dms_task_item */
    private static final String PERF_CTE_FALLBACK =
        " UNION ALL"
            + " SELECT COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,''), '未指定'),"
            + " COALESCE(t.rider_id, t.deliveryman_id),"
            + " t.source_bill_no,"
            + " i.product_id, i.product_code, i.product_name, COALESCE(NULLIF(i.unit,''), p.unit),"
            + " i.quantity, i.amount, i.weight, i.volume,"
            + " p.cost_price,"
            + " CASE WHEN p.cost_price IS NULL THEN NULL ELSE p.cost_price * i.quantity END,"
            + " (p.cost_price IS NOT NULL),"
            // 本单明细无退货/赠品列（退货仅 dms_task 任务级汇总）→ 商品级口径未知，留 NULL 不填 0
            + " NULL::numeric, NULL::numeric, FALSE,"
            + " NULL::numeric, FALSE,"
            + " p.small_unit,"
            + " COALESCE(pk.small_rate, pk.base_rate),"
            + " COALESCE(pk.mid_rate, pk.loose_rate),"
            + " COALESCE(pk.big_rate, pk.unit4_rate),"
            + " COALESCE(pk.small_name, pk.base_name),"
            + " COALESCE(pk.mid_name, pk.loose_name),"
            + " COALESCE(pk.big_name, pk.unit4_name)"
            + " FROM dms_task t"
            + " JOIN dms_task_item i ON i.task_id = t.id AND i.deleted = 0 AND i.tenant_id = t.tenant_id"
            + " LEFT JOIN pack pk ON pk.product_id = i.product_id"
            + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0 AND p.tenant_id = t.tenant_id"
            + " WHERE t.deleted = 0 AND t.tenant_id = ?"
            + " AND NOT EXISTS (SELECT 1 FROM dms_task_doc d2"
            + "   WHERE d2.task_id = t.id AND d2.deleted = 0 AND d2.tenant_id = t.tenant_id)";

    /**
     * 派生列收口：换算关系 / 换算结果。
     *
     * <p>「换算关系」取该商品<b>最大有效包装档</b>的关系串（如 {@code 1箱=12瓶}），由
     * {@code erp_product_unit.conversion_rate} 现算，<b>不落库</b>（Odoo 立场：换算率只存主数据，
     * 单据行换算实时算；本项目单据明细的 conversion_relation/conversion_result 列实测全空，
     * 是「声明了但从未写入」的死列，故走查询期计算而非复制该失败模式）。
     * 换算率 &lt;= 1 的档位视为脏数据（实测 LARGE 存在换算率 1 的行）不参与拼接。
     * 「换算结果」= 数量 × 该换算率 = 折合基本单位数量。</p>
     */
    private static final String PERF_CTE_DERIVED =
        "), perf_line AS ("
            + " SELECT r.*,"
            + " CASE WHEN r.big_rate > 1 AND r.big_name IS NOT NULL AND r.base_unit_name IS NOT NULL"
            + "        THEN '1' || r.big_name || '='"
            + "             || CASE WHEN r.big_rate = trunc(r.big_rate) THEN trunc(r.big_rate)::bigint::text"
            + "                     ELSE r.big_rate::text END || r.base_unit_name"
            + "      WHEN r.mid_rate > 1 AND r.mid_name IS NOT NULL AND r.base_unit_name IS NOT NULL"
            + "        THEN '1' || r.mid_name || '='"
            + "             || CASE WHEN r.mid_rate = trunc(r.mid_rate) THEN trunc(r.mid_rate)::bigint::text"
            + "                     ELSE r.mid_rate::text END || r.base_unit_name"
            + "      ELSE NULL END AS conv_relation,"
            + " CASE WHEN r.big_rate > 1 THEN r.big_rate"
            + "      WHEN r.mid_rate > 1 THEN r.mid_rate ELSE NULL END AS conv_rate"
            + " FROM perf_line_raw r)";

    /**
     * 业绩概览的按配送员包装汇总。
     *
     * <p>口径：小/中/大包装 = Σ(数量 × 该档位换算率)，即「把本单数量按该档位单位折算后的基本单位量」
     * （对齐 SAP 交货行同时存 LFIMG 销售单位量与 LGMNG 基本单位量的双数量思路）。
     * 没有对应档位单位（如商品未维护 SMALL/MEDIUM/LARGE）→ 该列整列 NULL（未知），不填 0。</p>
     */
    private static final String PERF_PACK_CTE =
        ", perf_pack AS ("
            + " SELECT rider_name,"
            + " CASE WHEN BOOL_AND(small_rate IS NOT NULL) THEN SUM(quantity * small_rate) ELSE NULL END AS small_pack,"
            + " CASE WHEN BOOL_AND(mid_rate IS NOT NULL)   THEN SUM(quantity * mid_rate)   ELSE NULL END AS mid_pack,"
            + " CASE WHEN BOOL_AND(big_rate IS NOT NULL)   THEN SUM(quantity * big_rate)   ELSE NULL END AS big_pack"
            + " FROM perf_line GROUP BY rider_name)";

    private static final String PERF_FROM =
        " FROM dms_task t"
            + " LEFT JOIN perf_pack pp ON pp.rider_name ="
            + " COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,''), '未指定')"
            + " WHERE t.deleted = 0 AND t.tenant_id = ? AND " + DELIVERED;

    /**
     * 把 {@link #PERF_CTE} / {@link #PERF_CTE_FALLBACK} / {@link #PERF_CTE_DERIVED} 拼成完整
     * WITH 子句，并按 SQL 中占位符出现的先后顺序把参数追加进 {@code params}。
     * 两条链路各自带日期区间与配送员过滤，保证概览的按人包装汇总与明细口径一致。
     */
    private void appendPerfCte(StringBuilder sql, List<Object> params, CommissionAnalyticsQuery q) {
        sql.append(PERF_CTE);
        params.add(tenantId());                 // pack
        params.add(tenantId());                 // doc_type = 1
        params.add(tenantId());                 // doc_type = 2
        params.add(tenantId());                 // doc_type = 3
        params.add(tenantId());                 // 链路 A 的 dms_task
        appendLineFilters(sql, params, q);
        sql.append(PERF_CTE_FALLBACK);
        params.add(tenantId());                 // 链路 B 的 dms_task
        appendLineFilters(sql, params, q);
        sql.append(PERF_CTE_DERIVED);
    }

    /**
     * 行源公共过滤（妥投状态 / 日期区间 / 配送员），两条链路各调用一次。
     *
     * <p>妥投状态过滤必须放在这里：{@link #PERF_CTE} 与 {@link #PERF_CTE_FALLBACK} 的 WHERE 各自
     * 只写了 {@code deleted / tenant_id}，漏掉它会把自己配送中、异常的单据也算进业绩。</p>
     */
    private void appendLineFilters(StringBuilder sql, List<Object> params, CommissionAnalyticsQuery q) {
        sql.append(" AND ").append(DELIVERED);
        if (hasText(q.getStartDate())) {
            sql.append(" AND t.delivery_date >= ?::date");
            params.add(q.getStartDate().trim());
        }
        if (hasText(q.getEndDate())) {
            sql.append(" AND t.delivery_date <= ?::date");
            params.add(q.getEndDate().trim());
        }
        if (hasText(q.getRiderName())) {
            sql.append(" AND COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,'')) LIKE ?");
            params.add("%" + q.getRiderName().trim() + "%");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> performanceOverview(CommissionAnalyticsQuery query) {
        CommissionAnalyticsQuery q = query == null ? new CommissionAnalyticsQuery() : query;
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        // 行源 = 配送单（配送员维度）× 上游单据明细（小/中/大包装汇总，见 PERF_CTE）
        appendPerfCte(sql, params, q);
        sql.append(PERF_PACK_CTE);
        sql.append(
            "SELECT COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,''), '未指定') AS \"riderName\","
                + " MAX(COALESCE(t.rider_id, t.deliveryman_id)) AS \"riderId\","
                + " COUNT(*) AS \"deliverDocCount\","
                + " COALESCE(SUM(COALESCE(t.return_order_count,0)),0) AS \"returnDocCount\","
                + " COALESCE(SUM(COALESCE(t.estimated_distance,0)),0) AS \"deliverMileage\","
                + " COALESCE(SUM(COALESCE(t.total_items,0)),0) AS \"boxingQty\","
                + " COUNT(DISTINCT t.delivery_date) AS \"deliverTimes\","
                + " COALESCE(SUM(COALESCE(t.total_quantity,0)),0) AS \"shipQty\","
                + " COALESCE(SUM(COALESCE(t.goods_amount,0)),0) AS \"shipAmount\","
                + " COALESCE(SUM(COALESCE(t.return_quantity,0)),0) AS \"returnQty\","
                + " COALESCE(SUM(COALESCE(t.return_amount,0)),0) AS \"returnAmount\","
                + " COALESCE(SUM(COALESCE(t.total_weight,0)),0) AS \"weight\","
                + " COALESCE(SUM(COALESCE(t.total_volume,0)),0) AS \"volume\","
                + " MAX(pp.small_pack) AS \"smallPack\","
                + " MAX(pp.mid_pack) AS \"midPack\","
                + " MAX(pp.big_pack) AS \"bigPack\","
                + " COALESCE(SUM(COALESCE(t.box_quantity,0)),0) AS \"packageQty\","
                + " COUNT(DISTINCT t.customer_id) AS \"pointCount\""
                + PERF_FROM);
        params.add(tenantId());
        if (hasText(q.getStartDate())) {
            sql.append(" AND t.delivery_date >= ?::date");
            params.add(q.getStartDate().trim());
        }
        if (hasText(q.getEndDate())) {
            sql.append(" AND t.delivery_date <= ?::date");
            params.add(q.getEndDate().trim());
        }
        if (hasText(q.getRiderName())) {
            sql.append(" AND COALESCE(NULLIF(t.rider_name,''), NULLIF(t.deliveryman_name,'')) LIKE ?");
            params.add("%" + q.getRiderName().trim() + "%");
        }
        sql.append(" GROUP BY 1 ORDER BY \"deliverDocCount\" DESC");

        Map<String, RiderMeta> riders = riderIndex();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql.toString(), params.toArray())) {
            String nm = String.valueOf(r.get("riderName"));
            String rid = r.get("riderId") == null ? null : r.get("riderId").toString();
            RiderMeta meta = metaOf(riders, nm, rid);
            if (!matchRole(meta, q.getRoleKey())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("riderCode", meta == null ? null : meta.riderNo());
            m.put("riderName", nm);
            m.put("deptName", meta == null ? null : meta.deptName());
            m.put("deliverDocCount", dec(r.get("deliverDocCount")));
            m.put("returnDocCount", dec(r.get("returnDocCount")));
            // 仍然无源：最优路线（Km）。
            // 业界口径是「派单时冻结规划快照（planned_route_distance）+ 执行后用轨迹回填实际值」，
            // 事后重算不可审计（司机会申诉「当时按什么距离算的钱」），也不可拿 estimated_distance 硬套
            // —— 该列是建单时 DTO 透传值、无任何派生/快照语义（且 dms_task 无成本/路线快照列，
            // 本模块无权新增迁移）。故如实留 NULL，前端显示 `-`。
            m.put("bestRouteKm", null);
            m.put("deliverMileage", money(r.get("deliverMileage")));
            m.put("boxingQty", money(r.get("boxingQty")));
            m.put("deliverTimes", dec(r.get("deliverTimes")));
            m.put("shipQty", money(r.get("shipQty")));
            m.put("shipAmount", money(r.get("shipAmount")));
            m.put("returnQty", money(r.get("returnQty")));
            m.put("returnAmount", money(r.get("returnAmount")));
            m.put("weight", money(r.get("weight")));
            m.put("volume", money(r.get("volume")));
            // 小/中/大包装：按上游单据明细（无上游单据时按本单明细）折算到对应档位单位；
            // 该配送员名下任一行商品没有对应档位单位时整列 NULL（未知），不填 0。
            m.put("smallPack", r.get("smallPack") == null ? null : money(r.get("smallPack")));
            m.put("midPack", r.get("midPack") == null ? null : money(r.get("midPack")));
            m.put("bigPack", r.get("bigPack") == null ? null : money(r.get("bigPack")));
            // 仍然无源：配送毛利总额。配送毛利 = 收入 1 项 − 成本 N 项，成本必须可空且覆盖率可解释；
            // 本系统成本侧实测：dms_settlement_item（按 task_id 归集，最完整的单趟成本台账）0 行、
            // dms_vehicle_energy_log 只有车辆维度（车辆可能多人共用，无法按配送员归集）、
            // 过路费/保险/折旧无数据源（dms_vehicle 只有保险到期日无金额）。
            // 成本覆盖率 = 0/3，只要接一项就会给出「看似完整实则失真」的毛利（成本算少了 → 毛利虚高 →
            // 提成基数失真），故整项留 NULL，不填 0。
            m.put("grossProfit", null);
            m.put("packageQty", money(r.get("packageQty")));
            m.put("pointCount", dec(r.get("pointCount")));
            if (hasText(q.getDeptName()) && (meta == null || meta.deptName() == null
                || !meta.deptName().contains(q.getDeptName().trim()))) {
                continue;
            }
            rows.add(m);
        }
        Map<String, Object> out = pageOf(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = sumAll(rows);
        summary.put("riderCode", null);
        summary.put("riderName", null);
        summary.put("deptName", null);
        out.put("summary", summary);
        return out;
    }

    private static boolean matchRole(RiderMeta meta, String roleKey) {
        if (!hasText(roleKey) || "全部".equals(roleKey)) {
            return true;
        }
        return meta != null && roleKey.equals(meta.duty());
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab6：业绩明细（21 列）
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> performanceDetail(CommissionAnalyticsQuery query) {
        CommissionAnalyticsQuery q = query == null ? new CommissionAnalyticsQuery() : query;
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        // 行源 = 配送单 → 上游单据明细（主流）；无上游单据的临时配送单才回落到 dms_task_item
        appendPerfCte(sql, params, q);
        sql.append(
            "SELECT l.rider_name AS \"riderName\","
                + " MAX(l.rider_id) AS \"riderId\","
                + " COALESCE(l.product_name,'') AS \"productName\","
                + " COALESCE(l.product_code,'') AS \"productCode\","
                + " COALESCE(l.unit,'') AS \"unit\","
                + " COALESCE(SUM(COALESCE(l.quantity,0)),0) AS \"shipQty\","
                + " COALESCE(SUM(COALESCE(l.amount,0)),0) AS \"shipAmount\","
                // 退货：上游退货单明细的行才有退货值（正向数量），出库行与兜底行无商品级退货列。
                // 分组内任一行无源 → 整列 NULL（未知），绝不填 0（0 是「测得的零」）。
                + " CASE WHEN BOOL_AND(l.return_known) THEN SUM(COALESCE(l.return_qty,0)) ELSE NULL END AS \"returnQty\","
                + " CASE WHEN BOOL_AND(l.return_known) THEN SUM(COALESCE(l.return_amount,0)) ELSE NULL END AS \"returnAmount\","
                // 赠品：行级标志（上游出库/退货明细的 gift / is_gift）而非 price_unit = 0 反推；
                // 调拨单与兜底的临时配送明细无赠品语义 → 该行 gift_known = FALSE → 整列 NULL
                + " CASE WHEN BOOL_AND(l.gift_known) THEN SUM(COALESCE(l.gift_qty,0)) ELSE NULL END AS \"giftQty\","
                // 换算关系 / 换算结果：由 erp_product_unit.conversion_rate 现算，不落库（见 PERF_CTE_DERIVED）
                + " MAX(l.conv_relation) AS \"conversionRelation\","
                + " CASE WHEN BOOL_AND(l.conv_rate IS NOT NULL) THEN SUM(l.quantity * l.conv_rate) ELSE NULL END"
                + "   AS \"conversionResult\","
                // 小/中/大包装：Σ(数量 × 该档位换算率)；任一行无该档位 → 整列 NULL（不填 0）
                + " CASE WHEN BOOL_AND(l.small_rate IS NOT NULL) THEN SUM(l.quantity * l.small_rate) ELSE NULL END AS \"smallPack\","
                + " CASE WHEN BOOL_AND(l.mid_rate IS NOT NULL) THEN SUM(l.quantity * l.mid_rate) ELSE NULL END AS \"midPack\","
                + " CASE WHEN BOOL_AND(l.big_rate IS NOT NULL) THEN SUM(l.quantity * l.big_rate) ELSE NULL END AS \"bigPack\","
                + " COALESCE(SUM(COALESCE(l.weight,0)),0) AS \"weight\","
                + " COALESCE(SUM(COALESCE(l.volume,0)),0) AS \"volume\","
                // 小单位：优先商品档案 erp_product.small_unit，为空时取 erp_product_unit 的基本单位名
                + " COALESCE(MAX(l.small_unit), MAX(l.base_unit_name)) AS \"smallUnit\","
                + " COALESCE(SUM(COALESCE(l.quantity,0)),0) AS \"smallUnitQty\","
                // 商品毛利 = 发货金额 − 取到的成本。上游明细自带 cost_price/cost_amount；兜底链路取
                // erp_product.cost_price。分组内任一行成本未知（NULL）→ 整列 NULL，绝不用 0 顶替
                // （成本算少了会让毛利虚高、提成基数失真）。
                + " CASE WHEN BOOL_AND(l.cost_known)"
                + "      THEN SUM(COALESCE(l.amount,0) - COALESCE(l.cost_amount,0)) ELSE NULL END AS \"grossProfit\""
                + " FROM perf_line l WHERE 1 = 1");
        if (hasText(q.getProductName())) {
            sql.append(" AND (COALESCE(l.product_name,'') LIKE ? OR COALESCE(l.product_code,'') LIKE ?)");
            String kw = "%" + q.getProductName().trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        if (hasText(q.getWarehouseName())) {
            sql.append(" AND COALESCE(l.source_bill_no,'') LIKE ?");
            params.add("%" + q.getWarehouseName().trim() + "%");
        }
        sql.append(" GROUP BY 1,3,4,5 ORDER BY \"shipAmount\" DESC");

        Map<String, RiderMeta> riders = riderIndex();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql.toString(), params.toArray())) {
            String nm = String.valueOf(r.get("riderName"));
            String rid = r.get("riderId") == null ? null : r.get("riderId").toString();
            RiderMeta meta = metaOf(riders, nm, rid);
            if (!matchRole(meta, q.getRoleKey())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("riderCode", meta == null ? null : meta.riderNo());
            m.put("riderName", nm);
            m.put("deptName", meta == null ? null : meta.deptName());
            m.put("productName", r.get("productName"));
            m.put("productCode", r.get("productCode"));
            m.put("unit", r.get("unit"));
            m.put("shipQty", money(r.get("shipQty")));
            m.put("shipAmount", money(r.get("shipAmount")));
            // 退货/赠品/包装/换算：有源则给真值，无源（该分组存在没有对应列的行）为 null → 前端显示 `-`
            m.put("returnQty", r.get("returnQty") == null ? null : money(r.get("returnQty")));
            m.put("returnAmount", r.get("returnAmount") == null ? null : money(r.get("returnAmount")));
            m.put("giftQty", r.get("giftQty") == null ? null : money(r.get("giftQty")));
            m.put("conversionRelation", r.get("conversionRelation"));
            m.put("conversionResult", r.get("conversionResult") == null ? null : money(r.get("conversionResult")));
            m.put("smallPack", r.get("smallPack") == null ? null : money(r.get("smallPack")));
            m.put("midPack", r.get("midPack") == null ? null : money(r.get("midPack")));
            m.put("bigPack", r.get("bigPack") == null ? null : money(r.get("bigPack")));
            m.put("weight", money(r.get("weight")));
            m.put("volume", money(r.get("volume")));
            m.put("smallUnit", r.get("smallUnit"));
            m.put("smallUnitQty", money(r.get("smallUnitQty")));
            m.put("grossProfit", r.get("grossProfit") == null ? null : money(r.get("grossProfit")));
            if (Boolean.TRUE.equals(q.getHideZero()) && dec(r.get("shipQty")).signum() == 0
                && dec(r.get("shipAmount")).signum() == 0) {
                continue;
            }
            rows.add(m);
        }
        Map<String, Object> out = pageOf(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = sumAll(rows);
        summary.put("riderCode", null);
        summary.put("riderName", null);
        summary.put("deptName", null);
        summary.put("productName", null);
        summary.put("productCode", null);
        summary.put("unit", null);
        summary.put("smallUnit", null);
        out.put("summary", summary);
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  批量结算
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> settle(Integer year, Integer month, List<String> riderNames, String operator) {
        int y = year == null ? LocalDate.now().getYear() : year;
        int m = month == null ? LocalDate.now().getMonthValue() : month;
        if (m < 1 || m > 12) {
            throw BusinessException.badRequest("结算月份非法");
        }
        // 结算顺序硬约束：更早月份存在未结提成时不允许跳过
        List<Map<String, Object>> earlier = jdbcTemplate.queryForList(
            "SELECT EXTRACT(MONTH FROM COALESCE(c.confirm_time, c.create_time))::int AS m"
                + " FROM erp_commission_record c"
                + " WHERE c.deleted = 0 AND c.tenant_id = ? AND c.status IN ('DRAFT','CONFIRMED')"
                + " AND EXTRACT(YEAR FROM COALESCE(c.confirm_time, c.create_time)) = ?"
                + " AND EXTRACT(MONTH FROM COALESCE(c.confirm_time, c.create_time))::int < ?"
                + " GROUP BY 1 ORDER BY 1 LIMIT 1", tenantId(), y, m);
        if (!earlier.isEmpty()) {
            throw BusinessException.badRequest("存在未结算的早期月份提成，请按月份顺序连续结算");
        }
        StringBuilder sql = new StringBuilder(
            "UPDATE erp_commission_record SET status = 'PAID', pay_time = ?, update_time = ?"
                + " WHERE deleted = 0 AND tenant_id = ? AND status IN ('DRAFT','CONFIRMED')"
                + " AND EXTRACT(YEAR FROM COALESCE(confirm_time, create_time)) = ?"
                + " AND EXTRACT(MONTH FROM COALESCE(confirm_time, create_time))::int = ?");
        List<Object> params = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        params.add(now);
        params.add(now);
        params.add(tenantId());
        params.add(y);
        params.add(m);
        if (riderNames != null && !riderNames.isEmpty()) {
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < riderNames.size(); i++) {
                if (i > 0) {
                    in.append(',');
                }
                in.append('?');
                params.add(riderNames.get(i));
            }
            sql.append(" AND referrer_id IN (SELECT u.id FROM sys_user u WHERE u.deleted = 0"
                + " AND COALESCE(u.real_name, u.nickname) IN (").append(in).append("))");
        }
        int affected = jdbcTemplate.update(sql.toString(), params.toArray());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", y);
        out.put("month", m);
        out.put("settledCount", affected);
        out.put("operator", operator);
        log.info("[业绩提成中心] 批量结算 {}年{}月 共 {} 条（操作人 {}）", y, m, affected, operator);
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  提成方案列表（工具栏「提成方案」只读入口）
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> planList() {
        return jdbcTemplate.queryForList(
            "SELECT rule_code AS \"ruleCode\", rule_name AS \"ruleName\","
                + " commission_type AS \"commissionType\", calc_basis AS \"calcBasis\","
                + " calc_method AS \"calcMethod\", commission_value AS \"commissionValue\","
                + " min_order_amount AS \"minOrderAmount\", max_commission AS \"maxCommission\","
                + " COALESCE(remark,'') AS \"remark\","
                + " CASE WHEN status = 'ACTIVE' THEN '启用' ELSE COALESCE(status,'') END AS \"statusText\""
                + " FROM erp_commission_rule WHERE deleted = 0 AND tenant_id = ?"
                + " ORDER BY rule_code", tenantId());
    }
}
