package cn.aiedge.erp.purchase.analytics.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.purchase.analytics.dto.PurchaseAnalysisQueryDTO;
import cn.aiedge.erp.purchase.analytics.service.PurchaseAnalysisReportService;
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
 * 采购分析报表服务实现（按时间 / 按商品 / 按供应商）
 *
 * <p><b>取数架构</b>：与《销售分析》一致——把「采订 → 采购入库 → 采购退货」拆成三个
 * <b>按维度分组的小聚合块</b>，每块一条 SQL 独立分组（不跨块 JOIN，避免明细行被表头字段放大），
 * 再在 Java 侧按键合并、派生比率列、排序、分页与合计。</p>
 *
 * <p><b>多租户</b>：本类全部走 {@link JdbcTemplate} 手写 SQL，MyBatis-Plus 的多租户拦截器
 * <b>不生效</b>，故每条 SQL 都显式带 {@code tenant_id = ?}；所有用户输入均以 {@code ?} 传入，
 * SQL 文本只拼接固定片段与白名单维度表达式。</p>
 *
 * <p><b>口径</b>（《采购分析开发文档》§4）：</p>
 * <ul>
 *   <li>采订段 —— 采购订单（<code>erp_purchase_order</code>，status &gt; 0 且 ≠ 6 已取消）</li>
 *   <li>采购段 —— 采购入库单（<code>erp_purchase_inbound</code>，status IN (4..9) 即已收货及以后，排除 10 已取消）</li>
 *   <li>退货段 —— 采购退货单（<code>erp_purchase_return</code>，status IN (2,4) 即已审批/已完成）</li>
 *   <li>实采金额 = 采购优惠后金额 − 采购退货折后金额（对标实测公式）</li>
 *   <li>退货率(%) = 采购退货折后金额 ÷ 采购优惠后金额 × 100（对标分母口径待复核，此处按金额口径）</li>
 * </ul>
 *
 * <p><b>缺口登记（确认无数据源 → 返回 null，前端显示 -，不返回 0 冒充）</b>：</p>
 * <ul>
 *   <li>浮动单位 / 采订浮动数量 / 采购浮动数量：本系统无「浮动单位」主数据与浮动数量列</li>
 *   <li>采购小单位数量：<code>erp_purchase_inbound_item</code> 无小单位列，
 *       按「来源采购订单明细 <code>order_item_id</code> 的小单位数量 × 实收/订购比」等比折算；
 *       无来源订单的入库行（直进）该列返回 null（显示 -）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAnalysisReportServiceImpl implements PurchaseAnalysisReportService {

    private final JdbcTemplate jdbcTemplate;

    /** 采购订单有效状态：排除草稿(0) 与已取消(6) */
    private static final String ORDER_VALID = "po.deleted = 0 AND po.status > 0 AND po.status <> 6";
    /** 采购入库有效状态（已收货及以后，排除已取消 10） */
    private static final String INBOUND_VALID = "pi.deleted = 0 AND pi.status IN (4,5,6,7,8,9)";
    /** 采购退货有效状态（已审批 / 已完成） */
    private static final String RETURN_VALID = "pr.deleted = 0 AND pr.status IN (2,4)";

    // ════════════════════════════════════════════════════════════════
    //  条件拼装（全部参数化；SQL 文本只含固定片段）
    // ════════════════════════════════════════════════════════════════

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
                sb.append(" AND ").append(expr).append(" <= ?::date");
                params.add(v.trim());
            }
        }

        /** 商品关键字：名称/货号/条码 任一命中（三段共用的三列表达式由调用方拼好） */
        void keyword(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" LIKE ?");
                params.add("%" + v.trim() + "%");
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

    private static Long tenantId() {
        return MyBatisPlusConfig.getCurrentTenantIdValue();
    }

    // ════════════════════════════════════════════════════════════════
    //  维度表达式（seg = order 采订段 / in 采购段 / ret 退货段）
    // ════════════════════════════════════════════════════════════════

    /** 按维度拼装的分组键（含粒度）：时间维度按 granularity 展开为天/周/月 */
    private String timeKey(String col, String gran) {
        if ("month".equals(gran)) {
            return "to_char(" + col + "::date, 'YYYY-MM')";
        }
        if ("week".equals(gran)) {
            return "to_char(date_trunc('week', " + col + "::date), 'YYYY-MM-DD')";
        }
        return "to_char(" + col + "::date, 'YYYY-MM-DD')";
    }

    // ════════════════════════════════════════════════════════════════
    //  FROM 片段
    // ════════════════════════════════════════════════════════════════

    /**
     * 商品档案列（仅「按商品」维度输出，且只出现在一个块内，避免合并时被覆盖）。
     * ⚠️ 各段明细表的档案列并集不同（采购入库明细无 image/brand/barcode/model/origin，
     * 采购退货明细无 specification），因此按段取「该表真实存在的列」，缺失列回落到商品主数据。
     */
    private static String productIdentity(String dim, String seg, String productAlias) {
        if (!"product".equals(dim)) {
            return "";
        }
        String item = switch (seg) {
            case "in" -> "pii";
            case "ret" -> "pri";
            default -> "poi";
        };
        boolean hasImage = !"in".equals(seg);
        boolean hasSpec = !"in".equals(seg);
        boolean hasExt = !"in".equals(seg);
        String specCol = "ret".equals(seg) ? "product_spec" : "specification";
        return ", MAX(" + (hasImage ? "COALESCE(" + item + ".image, " + productAlias + ".image_url)"
                : productAlias + ".image_url") + ") AS \"image\""
            + ", MAX(COALESCE(" + item + ".product_code, " + productAlias + ".product_code)) AS \"productCode\""
            + ", MAX(" + (hasExt ? "COALESCE(NULLIF(" + item + ".brand,''), " + productAlias + ".brand)"
                : productAlias + ".brand") + ") AS \"brand\""
            + ", MAX(" + (hasExt ? "COALESCE(" + item + ".barcode, " + productAlias + ".barcode)"
                : productAlias + ".barcode") + ") AS \"barcode\""
            + ", MAX(" + (hasSpec ? "COALESCE(" + item + "." + specCol + ", " + productAlias + ".spec)"
                : productAlias + ".spec") + ") AS \"spec\""
            + ", MAX(" + (hasExt ? "COALESCE(" + item + ".model, " + productAlias + ".model)"
                : productAlias + ".model") + ") AS \"model\""
            + ", MAX(" + (hasExt ? "COALESCE(" + item + ".origin, " + productAlias + ".origin)"
                : productAlias + ".origin") + ") AS \"origin\"";
    }

    /** 供应商编号（仅「按供应商」维度输出，只出现在一个块内） */
    private static String supplierIdentity(String dim, String supplierAlias) {
        if (!"supplier".equals(dim)) {
            return "";
        }
        // 别名指向 biz_party（PUR-BREAK-03：原先指向 0 行的 erp_supplier）
        return ", MAX(" + supplierAlias + ".party_code) AS \"supplierCode\"";
    }

    // ════════════════════════════════════════════════════════════════
    //  三个聚合块
    // ════════════════════════════════════════════════════════════════

    /** 采订段：采订单数 / 采订数量 / 采订小单位数量 / 订单优惠后金额 / 赠品数量 */
    private Map<String, Map<String, Object>> orderBlock(String dim, String gran, PurchaseAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("po.order_date", q.getStartDate());
        w.dateTo("po.order_date", q.getEndDate());
        w.like("po.supplier_name", q.getSupplierName());
        w.like("po.purchaser_name", q.getHandlerName());
        if (StringUtils.hasText(q.getWarehouseName())) {
            w.raw("po.warehouse_id IN (SELECT wh.id FROM erp_warehouse wh WHERE wh.deleted = 0 AND wh.tenant_id = ?"
                + " AND wh.warehouse_name LIKE ?)");
            w.addParams(tenantId(), "%" + q.getWarehouseName().trim() + "%");
        }
        if (StringUtils.hasText(q.getDeptName())) {
            w.raw("po.dept_id IN (SELECT d.id FROM sys_dept d WHERE d.deleted = 0 AND d.tenant_id = ? AND d.dept_name LIKE ?)");
            w.addParams(tenantId(), "%" + q.getDeptName().trim() + "%");
        }
        w.like("COALESCE(NULLIF(poi.brand,''), p.brand)", q.getBrand());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(poi.product_name LIKE ? OR poi.product_code LIKE ? OR poi.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + keyOf(dim, "order", gran) + " AS \"dimKey\","
            + " " + labelOf(dim, "order", gran) + " AS \"dimLabel\","
            + " COUNT(DISTINCT po.id) AS \"orderDocCount\","
            + " SUM(COALESCE(poi.quantity,0)) AS \"orderQty\","
            + " SUM(COALESCE(poi.small_unit_quantity,0)) AS \"orderSmallQty\","
            + " SUM(COALESCE(NULLIF(poi.discounted_amount,0), poi.amount, 0)) AS \"orderAmount\","
            + " SUM(CASE WHEN poi.gift THEN COALESCE(poi.quantity,0) ELSE 0 END) AS \"giftQty\""
            + productIdentity(dim, "order", "p")
            + " FROM erp_purchase_order_item poi"
            + " JOIN erp_purchase_order po ON po.id = poi.order_id AND po.tenant_id = poi.tenant_id"
            + " LEFT JOIN erp_product p ON p.id = poi.product_id AND p.tenant_id = poi.tenant_id AND p.deleted = 0"
            + " WHERE poi.tenant_id = ? AND " + ORDER_VALID + w.sql()
            + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 采购段：采购单数 / 采购数量 / 采购小单位数量 / 采购优惠后金额 */
    private Map<String, Map<String, Object>> inboundBlock(String dim, String gran, PurchaseAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("pi.inbound_date", q.getStartDate());
        w.dateTo("pi.inbound_date", q.getEndDate());
        w.like("pi.supplier_name", q.getSupplierName());
        w.like("pi.purchaser_name", q.getHandlerName());
        w.like("pi.warehouse_name", q.getWarehouseName());
        w.like("pi.department_name", q.getDeptName());
        w.like("p.brand", q.getBrand());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(pii.product_name LIKE ? OR pii.product_code LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw);
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        // 采购小单位数量：入库明细无小单位列，按来源采购订单明细的小单位数量 × (实收 ÷ 订购) 等比折算；
        // 无来源订单的直进行返回 null（不填 0）
        String smallQty = "SUM(CASE WHEN poi2.small_unit_quantity IS NULL THEN NULL"
            + " ELSE poi2.small_unit_quantity * COALESCE(pii.inbound_quantity,0)"
            + "      / NULLIF(COALESCE(NULLIF(poi2.quantity,0), NULLIF(pii.inbound_quantity,0)),0) END)";
        String sql = "SELECT " + keyOf(dim, "in", gran) + " AS \"dimKey\","
            + " " + labelOf(dim, "in", gran) + " AS \"dimLabel\","
            + " COUNT(DISTINCT pi.id) AS \"purchaseDocCount\","
            + " SUM(COALESCE(pii.inbound_quantity,0)) AS \"purchaseQty\","
            + " " + smallQty + " AS \"purchaseSmallQty\","
            + " SUM(COALESCE(pii.line_amount,0)) AS \"purchaseAmount\""
            + productIdentity(dim, "in", "p")
            + " FROM erp_purchase_inbound_item pii"
            + " JOIN erp_purchase_inbound pi ON pi.id = pii.inbound_id AND pi.tenant_id = pii.tenant_id"
            + " LEFT JOIN erp_purchase_order_item poi2 ON poi2.id = pii.order_item_id AND poi2.tenant_id = pii.tenant_id"
            + " LEFT JOIN erp_product p ON p.id = pii.product_id AND p.tenant_id = pii.tenant_id AND p.deleted = 0"
            + " WHERE pii.deleted = 0 AND pii.tenant_id = ? AND " + INBOUND_VALID + w.sql()
            + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /** 退货段：退货单数 / 退货数量 / 退货小单位数量 / 采购退货折后金额 */
    private Map<String, Map<String, Object>> returnBlock(String dim, String gran, PurchaseAnalysisQueryDTO q) {
        Where w = new Where();
        w.dateFrom("pr.return_date", q.getStartDate());
        w.dateTo("pr.return_date", q.getEndDate());
        w.like("pr.supplier_name", q.getSupplierName());
        w.like("pr.purchaser_name", q.getHandlerName());
        w.like("pr.warehouse_name", q.getWarehouseName());
        w.like("pr.department_name", q.getDeptName());
        w.like("p.brand", q.getBrand());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(pri.product_name LIKE ? OR pri.product_code LIKE ? OR pri.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT " + keyOf(dim, "ret", gran) + " AS \"dimKey\","
            + " " + labelOf(dim, "ret", gran) + " AS \"dimLabel\","
            + " COUNT(DISTINCT pr.id) AS \"returnDocCount\","
            + " SUM(COALESCE(pri.return_quantity,0)) AS \"returnQty\","
            + " SUM(COALESCE(pri.small_unit_quantity,0)) AS \"returnSmallQty\","
            + " SUM(COALESCE(pri.line_amount,0)) AS \"returnAmount\""
            + productIdentity(dim, "ret", "p")
            + supplierIdentity(dim, "s")
            + " FROM erp_purchase_return_item pri"
            + " JOIN erp_purchase_return pr ON pr.id = pri.return_id AND pr.tenant_id = pri.tenant_id"
            + " LEFT JOIN erp_product p ON p.id = pri.product_id AND p.tenant_id = pri.tenant_id AND p.deleted = 0"
            // 供应商档案走 biz_party（见 supplierCodeBlock 上方注释）
            + " LEFT JOIN biz_party s ON s.id = pr.supplier_id AND s.tenant_id = pr.tenant_id AND s.deleted = 0"
            + " WHERE pri.deleted = 0 AND pri.tenant_id = ? AND " + RETURN_VALID + w.sql()
            + " GROUP BY 1";
        return groupBy(sql, params);
    }

    /**
     * 供应商编号块（仅「按供应商」维度；采购订单/入库单块输出，保证只出现一次）
     *
     * <p><b>2026-09-21 修复（PUR-BREAK-03）：供应商取数改走 `biz_party`。</b>
     * 原先这里（以及上面退货块）`LEFT JOIN erp_supplier`，而 `erp_supplier` **实测 0 行**
     * —— 那是个没人写、没人维护的重复供应商档案表（建表语句在 `DatabaseInitializer` 里，
     * 消费方只有本类与 `SupplierSnapshotMapper`）；系统里**真正在用的供应商主数据是
     * `biz_party`（152 行，角色里含供应商）**，供应商管理页 `views/md/supplier` 也走
     * `partnerApi`（biz_party）。于是「按供应商」维度的编码列恒空。</p>
     *
     * <p>顺带修掉第二处断点：`MAX(po.supplier_name)` —— 实测 `erp_purchase_order.supplier_name`
     * **0/11 非空**（订单表根本不写这一列，只有入库单写），所以标签也恒空。
     * 现按「订单表冗余 → 供应商快照 → 档案」三级取值，与采购单据列表页
     * （`UnifiedPurchaseDocQueryServiceImpl` 用 `erp_purchase_order_partner_snapshot` 兜底）同口径。</p>
     */
    private Map<String, Map<String, Object>> supplierCodeBlock(String dim, PurchaseAnalysisQueryDTO q) {
        if (!"supplier".equals(dim)) {
            return Map.of();
        }
        Where w = new Where();
        w.dateFrom("po.order_date", q.getStartDate());
        w.dateTo("po.order_date", q.getEndDate());
        List<Object> params = new ArrayList<>();
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(po.supplier_id::text, ps.supplier_name, po.supplier_name) AS \"dimKey\","
            + " COALESCE(MAX(NULLIF(po.supplier_name, '')), MAX(ps.supplier_name), MAX(s.party_name)) AS \"dimLabel\","
            + " COALESCE(MAX(ps.supplier_code), MAX(s.party_code)) AS \"supplierCode\""
            + " FROM erp_purchase_order po"
            + " LEFT JOIN erp_purchase_order_partner_snapshot ps ON ps.order_id = po.id"
            + " LEFT JOIN biz_party s ON s.id = po.supplier_id AND s.tenant_id = po.tenant_id AND s.deleted = 0"
            + " WHERE po.tenant_id = ? AND " + ORDER_VALID + w.sql() + " GROUP BY 1";
        return groupBy(sql, params);
    }

    // ════════════════════════════════════════════════════════════════
    //  维度键 / 展示名（按段取列名）
    // ════════════════════════════════════════════════════════════════

    private String keyOf(String dim, String seg, String gran) {
        if ("time".equals(dim)) {
            String col = switch (seg) {
                case "in" -> "pi.inbound_date";
                case "ret" -> "pr.return_date";
                default -> "po.order_date";
            };
            return timeKey(col, gran);
        }
        if ("product".equals(dim)) {
            String item = switch (seg) {
                case "in" -> "pii";
                case "ret" -> "pri";
                default -> "poi";
            };
            return "COALESCE(" + item + ".product_id::text, " + item + ".product_name)";
        }
        String item = switch (seg) {
            case "in" -> "pi";
            case "ret" -> "pr";
            default -> "po";
        };
        return "COALESCE(" + item + ".supplier_id::text, " + item + ".supplier_name)";
    }

    private String labelOf(String dim, String seg, String gran) {
        if ("time".equals(dim)) {
            String col = switch (seg) {
                case "in" -> "pi.inbound_date";
                case "ret" -> "pr.return_date";
                default -> "po.order_date";
            };
            String key = timeKey(col, gran);
            String suffix = "month".equals(gran) ? " || '（月）'" : ("week".equals(gran) ? " || '（周）'" : "");
            return "MAX(" + key + suffix + ")";
        }
        if ("product".equals(dim)) {
            String item = switch (seg) {
                case "in" -> "pii";
                case "ret" -> "pri";
                default -> "poi";
            };
            return "MAX(" + item + ".product_name)";
        }
        String item = switch (seg) {
            case "in" -> "pi";
            case "ret" -> "pr";
            default -> "po";
        };
        return "MAX(" + item + ".supplier_name)";
    }

    // ════════════════════════════════════════════════════════════════
    //  SQL 执行与合并
    // ════════════════════════════════════════════════════════════════

    private Map<String, Map<String, Object>> groupBy(String sql, List<Object> params) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Object k = r.get("dimKey");
            String key = k == null ? "" : k.toString();
            if (key.isEmpty()) {
                continue;
            }
            Map<String, Object> exist = out.get(key);
            if (exist == null) {
                out.put(key, r);
            } else {
                r.forEach((ck, cv) -> {
                    if ("dimKey".equals(ck) || cv == null) {
                        return;
                    }
                    exist.merge(ck, cv, this::addValues);
                });
            }
        }
        return out;
    }

    private Object addValues(Object a, Object b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return toDecimal(a).add(toDecimal(b));
    }

    /**
     * 分块合并：数值列相加；标识列（字符串）取首个非空——避免字符串走相加被吞成 0。
     * （销售分析用 merge(ck, cv, addValues) 只对数值列生效，是因为标识列只出现在单个块内；
     * 本页商品/供应商档案列可能出现在多个段，故显式区分。）
     */
    @SafeVarargs
    private final Map<String, Map<String, Object>> merge(Map<String, Map<String, Object>>... blocks) {
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        for (Map<String, Map<String, Object>> block : blocks) {
            block.forEach((k, v) -> {
                Map<String, Object> row = rows.computeIfAbsent(k, key -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("dimKey", key);
                    return m;
                });
                v.forEach((ck, cv) -> {
                    if ("dimKey".equals(ck) || cv == null) {
                        return;
                    }
                    Object cur = row.get(ck);
                    if (cur == null) {
                        row.put(ck, cv);
                    } else if (isNumeric(cur) && isNumeric(cv)) {
                        row.put(ck, toDecimal(cur).add(toDecimal(cv)));
                    }
                    // 标识列冲突时保留首个非空值
                });
            });
        }
        rows.forEach((k, r) -> r.putIfAbsent("dimLabel", k));
        return rows;
    }

    private static boolean isNumeric(Object v) {
        return v instanceof Number || v instanceof BigDecimal;
    }

    // ════════════════════════════════════════════════════════════════
    //  数值工具
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

    private static BigDecimal num(Map<String, Object> m, String key) {
        return m == null ? BigDecimal.ZERO : toDecimal(m.get(key));
    }

    /** 比率（×100）：分母为 0 或缺失时返回 null（前端显示 -），不返回 0 冒充 */
    private static BigDecimal rate(Object numerator, Object denominator, int scale) {
        if (numerator == null) {
            return null;
        }
        BigDecimal den = toDecimal(denominator);
        if (den.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return toDecimal(numerator).multiply(BigDecimal.valueOf(100)).divide(den, scale, RoundingMode.HALF_UP);
    }

    // ════════════════════════════════════════════════════════════════
    //  派生列
    // ════════════════════════════════════════════════════════════════

    private void derive(Map<String, Object> m) {
        BigDecimal purchaseQty = num(m, "purchaseQty");
        BigDecimal purchaseAmount = num(m, "purchaseAmount");
        BigDecimal returnQty = num(m, "returnQty");
        BigDecimal returnAmount = num(m, "returnAmount");
        m.put("orderDocCount", num(m, "orderDocCount"));
        m.put("orderQty", num(m, "orderQty"));
        m.put("orderSmallQty", num(m, "orderSmallQty"));
        m.put("orderAmount", num(m, "orderAmount"));
        m.put("purchaseDocCount", num(m, "purchaseDocCount"));
        m.put("purchaseQty", purchaseQty);
        m.put("purchaseAmount", purchaseAmount);
        m.put("returnDocCount", num(m, "returnDocCount"));
        m.put("returnQty", returnQty);
        m.put("returnSmallQty", num(m, "returnSmallQty"));
        m.put("returnAmount", returnAmount);
        m.put("giftQty", num(m, "giftQty"));
        // 实采 = 采购 − 退货（对标公式：实采金额 = 采购优惠后金额 − 采购退货折后金额）
        m.put("netAmount", purchaseAmount.subtract(returnAmount));
        m.put("netQty", purchaseQty.subtract(returnQty));
        // 小单位数量整段无源时（直进入库）保持 null，不填 0 冒充
        if (m.get("purchaseSmallQty") == null) {
            m.put("netSmallQty", null);
        } else {
            m.put("netSmallQty", num(m, "purchaseSmallQty").subtract(num(m, "returnSmallQty")));
        }
        m.put("returnRate", rate(returnAmount, purchaseAmount, 2));
    }

    private Map<String, Object> summary(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (isNumeric(v)) {
                    s.merge(k, toDecimal(v), this::addValues);
                }
            });
        }
        s.remove("returnRate");
        derive(s);
        return s;
    }

    // ════════════════════════════════════════════════════════════════
    //  分页
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> pageResult(List<Map<String, Object>> all, PurchaseAnalysisQueryDTO q) {
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
            if (isNumeric(va) && isNumeric(vb)) {
                return toDecimal(va).compareTo(toDecimal(vb));
            }
            return va.toString().compareTo(vb.toString());
        };
        rows.sort(asc ? cmp : cmp.reversed());
    }

    // ════════════════════════════════════════════════════════════════
    //  入口
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> analysis(PurchaseAnalysisQueryDTO query) {
        String tab = query.getTab() == null ? "time" : query.getTab();
        String dim = switch (tab) {
            case "product", "supplier" -> tab;
            default -> "time";
        };
        String gran = query.getGranularity();

        Map<String, Map<String, Object>> rows = merge(
            orderBlock(dim, gran, query),
            inboundBlock(dim, gran, query),
            returnBlock(dim, gran, query),
            supplierCodeBlock(dim, query));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> m : rows.values()) {
            derive(m);
            out.add(m);
        }
        if ("time".equals(dim)) {
            sortBy(out, "dimKey", true);
        } else {
            sortBy(out, "purchaseAmount", false);
        }
        Map<String, Object> result = pageResult(out, query);
        result.put("summary", summary(out));
        return result;
    }
}
