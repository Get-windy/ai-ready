package cn.aiedge.erp.finance.analytics.service.impl;

import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.InvoiceStatsService;
import cn.aiedge.erp.finance.analytics.support.AnalyticsSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票统计（菜单 80455）· 增值税进销项月度台账实现。
 *
 * <p><b>数据源</b>：{@code invoice} 表（发票域 {@code cn.aiedge.erp.invoice}）——
 * 已核实其真实取值域完全支撑对标两组 7 列：</p>
 * <ul>
 *   <li>销项/进项由 {@code invoice_type} 区分（InvoiceType 枚举，含 isSalesType / isPurchaseType 语义）</li>
 *   <li>正数/负数由 {@code is_credit_note}（红字发票）区分</li>
 *   <li>{@code subtotal_amount}（不含税开票金额）/ {@code tax_amount}（税额）/ {@code total_amount}（价税合计）</li>
 *   <li>期间取 {@code invoice_date}</li>
 * </ul>
 *
 * <p><b>口径</b>：</p>
 * <ul>
 *   <li>销项开票金额 = 正数开票金额 − 负数开票金额（净额）；价税合计 = 金额 + 税额</li>
 *   <li>取得进项专票 = 采购类发票（PURCHASE_INVOICE / IMPORT_INVOICE）</li>
 *   <li>抵扣后应交税额 = 销项税额 − 进项税额（留抵为负）</li>
 *   <li>剔除 草稿 / 已拒绝 / 已取消 的发票（未实际开具，不进入税务台账）</li>
 *   <li>未勾选「进销均无发生的不显示」时，区间内<b>每个月都出行</b>（全 0 月也列，与对标实测一致）——
 *       月份骨架由 {@code generate_series} 生成，指标列 LEFT JOIN 真实聚合，不做任何数值兜底</li>
 * </ul>
 *
 * <p><b>多租户</b>：{@code invoice.tenant_id} 是 varchar(50)（与本系统其余表的 bigint 不同），
 * 故按字符串比较，仍以 {@code ?} 参数化传入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceStatsServiceImpl implements InvoiceStatsService {

    /** 销项（销售类）发票类型：与 InvoiceType.isSalesType() 逐字一致 */
    private static final String SALES_TYPES =
        "'SALES_INVOICE','TAX_INVOICE','ELECTRONIC_INVOICE','REGULAR_INVOICE',"
        + "'SPECIAL_INVOICE','VEHICLE_INVOICE','USED_VEHICLE_INVOICE','EXPORT_INVOICE'";

    /** 进项（采购类）发票类型：与 InvoiceType.isPurchaseType() 逐字一致 */
    private static final String PURCHASE_TYPES = "'PURCHASE_INVOICE','IMPORT_INVOICE'";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> page(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        YearMonth from = parseMonth(q.getMonthStart(), YearMonth.now().withMonth(1));
        YearMonth to = parseMonth(q.getMonthEnd(), YearMonth.now());
        if (from.isAfter(to)) {
            YearMonth t = from;
            from = to;
            to = t;
        }
        String start = from.atDay(1).toString();
        String end = to.atEndOfMonth().toString();

        String salesFlag = "i.invoice_type IN (" + SALES_TYPES + ")";
        String purchaseFlag = "i.invoice_type IN (" + PURCHASE_TYPES + ")";
        String credit = "COALESCE(i.is_credit_note, false) = true";
        String notCredit = "COALESCE(i.is_credit_note, false) = false";

        String sql = "SELECT to_char(m.d,'YYYY') AS \"year\", to_char(m.d,'MM') AS \"month\","
            + " COALESCE(a.spCnt,0) AS \"salesPosCount\","
            + " COALESCE(a.spAmt,0) AS \"salesPosAmount\","
            + " COALESCE(a.snCnt,0) AS \"salesNegCount\","
            + " COALESCE(ABS(a.snAmt),0) AS \"salesNegAmount\","
            + " COALESCE(a.spAmt,0) - COALESCE(ABS(a.snAmt),0) AS \"salesNetAmount\","
            + " COALESCE(a.sTotal,0) AS \"salesTotalAmount\","
            + " COALESCE(a.sTax,0) AS \"salesTaxAmount\","
            + " COALESCE(a.ppCnt,0) AS \"purPosCount\","
            + " COALESCE(a.ppAmt,0) AS \"purPosAmount\","
            + " COALESCE(a.pnCnt,0) AS \"purNegCount\","
            + " COALESCE(ABS(a.pnAmt),0) AS \"purNegAmount\","
            + " COALESCE(a.ppAmt,0) - COALESCE(ABS(a.pnAmt),0) AS \"purNetAmount\","
            + " COALESCE(a.pTotal,0) AS \"purTotalAmount\","
            + " COALESCE(a.pTax,0) AS \"purTaxAmount\","
            + " COALESCE(a.sTax,0) - COALESCE(a.pTax,0) AS \"taxPayable\""
            + " FROM (SELECT generate_series(?::date, ?::date, interval '1 month')::date AS d) m"
            + " LEFT JOIN ("
            + "   SELECT date_trunc('month', i.invoice_date)::date AS d,"
            + "    COUNT(*) FILTER (WHERE " + salesFlag + " AND " + notCredit + ") AS spCnt,"
            + "    SUM(i.subtotal_amount) FILTER (WHERE " + salesFlag + " AND " + notCredit + ") AS spAmt,"
            + "    COUNT(*) FILTER (WHERE " + salesFlag + " AND " + credit + ") AS snCnt,"
            + "    SUM(i.subtotal_amount) FILTER (WHERE " + salesFlag + " AND " + credit + ") AS snAmt,"
            + "    SUM(CASE WHEN " + credit + " THEN -i.total_amount ELSE i.total_amount END)"
            + "      FILTER (WHERE " + salesFlag + ") AS sTotal,"
            + "    SUM(CASE WHEN " + credit + " THEN -i.tax_amount ELSE i.tax_amount END)"
            + "      FILTER (WHERE " + salesFlag + ") AS sTax,"
            + "    COUNT(*) FILTER (WHERE " + purchaseFlag + " AND " + notCredit + ") AS ppCnt,"
            + "    SUM(i.subtotal_amount) FILTER (WHERE " + purchaseFlag + " AND " + notCredit + ") AS ppAmt,"
            + "    COUNT(*) FILTER (WHERE " + purchaseFlag + " AND " + credit + ") AS pnCnt,"
            + "    SUM(i.subtotal_amount) FILTER (WHERE " + purchaseFlag + " AND " + credit + ") AS pnAmt,"
            + "    SUM(CASE WHEN " + credit + " THEN -i.total_amount ELSE i.total_amount END)"
            + "      FILTER (WHERE " + purchaseFlag + ") AS pTotal,"
            + "    SUM(CASE WHEN " + credit + " THEN -i.tax_amount ELSE i.tax_amount END)"
            + "      FILTER (WHERE " + purchaseFlag + ") AS pTax"
            + "   FROM invoice i"
            + "   WHERE i.tenant_id = ? AND COALESCE(i.is_deleted, false) = false"
            + "    AND i.invoice_date >= ?::date AND i.invoice_date <= ?::date"
            + "    AND COALESCE(i.invoice_status,'') NOT IN ('DRAFT','REJECTED','CANCELLED')"
            + "   GROUP BY 1"
            + " ) a ON a.d = m.d"
            + " ORDER BY m.d";

        List<Object> params = new ArrayList<>();
        params.add(start);
        params.add(end);
        params.add(AnalyticsSupport.tenantIdText());
        params.add(start);
        params.add(end);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", text(r.get("year")));
            m.put("month", text(r.get("month")));
            m.put("periodLabel", text(r.get("year")) + "-" + text(r.get("month")));
            BigDecimal salesPos = AnalyticsSupport.toDecimal(r.get("salesPosAmount"));
            BigDecimal salesNeg = AnalyticsSupport.toDecimal(r.get("salesNegAmount"));
            BigDecimal purPos = AnalyticsSupport.toDecimal(r.get("purPosAmount"));
            BigDecimal purNeg = AnalyticsSupport.toDecimal(r.get("purNegAmount"));
            BigDecimal salesTax = AnalyticsSupport.toDecimal(r.get("salesTaxAmount"));
            BigDecimal purTax = AnalyticsSupport.toDecimal(r.get("purTaxAmount"));
            m.put("salesPosCount", AnalyticsSupport.toDecimal(r.get("salesPosCount")));
            m.put("salesPosAmount", AnalyticsSupport.round(salesPos, 2));
            m.put("salesNegCount", AnalyticsSupport.toDecimal(r.get("salesNegCount")));
            m.put("salesNegAmount", AnalyticsSupport.round(salesNeg, 2));
            m.put("salesNetAmount", AnalyticsSupport.round(salesPos.subtract(salesNeg), 2));
            m.put("salesTotalAmount", AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("salesTotalAmount")), 2));
            m.put("salesTaxAmount", AnalyticsSupport.round(salesTax, 2));
            m.put("purPosCount", AnalyticsSupport.toDecimal(r.get("purPosCount")));
            m.put("purPosAmount", AnalyticsSupport.round(purPos, 2));
            m.put("purNegCount", AnalyticsSupport.toDecimal(r.get("purNegCount")));
            m.put("purNegAmount", AnalyticsSupport.round(purNeg, 2));
            m.put("purNetAmount", AnalyticsSupport.round(purPos.subtract(purNeg), 2));
            m.put("purTotalAmount", AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("purTotalAmount")), 2));
            m.put("purTaxAmount", AnalyticsSupport.round(purTax, 2));
            m.put("taxPayable", AnalyticsSupport.round(salesTax.subtract(purTax), 2));

            if (Boolean.TRUE.equals(q.getHideEmpty()) && isAllZero(m)) {
                continue;
            }
            rows.add(m);
        }

        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("year", null);
        summary.put("month", null);
        summary.put("periodLabel", null);
        // 合计行的净额/应交税额按口径重算（避免 SUM 已派生列导致的重复口径）
        BigDecimal sPos = AnalyticsSupport.toDecimal(summary.get("salesPosAmount"));
        BigDecimal sNeg = AnalyticsSupport.toDecimal(summary.get("salesNegAmount"));
        BigDecimal pPos = AnalyticsSupport.toDecimal(summary.get("purPosAmount"));
        BigDecimal pNeg = AnalyticsSupport.toDecimal(summary.get("purNegAmount"));
        BigDecimal sTax = AnalyticsSupport.toDecimal(summary.get("salesTaxAmount"));
        BigDecimal pTax = AnalyticsSupport.toDecimal(summary.get("purTaxAmount"));
        summary.put("salesNetAmount", AnalyticsSupport.round(sPos.subtract(sNeg), 2));
        summary.put("purNetAmount", AnalyticsSupport.round(pPos.subtract(pNeg), 2));
        summary.put("taxPayable", AnalyticsSupport.round(sTax.subtract(pTax), 2));
        out.put("summary", summary);
        return out;
    }

    private static boolean isAllZero(Map<String, Object> m) {
        for (String k : List.of("salesPosCount", "salesPosAmount", "salesNegCount", "salesNegAmount",
            "salesNetAmount", "salesTotalAmount", "salesTaxAmount", "purPosCount", "purPosAmount",
            "purNegCount", "purNegAmount", "purNetAmount", "purTotalAmount", "purTaxAmount", "taxPayable")) {
            if (AnalyticsSupport.toDecimal(m.get(k)).signum() != 0) {
                return false;
            }
        }
        return true;
    }

    private static String text(Object v) {
        return v == null ? "" : v.toString();
    }

    /** 解析 YYYY-MM；非法值回落到默认 */
    private static YearMonth parseMonth(String raw, YearMonth fallback) {
        if (!AnalyticsSupport.hasText(raw)) {
            return fallback;
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
