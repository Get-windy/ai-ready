package cn.aiedge.erp.finance.analytics.service.impl;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.CollectionStatsAnalyticsService;
import cn.aiedge.erp.finance.analytics.support.AnalyticsSupport;
import cn.aiedge.erp.finance.analytics.support.SqlWhere;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 回款统计（菜单 80442）· 两视图聚合实现。
 *
 * <p><b>口径</b>（《回款统计开发文档》§4）：同一份回款数据按「职员」「部门」两个维度各出一个视图，
 * 每行给出 单量 + 收款金额 + 预收款金额 + 预订货收款金额 + 回款总金额，且
 * {@code 回款总金额 = 收款金额 + 预收款金额 + 预订货收款金额}（逐行与合计均成立）。</p>
 *
 * <p><b>三口径来源</b>：</p>
 * <ul>
 *   <li>收款金额 —— {@code erp_receipt.receipt_amount}（状态 &ge;2 且非 3 已拒绝 / 8 已取消）</li>
 *   <li>预收款金额 —— {@code erp_pre_receipt}（金额列 COALESCE(pre_receipt_amount, amount)，剔除 forfeited/refunded）</li>
 *   <li>预订货收款金额 —— {@code erp_sale_pre_order.received_deposit}（status&gt;0 且非 6 已取消）</li>
 * </ul>
 *
 * <p><b>维度归集按「名称」而非「ID」</b>：本系统历史单据的 经手人/部门 ID 存在不统一（同一部门名在不同单据上
 * 落不同 ID，如 9001 / 900009 / 1），按 ID 归集会把同一部门拆成多行；因此与《辅助核算余额表》同一做法——
 * 按名称归集、名称回主数据（sys_user / sys_department）取编号。</p>
 *
 * <p><b>多租户</b>：全部 JdbcTemplate 手写 SQL，每条均显式带 {@code tenant_id = ?}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionStatsAnalyticsServiceImpl implements CollectionStatsAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /** 聚合中间态 */
    private static final class Agg {
        String code;
        String name;
        BigDecimal docCount = BigDecimal.ZERO;
        BigDecimal receiptAmount = BigDecimal.ZERO;
        BigDecimal preReceiptAmount = BigDecimal.ZERO;
        BigDecimal preOrderAmount = BigDecimal.ZERO;
    }

    // ════════════════════════════════════════════════════════════════
    //  维度表达式
    // ════════════════════════════════════════════════════════════════

    /** 编号表达式：按名称回主数据（标量子查询，避免一码多名导致行膨胀） */
    private static String codeExpr(boolean staff, String nameExpr) {
        if (staff) {
            return "(SELECT u.username FROM sys_user u WHERE u.deleted = 0"
                + " AND (u.real_name = " + nameExpr + " OR u.nickname = " + nameExpr + ") ORDER BY u.id LIMIT 1)";
        }
        return "(SELECT d.dept_code FROM sys_department d WHERE d.deleted = 0"
            + " AND d.dept_name = " + nameExpr + " LIMIT 1)";
    }

    // ════════════════════════════════════════════════════════════════
    //  分页查询
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> page(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        boolean staff = !"dept".equalsIgnoreCase(q.getTab());
        Map<String, Agg> merged = new LinkedHashMap<>();
        collectReceipt(merged, q, staff);
        collectPreReceipt(merged, q, staff);
        collectPreOrder(merged, q, staff);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Agg a : merged.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            BigDecimal total = a.receiptAmount.add(a.preReceiptAmount).add(a.preOrderAmount);
            m.put("code", a.code);
            m.put("name", a.name);
            m.put("groupKey", a.name);
            m.put("docCount", a.docCount);
            m.put("receiptAmount", AnalyticsSupport.round(a.receiptAmount, 2));
            m.put("preReceiptAmount", AnalyticsSupport.round(a.preReceiptAmount, 2));
            m.put("preOrderDepositAmount", AnalyticsSupport.round(a.preOrderAmount, 2));
            m.put("totalAmount", AnalyticsSupport.round(total, 2));
            rows.add(m);
        }
        // 对标「按职员」按回款总金额降序；「按部门」按单量降序（管理部 86 > 客服部 34 > 配送部 22）
        if (staff) {
            AnalyticsSupport.sortDesc(rows, "totalAmount");
        } else {
            AnalyticsSupport.sortDesc(rows, "docCount");
        }

        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("code", null);
        summary.put("name", null);
        summary.put("groupKey", null);
        out.put("summary", summary);
        return out;
    }

    // ── 收款块 ──
    private void collectReceipt(Map<String, Agg> merged, AnalyticsQuery q, boolean staff) {
        String nameExpr = staff
            ? "COALESCE(NULLIF(r.sales_person_name,''), '未指定')"
            : "COALESCE(NULLIF(r.department_name,''), '未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("r.receipt_date", q.getStartDate());
        w.dateTo("r.receipt_date", q.getEndDate());
        if (staff) {
            w.like("r.sales_person_name", q.getStaffName());
        } else {
            w.like("r.department_name", q.getDeptName());
        }
        String sql = "SELECT " + nameExpr + " AS \"k\", MAX(" + codeExpr(staff, nameExpr) + ") AS \"c\","
            + " COUNT(*) AS \"dc\", COALESCE(SUM(COALESCE(r.receipt_amount,0)),0) AS \"amt\""
            + " FROM erp_receipt r"
            + " WHERE r.deleted = 0 AND r.tenant_id = ? AND r.status >= 2 AND r.status NOT IN (3,8)"
            + w.sql() + " GROUP BY 1";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Agg a = take(merged, r.get("k"));
            a.code = firstText(a.code, r.get("c"));
            a.docCount = a.docCount.add(AnalyticsSupport.toDecimal(r.get("dc")));
            a.receiptAmount = a.receiptAmount.add(AnalyticsSupport.toDecimal(r.get("amt")));
        }
        if (log.isDebugEnabled()) {
            log.debug("[回款统计] 收款块 {} 行", merged.size());
        }
    }

    // ── 预收款块 ──
    private void collectPreReceipt(Map<String, Agg> merged, AnalyticsQuery q, boolean staff) {
        String nameExpr = staff
            ? "COALESCE(NULLIF(pr.handler_name,''), '未指定')"
            : "COALESCE(NULLIF(pr.dept_name,''), '未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("pr.receipt_date", q.getStartDate());
        w.dateTo("pr.receipt_date", q.getEndDate());
        if (staff) {
            w.like("pr.handler_name", q.getStaffName());
        } else {
            w.like("pr.dept_name", q.getDeptName());
        }
        String sql = "SELECT " + nameExpr + " AS \"k\", MAX(" + codeExpr(staff, nameExpr) + ") AS \"c\","
            + " COUNT(*) AS \"dc\","
            // 预收款金额：pre_receipt_amount 在真库中恒为 0（由明细汇总，未回写），依次回落到 amount / total_amount
            + " COALESCE(SUM(COALESCE(NULLIF(pr.pre_receipt_amount,0), NULLIF(pr.amount,0),"
            + "   NULLIF(pr.total_amount,0), 0)),0) AS \"amt\""
            + " FROM erp_pre_receipt pr"
            + " WHERE pr.deleted = 0 AND pr.tenant_id = ?"
            + " AND pr.status IS DISTINCT FROM 'forfeited' AND pr.status IS DISTINCT FROM 'refunded'"
            + w.sql() + " GROUP BY 1";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Agg a = take(merged, r.get("k"));
            a.code = firstText(a.code, r.get("c"));
            a.docCount = a.docCount.add(AnalyticsSupport.toDecimal(r.get("dc")));
            a.preReceiptAmount = a.preReceiptAmount.add(AnalyticsSupport.toDecimal(r.get("amt")));
        }
    }

    // ── 预订货收款块 ──
    private void collectPreOrder(Map<String, Agg> merged, AnalyticsQuery q, boolean staff) {
        String nameExpr = staff
            ? "COALESCE(NULLIF(spo.handler_name,''), '未指定')"
            : "COALESCE(NULLIF(spo.dept_name,''), '未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("spo.order_date", q.getStartDate());
        w.dateTo("spo.order_date", q.getEndDate());
        if (staff) {
            w.like("spo.handler_name", q.getStaffName());
        } else {
            w.like("spo.dept_name", q.getDeptName());
        }
        String sql = "SELECT " + nameExpr + " AS \"k\", MAX(" + codeExpr(staff, nameExpr) + ") AS \"c\","
            + " COUNT(*) AS \"dc\", COALESCE(SUM(COALESCE(spo.received_deposit,0)),0) AS \"amt\""
            + " FROM erp_sale_pre_order spo"
            + " WHERE spo.deleted = 0 AND spo.tenant_id = ? AND spo.status > 0 AND spo.status <> 6"
            + w.sql() + " GROUP BY 1";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Agg a = take(merged, r.get("k"));
            a.code = firstText(a.code, r.get("c"));
            a.docCount = a.docCount.add(AnalyticsSupport.toDecimal(r.get("dc")));
            a.preOrderAmount = a.preOrderAmount.add(AnalyticsSupport.toDecimal(r.get("amt")));
        }
    }

    private static Agg take(Map<String, Agg> merged, Object key) {
        String k = key == null || key.toString().isBlank() ? "未指定" : key.toString();
        Agg a = merged.get(k);
        if (a == null) {
            a = new Agg();
            a.name = k;
            merged.put(k, a);
        }
        return a;
    }

    private static String firstText(String current, Object v) {
        if (current != null && !current.isBlank()) {
            return current;
        }
        return v == null || v.toString().isBlank() ? null : v.toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  行级「明细」钻取
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> detail(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        boolean staff = !"dept".equalsIgnoreCase(q.getTab());
        String key = q.getGroupKey();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("receipts", receiptDetail(q, staff, key));
        out.put("preReceipts", preReceiptDetail(q, staff, key));
        out.put("preOrders", preOrderDetail(q, staff, key));
        return out;
    }

    private List<Map<String, Object>> receiptDetail(AnalyticsQuery q, boolean staff, String key) {
        String nameExpr = staff ? "COALESCE(NULLIF(r.sales_person_name,''),'未指定')"
            : "COALESCE(NULLIF(r.department_name,''),'未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("r.receipt_date", q.getStartDate());
        w.dateTo("r.receipt_date", q.getEndDate());
        if (AnalyticsSupport.hasText(key)) {
            w.raw(nameExpr + " = ?").addParams(key);
        }
        String sql = "SELECT r.receipt_no AS \"docNo\", to_char(r.receipt_date,'YYYY-MM-DD') AS \"bizDate\","
            + " COALESCE(r.customer_name, '') AS \"partnerName\","
            + " COALESCE(r.receipt_amount,0) AS \"amount\", r.status AS \"status\","
            + " COALESCE(r.sales_person_name,'') AS \"staffName\", COALESCE(r.department_name,'') AS \"deptName\""
            + " FROM erp_receipt r"
            + " WHERE r.deleted = 0 AND r.tenant_id = ? AND r.status >= 2 AND r.status NOT IN (3,8)"
            + w.sql() + " ORDER BY r.receipt_date DESC, r.id DESC LIMIT 500";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    private List<Map<String, Object>> preReceiptDetail(AnalyticsQuery q, boolean staff, String key) {
        String nameExpr = staff ? "COALESCE(NULLIF(pr.handler_name,''),'未指定')"
            : "COALESCE(NULLIF(pr.dept_name,''),'未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("pr.receipt_date", q.getStartDate());
        w.dateTo("pr.receipt_date", q.getEndDate());
        if (AnalyticsSupport.hasText(key)) {
            w.raw(nameExpr + " = ?").addParams(key);
        }
        String sql = "SELECT pr.pre_receipt_no AS \"docNo\", to_char(pr.receipt_date,'YYYY-MM-DD') AS \"bizDate\","
            + " COALESCE(pr.customer_name,'') AS \"partnerName\","
            + " COALESCE(NULLIF(pr.pre_receipt_amount,0), NULLIF(pr.amount,0), NULLIF(pr.total_amount,0), 0)"
            + "   AS \"amount\", 0 AS \"status\","
            + " COALESCE(pr.handler_name,'') AS \"staffName\", COALESCE(pr.dept_name,'') AS \"deptName\""
            + " FROM erp_pre_receipt pr"
            + " WHERE pr.deleted = 0 AND pr.tenant_id = ?"
            + " AND pr.status IS DISTINCT FROM 'forfeited' AND pr.status IS DISTINCT FROM 'refunded'"
            + w.sql() + " ORDER BY pr.receipt_date DESC, pr.id DESC LIMIT 500";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        return jdbcTemplate.queryForList(sql, params.toArray());
    }

    private List<Map<String, Object>> preOrderDetail(AnalyticsQuery q, boolean staff, String key) {
        String nameExpr = staff ? "COALESCE(NULLIF(spo.handler_name,''),'未指定')"
            : "COALESCE(NULLIF(spo.dept_name,''),'未指定')";
        SqlWhere w = new SqlWhere();
        w.dateFrom("spo.order_date", q.getStartDate());
        w.dateTo("spo.order_date", q.getEndDate());
        if (AnalyticsSupport.hasText(key)) {
            w.raw(nameExpr + " = ?").addParams(key);
        }
        String sql = "SELECT spo.order_no AS \"docNo\", to_char(spo.order_date,'YYYY-MM-DD') AS \"bizDate\","
            + " COALESCE(spo.customer_name,'') AS \"partnerName\","
            + " COALESCE(spo.received_deposit,0) AS \"amount\", spo.status AS \"status\","
            + " COALESCE(spo.handler_name,'') AS \"staffName\", COALESCE(spo.dept_name,'') AS \"deptName\""
            + " FROM erp_sale_pre_order spo"
            + " WHERE spo.deleted = 0 AND spo.tenant_id = ? AND spo.status > 0 AND spo.status <> 6"
            + w.sql() + " ORDER BY spo.order_date DESC, spo.id DESC LIMIT 500";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        return jdbcTemplate.queryForList(sql, params.toArray());
    }
}
