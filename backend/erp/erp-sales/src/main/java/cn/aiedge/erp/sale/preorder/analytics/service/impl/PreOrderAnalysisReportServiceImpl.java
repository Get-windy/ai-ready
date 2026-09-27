package cn.aiedge.erp.sale.preorder.analytics.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.sale.preorder.analytics.dto.PreOrderAnalysisQueryDTO;
import cn.aiedge.erp.sale.preorder.analytics.service.PreOrderAnalysisReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预订货汇总报表服务实现（商品预订货分析 / 客户预订货分析）
 *
 * <p><b>取数红线</b>（对标页黄底提示「已取消、已驳回订单不参与统计」）：
 * {@code erp_sale_pre_order.status >= 0}——已取消（−1）与驳回负值状态一律剔除。</p>
 *
 * <p><b>口径</b>（《预订货查询开发文档》§4 三量递进 订 → 转 → 发）：</p>
 * <ul>
 *   <li>预订货数量 = SUM(item.quantity)；已订数量 = SUM(item.ordered_quantity)；已发数量 = SUM(item.shipped_quantity)</li>
 *   <li>未订/未发数量直接取明细列 un_ordered_quantity / un_shipped_quantity</li>
 *   <li>赠品三口径 = 赠品行（item.gift = true）的 数量 / 已订数量 / 已发数量</li>
 *   <li>预订金三口径取表头 received_deposit / unreceived_deposit / deposit_balance（客户维度按客户 SUM）</li>
 * </ul>
 *
 * <p><b>缺口登记（确认无数据源 → 返回 null，前端显示 -，不返回 0 冒充）</b>：</p>
 * <ul>
 *   <li>含税单价 / 价税合计 / 税额：{@code erp_sale_pre_order(_item)} 全表无税额、税率、含税金额列
 *       （已核 information_schema），本系统预订货单为不含税口径</li>
 *   <li>已订金额 / 已发金额：明细无对应金额列，按「已订（已发）数量 × 行折后单价（无折后价则取单价）」折算，
 *       折算口径对齐销售侧「数量 × 单价」惯例；若行价为 0 则结果为 0</li>
 * </ul>
 *
 * <p><b>多租户</b>：全部手写 SQL，均显式带 {@code tenant_id = ?}；用户输入一律以 {@code ?} 传入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreOrderAnalysisReportServiceImpl implements PreOrderAnalysisReportService {

    private final JdbcTemplate jdbcTemplate;

    /** 有效预订货单：剔除已取消 / 已驳回（负值状态） */
    private static final String VALID = "h.deleted = 0 AND h.status >= 0";

    /**
     * 当前登录会话租户；**取不到时明确拒绝**，不把 null 传进 SQL。
     *
     * <p>⚠️ 2026-09-23 修复：原实现直接返回可能为 null 的租户，而 SQL 里的 {@code tenant_id = null}
     * 恒不成立 ⇒ 列表**静默返回 0 行**，"数据凭空少了"却没有任何报错。
     * 按本仓已确立口径（{@code DocQueryController#currentTenantId}），解析不出租户应当明确报「请重新登录」。</p>
     */
    private static Long tenantId() {
        Long t = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (t == null) {
            throw new cn.aiedge.common.exception.BusinessException(401, "无法确定当前租户，请重新登录");
        }
        return t;
    }

    private static final class Where {
        private final StringBuilder sb = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        void raw(String sql) {
            if (StringUtils.hasText(sql)) {
                sb.append(" AND ").append(sql);
            }
        }

        void like(String expr, String v) {
            if (expr != null && StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" LIKE ?");
                params.add("%" + v.trim() + "%");
            }
        }

        void dateFrom(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" >= ?::date");
                params.add(v.trim());
            }
        }

        void dateTo(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" < ?::date + 1");
                params.add(v.trim());
            }
        }

        void addParams(Object... values) {
            for (Object o : values) {
                params.add(o);
            }
        }

        String sql() {
            return sb.toString();
        }

        List<Object> list() {
            return params;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab1：商品预订货分析（28 列口径）
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> productTab(PreOrderAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("h.order_date", q.getStartDate());
        w.dateTo("h.order_date", q.getEndDate());
        w.like("h.customer_name", q.getCustomerName());
        w.like("h.handler_name", q.getHandlerName());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(i.product_name LIKE ? OR i.product_code LIKE ? OR i.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(i.product_id::text, i.product_name) AS \"dimKey\","
            + " MAX(i.product_name) AS \"dimLabel\", MAX(i.image_url) AS \"image\","
            + " MAX(COALESCE(i.specification, p.spec)) AS \"spec\", MAX(COALESCE(i.model, p.model)) AS \"model\","
            + " MAX(COALESCE(i.origin, p.origin)) AS \"origin\", MAX(COALESCE(i.barcode, p.barcode)) AS \"barcode\","
            + " MAX(i.remark) AS \"remark\", MAX(i.product_code) AS \"productCode\","
            + " MAX(COALESCE(NULLIF(i.brand,''), p.brand)) AS \"brand\", MAX(i.unit) AS \"unit\","
            + " COALESCE(SUM(i.quantity),0) AS \"quantity\","
            + " MAX(i.conversion_result) AS \"conversionResult\", MAX(i.small_unit) AS \"smallUnit\","
            + " COALESCE(SUM(i.small_unit_quantity),0) AS \"smallUnitQty\","
            + " COALESCE(SUM(i.amount),0) AS \"amount\","
            + " COALESCE(SUM(i.discounted_amount),0) AS \"discountedAmount\","
            + " COALESCE(SUM(CASE WHEN i.gift THEN i.quantity ELSE 0 END),0) AS \"giftQty\","
            + " COALESCE(SUM(CASE WHEN i.gift THEN i.ordered_quantity ELSE 0 END),0) AS \"giftOrderQty\","
            + " COALESCE(SUM(CASE WHEN i.gift THEN i.shipped_quantity ELSE 0 END),0) AS \"giftShipQty\","
            + " COALESCE(SUM(i.ordered_quantity),0) AS \"orderedQty\","
            + " COALESCE(SUM(i.un_ordered_quantity),0) AS \"unOrderedQty\","
            + " COALESCE(SUM(i.shipped_quantity),0) AS \"shippedQty\","
            + " COALESCE(SUM(i.un_shipped_quantity),0) AS \"unShippedQty\","
            // 单价/折后单价：行价按数量加权（合计 ÷ 总数量），无数量时为 null
            + " CASE WHEN COALESCE(SUM(i.quantity),0) = 0 THEN NULL"
            + "      ELSE SUM(i.amount) / SUM(i.quantity) END AS \"unitPrice\","
            + " CASE WHEN COALESCE(SUM(i.quantity),0) = 0 THEN NULL"
            + "      ELSE SUM(i.discounted_amount) / SUM(i.quantity) END AS \"discountedPrice\""
            + " FROM erp_sale_pre_order_item i"
            + " JOIN erp_sale_pre_order h ON h.id = i.order_id AND h.tenant_id = i.tenant_id"
            + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.tenant_id = i.tenant_id AND p.deleted = 0"
            + " WHERE i.deleted = 0 AND i.tenant_id = ? AND " + VALID + w.sql()
            + " GROUP BY 1";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : rows) {
            if (r.get("dimLabel") == null) {
                r.put("dimLabel", r.get("dimKey"));
            }
            // 无税额/含税列：显式置 null（前端显示 -）
            r.put("taxUnitPrice", null);
            r.put("taxAmount", null);
            r.put("taxFee", null);
            r.put("rowKey", String.valueOf(r.get("dimKey")));
        }
        sortBy(rows, "amount", false);
        return pageResult(rows, q);
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab2：客户预订货分析（23 列口径）
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> customerTab(PreOrderAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("h.order_date", q.getStartDate());
        w.dateTo("h.order_date", q.getEndDate());
        w.like("h.customer_name", q.getCustomerName());
        w.like("h.handler_name", q.getHandlerName());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(i.product_name LIKE ? OR i.product_code LIKE ? OR i.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(h.customer_id::text, h.customer_name) AS \"dimKey\","
            + " MAX(h.customer_name) AS \"dimLabel\","
            + " MAX(COALESCE(bp.default_handler_name, h.handler_name)) AS \"defaultHandler\","
            + " MAX(COALESCE(bp.party_code, h.customer_code)) AS \"customerCode\","
            + " MAX(COALESCE(bp.party_level, h.customer_level)) AS \"customerLevel\","
            + " MAX(cat.category_name) AS \"categoryName\","
            + " MAX(bp.address) AS \"address\","
            + " MAX(h.warehouse_name) AS \"warehouseName\","
            + " MAX(COALESCE(h.region, bp.region)) AS \"region\","
            + " MAX(ct.contact_name) AS \"contactName\","
            + " MAX(COALESCE(bp.remark, h.customer_remark)) AS \"remark\","
            + " MAX(COALESCE(h.customer_ticket, bp.customer_one_pass)) AS \"customerTicket\","
            + " COALESCE(SUM(i.quantity),0) AS \"quantity\","
            + " COALESCE(SUM(i.amount),0) AS \"amount\","
            + " COALESCE(SUM(i.discounted_amount),0) AS \"discountedAmount\","
            + " COALESCE(SUM(i.ordered_quantity),0) AS \"orderedQty\","
            + " COALESCE(SUM(i.shipped_quantity),0) AS \"shippedQty\","
            // 已订/已发金额：明细无金额列，按 数量 × 行折后单价（无则取单价）折算（口径见类注释缺口登记）
            + " COALESCE(SUM(i.ordered_quantity * COALESCE(i.discounted_price, i.unit_price, 0)),0) AS \"orderedAmount\","
            + " COALESCE(SUM(i.shipped_quantity * COALESCE(i.discounted_price, i.unit_price, 0)),0) AS \"shippedAmount\""
            + " FROM erp_sale_pre_order_item i"
            + " JOIN erp_sale_pre_order h ON h.id = i.order_id AND h.tenant_id = i.tenant_id"
            + " LEFT JOIN biz_party bp ON bp.id = h.customer_id AND bp.tenant_id = h.tenant_id AND bp.deleted = 0"
            + " LEFT JOIN biz_party_category cat ON cat.id = bp.category_id"
            + " LEFT JOIN (SELECT DISTINCT ON (c.party_id) c.party_id, c.contact_name"
            + "   FROM biz_party_contact c WHERE c.deleted = 0 ORDER BY c.party_id, c.is_primary DESC NULLS LAST, c.id) ct"
            + "   ON ct.party_id = h.customer_id"
            + " WHERE i.deleted = 0 AND i.tenant_id = ? AND " + VALID + w.sql()
            + " GROUP BY 1, h.id";
        // 预订金三口径取表头，与明细聚合解耦（避免多明细放大表头金额）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String key = String.valueOf(r.get("dimKey"));
            Map<String, Object> acc = merged.get(key);
            if (acc == null) {
                merged.put(key, r);
                continue;
            }
            r.forEach((k, v) -> {
                if ("dimKey".equals(k) || v == null) {
                    return;
                }
                Object cur = acc.get(k);
                if (cur instanceof Number && v instanceof Number) {
                    acc.put(k, toDecimal(cur).add(toDecimal(v)));
                } else if (cur == null) {
                    acc.put(k, v);
                }
            });
        }
        List<Map<String, Object>> out = new ArrayList<>(merged.values());
        Map<String, Map<String, Object>> deposits = depositByCustomer(q);
        for (Map<String, Object> r : out) {
            Map<String, Object> dep = deposits.get(String.valueOf(r.get("dimKey")));
            r.put("receivedDeposit", dep == null ? BigDecimal.ZERO : toDecimal(dep.get("receivedDeposit")));
            r.put("unreceivedDeposit", dep == null ? BigDecimal.ZERO : toDecimal(dep.get("unreceivedDeposit")));
            r.put("depositBalance", dep == null ? BigDecimal.ZERO : toDecimal(dep.get("depositBalance")));
            // 无税额/含税列：显式置 null（前端显示 -）
            r.put("taxAmount", null);
            r.put("taxFee", null);
            if (r.get("dimLabel") == null) {
                r.put("dimLabel", r.get("dimKey"));
            }
            r.put("rowKey", String.valueOf(r.get("dimKey")));
        }
        sortBy(out, "amount", false);
        return pageResult(out, q);
    }

    /** 客户维度预订金三口径（表头级，按客户 SUM，不随明细放大） */
    private Map<String, Map<String, Object>> depositByCustomer(PreOrderAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("h.order_date", q.getStartDate());
        w.dateTo("h.order_date", q.getEndDate());
        w.like("h.customer_name", q.getCustomerName());
        w.like("h.handler_name", q.getHandlerName());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(h.customer_id::text, h.customer_name) AS \"dimKey\","
            + " COALESCE(SUM(h.received_deposit),0) AS \"receivedDeposit\","
            + " COALESCE(SUM(h.unreceived_deposit),0) AS \"unreceivedDeposit\","
            + " COALESCE(SUM(h.deposit_balance),0) AS \"depositBalance\""
            + " FROM erp_sale_pre_order h WHERE h.tenant_id = ? AND " + VALID + w.sql()
            + " GROUP BY 1";
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            out.put(String.valueOf(r.get("dimKey")), r);
        }
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  工具
    // ════════════════════════════════════════════════════════════════

    private static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** 合计行：数值列求和；单价类列按合计口径重算（金额总和 ÷ 数量总和） */
    private Map<String, Object> summary(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v instanceof BigDecimal || v instanceof Number) {
                    s.merge(k, toDecimal(v), (a, b) -> toDecimal(a).add(toDecimal(b)));
                }
            });
        }
        s.remove("dimKey");
        BigDecimal qty = toDecimal(s.get("quantity"));
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            s.put("unitPrice", null);
            s.put("discountedPrice", null);
        } else {
            s.put("unitPrice", toDecimal(s.get("amount")).divide(qty, 4, RoundingMode.HALF_UP));
            s.put("discountedPrice", toDecimal(s.get("discountedAmount")).divide(qty, 4, RoundingMode.HALF_UP));
        }
        return s;
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> all, PreOrderAnalysisQueryDTO q) {
        int page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : q.getSize();
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", new ArrayList<>(all.subList(from, to)));
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        out.put("pages", (int) Math.ceil(total / (double) size));
        out.put("summary", summary(all));
        return out;
    }

    private void sortBy(List<Map<String, Object>> rows, String field, boolean asc) {
        Comparator<Map<String, Object>> cmp = (a, b) -> {
            Object va = a.get(field);
            Object vb = b.get(field);
            if (va == null && vb == null) {
                return 0;
            }
            if (va == null) {
                return 1;
            }
            if (vb == null) {
                return -1;
            }
            if (va instanceof Number && vb instanceof Number) {
                return toDecimal(va).compareTo(toDecimal(vb));
            }
            return va.toString().compareTo(vb.toString());
        };
        rows.sort(asc ? cmp : cmp.reversed());
    }

    @Override
    public Map<String, Object> analysis(PreOrderAnalysisQueryDTO query) {
        String tab = query.getTab() == null ? "product" : query.getTab();
        return "customer".equals(tab) ? customerTab(query) : productTab(query);
    }
}
