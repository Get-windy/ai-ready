package cn.aiedge.erp.finance.analytics.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
import cn.aiedge.erp.finance.analytics.service.PartnerLedgerService;
import cn.aiedge.erp.finance.analytics.support.AnalyticsSupport;
import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.service.BusinessAccountingService;
import cn.aiedge.erp.finance.service.PayableService;
import cn.aiedge.erp.finance.service.ReceivableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 往来余额表（菜单 80459）· 四象限滚动实现。
 *
 * <p><b>四象限</b>：应收（finance_receivable）/ 预收（erp_pre_receipt）/ 应付（finance_payable）/ 预付（erp_pre_payment），
 * 每个象限取「期初 / 本期发生 / 期末」三列，末组「往来合计」为净额。</p>
 *
 * <p><b>口径（本页成立且必须在下游保持一致）</b>：</p>
 * <ul>
 *   <li>期末余额 = 该象限「单据日期 ≤ 区间止」的未结余额累计</li>
 *   <li>期初余额 = 该象限「单据日期 &lt; 区间起」的未结余额累计</li>
 *   <li>本期发生 = 期末 − 期初（区间内净发生额：新增 − 收付 − 调整），据此恒满足对标实测的滚动恒等式
 *       {@code 期末 = 期初 + 本期}；本系统「应收/应付/预收/预付」四表均<b>无付款日期列</b>，
 *       无法把「本期新增」与「本期收付」拆开，故如实以净发生额呈现（见开发文档缺口登记）。</li>
 *   <li>往来合计 = （应收 + 预付） − （预收 + 应付）；正 = 对方欠我，负 = 我欠对方</li>
 * </ul>
 *
 * <p><b>清账</b>：应收与应付对冲，经会计凭证（KJPZ-）生成分录 {@code Dr 应付账款 / Cr 应收账款}，
 * 并同步写应收（负）与应付（负）余额；一律不直改余额列。</p>
 *
 * <p><b>多租户</b>：全部 JdbcTemplate 手写 SQL，每条均显式带 {@code tenant_id = ?}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerLedgerServiceImpl implements PartnerLedgerService {

    private static final String RECONCILE_SOURCE = "partner_reconcile";
    private static final String SUBJECT_AR = "1122";
    private static final String SUBJECT_AP = "2202";

    private final JdbcTemplate jdbcTemplate;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final BusinessAccountingService businessAccountingService;

    /** 一个象限的聚合中间态 */
    private static final class Quad {
        String code;
        BigDecimal begin = BigDecimal.ZERO;
        BigDecimal end = BigDecimal.ZERO;
    }

    // ════════════════════════════════════════════════════════════════
    //  分页
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> page(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        String start = AnalyticsSupport.hasText(q.getStartDate()) ? q.getStartDate().trim()
            : LocalDate.now().withDayOfMonth(1).toString();
        String end = AnalyticsSupport.hasText(q.getEndDate()) ? q.getEndDate().trim() : LocalDate.now().toString();

        Map<String, Quad> ar = quadrant("finance_receivable", "customer_name", "remaining_amount",
            "COALESCE(invoice_date, created_at::date)", "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')", start, end);
        Map<String, Quad> preReceipt = quadrant("erp_pre_receipt", "customer_name", "remaining_amount",
            "receipt_date", "deleted = 0 AND status IS DISTINCT FROM 'forfeited' AND status IS DISTINCT FROM 'refunded'", start, end);
        Map<String, Quad> ap = quadrant("finance_payable", "supplier_name", "remaining_amount",
            "COALESCE(invoice_date, created_at::date)", "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')", start, end);
        Map<String, Quad> prePay = quadrant("erp_pre_payment", "supplier_name", "remaining_amount",
            "payment_date", "deleted = 0", start, end);

        Map<String, String> names = new LinkedHashMap<>();
        List.of(ar, preReceipt, ap, prePay).forEach(m -> m.keySet().forEach(k -> names.putIfAbsent(k, k)));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String name : names.keySet()) {
            if (AnalyticsSupport.hasText(q.getPartnerName())
                && !name.toLowerCase().contains(q.getPartnerName().trim().toLowerCase())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partnerName", name);
            m.put("partnerCode", partnerCode(name));
            putQuad(m, "ar", ar.get(name));
            putQuad(m, "preReceipt", preReceipt.get(name));
            putQuad(m, "ap", ap.get(name));
            putQuad(m, "prePay", prePay.get(name));

            BigDecimal netBegin = num(m, "arBegin").add(num(m, "prePayBegin"))
                .subtract(num(m, "preReceiptBegin")).subtract(num(m, "apBegin"));
            BigDecimal netPeriod = num(m, "arPeriod").add(num(m, "prePayPeriod"))
                .subtract(num(m, "preReceiptPeriod")).subtract(num(m, "apPeriod"));
            BigDecimal netEnd = num(m, "arEnd").add(num(m, "prePayEnd"))
                .subtract(num(m, "preReceiptEnd")).subtract(num(m, "apEnd"));
            m.put("netBegin", AnalyticsSupport.round(netBegin, 2));
            m.put("netPeriod", AnalyticsSupport.round(netPeriod, 2));
            m.put("netEnd", AnalyticsSupport.round(netEnd, 2));

            boolean hasAr = ar.containsKey(name);
            boolean hasAp = ap.containsKey(name);
            m.put("bothRoles", hasAr && hasAp);
            m.put("partnerTypeText", hasAr && hasAp ? "客户/供应商" : (hasAr ? "客户" : "供应商"));

            // 「显示本期金额为0的数据」未勾选时：四象限本期与期末全为 0 的行不显示
            if (!Boolean.TRUE.equals(q.getShowZero()) && isAllZero(m)) {
                continue;
            }
            if (Boolean.TRUE.equals(q.getOnlyBoth()) && !(hasAr && hasAp)) {
                continue;
            }
            rows.add(m);
        }

        AnalyticsSupport.sortDesc(rows, "netEnd");
        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("partnerName", null);
        summary.put("partnerCode", null);
        summary.put("partnerTypeText", null);
        summary.put("bothRoles", null);
        out.put("summary", summary);
        return out;
    }

    private static void putQuad(Map<String, Object> m, String prefix, Quad quad) {
        BigDecimal begin = quad == null ? BigDecimal.ZERO : quad.begin;
        BigDecimal end = quad == null ? BigDecimal.ZERO : quad.end;
        m.put(prefix + "Begin", AnalyticsSupport.round(begin, 2));
        m.put(prefix + "Period", AnalyticsSupport.round(end.subtract(begin), 2));
        m.put(prefix + "End", AnalyticsSupport.round(end, 2));
    }

    private static BigDecimal num(Map<String, Object> m, String key) {
        return AnalyticsSupport.toDecimal(m.get(key));
    }

    private static boolean isAllZero(Map<String, Object> m) {
        for (String p : List.of("ar", "preReceipt", "ap", "prePay")) {
            if (num(m, p + "Begin").signum() != 0 || num(m, p + "Period").signum() != 0
                || num(m, p + "End").signum() != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 单象限聚合：按往来单位名称归集 期初（单据日 &lt; 区间起）/ 期末（单据日 ≤ 区间止）未结余额。
     */
    private Map<String, Quad> quadrant(String table, String nameCol, String balanceCol, String dateExpr,
                                       String extraWhere, String start, String end) {
        String nameExpr = "COALESCE(NULLIF(" + nameCol + ",''), '未指定')";
        String sql = "SELECT " + nameExpr + " AS \"k\","
            + " COALESCE(SUM(CASE WHEN " + dateExpr + " < ?::date THEN COALESCE(" + balanceCol + ",0) ELSE 0 END),0) AS \"b\","
            + " COALESCE(SUM(CASE WHEN " + dateExpr + " <= ?::date THEN COALESCE(" + balanceCol + ",0) ELSE 0 END),0) AS \"e\""
            + " FROM " + table
            + " WHERE tenant_id = ? AND " + extraWhere
            + " GROUP BY 1";
        List<Object> params = new ArrayList<>();
        params.add(start);
        params.add(end);
        params.add(AnalyticsSupport.tenantId());
        Map<String, Quad> out = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(sql, params.toArray())) {
            Object k = r.get("k");
            if (k == null || k.toString().isBlank()) {
                continue;
            }
            Quad quad = new Quad();
            quad.begin = AnalyticsSupport.toDecimal(r.get("b"));
            quad.end = AnalyticsSupport.toDecimal(r.get("e"));
            out.put(k.toString(), quad);
        }
        return out;
    }

    /** 结算单位编号：按名称回往来单位主数据（biz_party） */
    private String partnerCode(String name) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT party_code FROM biz_party WHERE deleted = 0 AND tenant_id = ? AND party_name = ? ORDER BY id LIMIT 1",
            AnalyticsSupport.tenantId(), name);
        if (rows.isEmpty()) {
            return null;
        }
        Object code = rows.get(0).get("party_code");
        return code == null ? null : code.toString();
    }

    private Long partnerId(String name) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM biz_party WHERE deleted = 0 AND tenant_id = ? AND party_name = ? ORDER BY id LIMIT 1",
            AnalyticsSupport.tenantId(), name);
        if (rows.isEmpty()) {
            return null;
        }
        Object id = rows.get(0).get("id");
        return id instanceof Number n ? n.longValue() : AnalyticsSupport.numericId(String.valueOf(id));
    }

    // ════════════════════════════════════════════════════════════════
    //  行级「对账」
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        String name = q.getPartnerName();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("receivables", sourceRows("finance_receivable", "customer_name", "remaining_amount",
            "COALESCE(invoice_date, created_at::date)", "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')",
            "source_no", name, q));
        out.put("preReceipts", sourceRows("erp_pre_receipt", "customer_name", "remaining_amount",
            "receipt_date", "deleted = 0 AND status IS DISTINCT FROM 'forfeited' AND status IS DISTINCT FROM 'refunded'",
            "pre_receipt_no", name, q));
        out.put("payables", sourceRows("finance_payable", "supplier_name", "remaining_amount",
            "COALESCE(invoice_date, created_at::date)", "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')",
            "source_no", name, q));
        out.put("prePayments", sourceRows("erp_pre_payment", "supplier_name", "remaining_amount",
            "payment_date", "deleted = 0", "pre_payment_no", name, q));
        return out;
    }

    private List<Map<String, Object>> sourceRows(String table, String nameCol, String balanceCol,
                                                 String dateExpr, String extraWhere, String noCol,
                                                 String name, AnalyticsQuery q) {
        if (!AnalyticsSupport.hasText(name)) {
            return List.of();
        }
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.add(name);
        StringBuilder sql = new StringBuilder("SELECT " + noCol + " AS \"docNo\","
            + " to_char(" + dateExpr + ",'YYYY-MM-DD') AS \"bizDate\","
            + " COALESCE(" + balanceCol + ",0) AS \"balance\","
            + " " + dateExpr + " AS \"bizDateRaw\""
            + " FROM " + table + " WHERE tenant_id = ? AND " + nameCol + " = ? AND " + extraWhere);
        if (AnalyticsSupport.hasText(q.getStartDate())) {
            sql.append(" AND ").append(dateExpr).append(" >= ?::date");
            params.add(q.getStartDate().trim());
        }
        if (AnalyticsSupport.hasText(q.getEndDate())) {
            sql.append(" AND ").append(dateExpr).append(" <= ?::date");
            params.add(q.getEndDate().trim());
        }
        sql.append(" ORDER BY ").append(dateExpr).append(" DESC LIMIT 300");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    // ════════════════════════════════════════════════════════════════
    //  行级「清账」
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reconcile(String partnerName, BigDecimal amount, String remark) {
        if (!AnalyticsSupport.hasText(partnerName)) {
            throw BusinessException.badRequest("请先选择结算单位");
        }
        BigDecimal amt = AnalyticsSupport.round(amount, 2);
        if (amt.signum() <= 0) {
            throw BusinessException.badRequest("清账金额必须大于 0");
        }
        BigDecimal arBalance = currentBalance("finance_receivable", "customer_name", "remaining_amount",
            "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')", partnerName);
        BigDecimal apBalance = currentBalance("finance_payable", "supplier_name", "remaining_amount",
            "deleted_flag = 0 AND status NOT IN ('written_off','bad_debt')", partnerName);
        if (arBalance.signum() <= 0 || apBalance.signum() <= 0) {
            throw BusinessException.badRequest("只支持对应收、应付账款清账：该单位需同时存在应收余额与应付余额");
        }
        if (amt.compareTo(arBalance.min(apBalance.abs())) > 0) {
            throw BusinessException.badRequest("清账金额不能超过该单位的应收余额与应付余额的较小值");
        }
        Long partnerId = partnerId(partnerName);
        String docNo = nextReconcileNo();
        LocalDate today = LocalDate.now();

        // 凭证：Dr 应付账款 / Cr 应收账款（两方对冲，借贷平衡）
        BusinessAccountingRequest req = new BusinessAccountingRequest();
        req.setSourceType(RECONCILE_SOURCE);
        req.setSourceNo(docNo);
        req.setAmount(amt);
        req.setSummary("往来清账 - " + partnerName + "（" + docNo + "）");
        req.setVoucherDate(today);
        List<BusinessAccountingRequest.AccountingRequestItem> items = new ArrayList<>();
        items.add(voucherItem("往来清账-" + partnerName, SUBJECT_AP, amt, BigDecimal.ZERO, partnerName));
        items.add(voucherItem("往来清账-" + partnerName, SUBJECT_AR, BigDecimal.ZERO, amt, partnerName));
        req.setItems(items);
        businessAccountingService.createVoucherFromBusiness(req);

        // 应收减少
        ReceivableDTO ar = new ReceivableDTO();
        ar.setSourceType(RECONCILE_SOURCE);
        ar.setSourceNo(docNo);
        ar.setCustomerId(partnerId == null ? null : String.valueOf(partnerId));
        ar.setCustomerName(partnerName);
        ar.setTotalAmount(amt.negate());
        ar.setRemainingAmount(amt.negate());
        ar.setPaidAmount(BigDecimal.ZERO);
        ar.setStatus("normal");
        ar.setInvoiceDate(today);
        ar.setRemark(remark);
        receivableService.create(ar);

        // 应付减少
        PayableDTO ap = new PayableDTO();
        ap.setSourceType(RECONCILE_SOURCE);
        ap.setSourceNo(docNo);
        ap.setSupplierId(partnerId == null ? null : String.valueOf(partnerId));
        ap.setSupplierName(partnerName);
        ap.setTotalAmount(amt.negate());
        ap.setRemainingAmount(amt.negate());
        ap.setPaidAmount(BigDecimal.ZERO);
        ap.setStatus("normal");
        ap.setInvoiceDate(today);
        ap.setRemark(remark);
        payableService.create(ap);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("docNo", docNo);
        out.put("partnerName", partnerName);
        out.put("amount", amt);
        out.put("docDate", today.toString());
        log.info("[往来余额表] 清账 {} {} 金额 {}", docNo, partnerName, amt);
        return out;
    }

    private BusinessAccountingRequest.AccountingRequestItem voucherItem(String summary, String subjectCode,
                                                                       BigDecimal debit, BigDecimal credit,
                                                                       String auxUnit) {
        BusinessAccountingRequest.AccountingRequestItem item = new BusinessAccountingRequest.AccountingRequestItem();
        item.setSummary(summary);
        item.setSubjectCode(subjectCode);
        item.setDebitAmount(debit);
        item.setCreditAmount(credit);
        item.setAuxUnit(auxUnit);
        return item;
    }

    private BigDecimal currentBalance(String table, String nameCol, String balanceCol, String extraWhere, String name) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT COALESCE(SUM(COALESCE(" + balanceCol + ",0)),0) AS \"b\" FROM " + table
                + " WHERE tenant_id = ? AND " + nameCol + " = ? AND " + extraWhere,
            AnalyticsSupport.tenantId(), name);
        return rows.isEmpty() ? BigDecimal.ZERO : AnalyticsSupport.toDecimal(rows.get(0).get("b"));
    }

    /** 清账单号：QZ-YYYYMMDD-序号（序号取当日已存在的最大号 +1） */
    private String nextReconcileNo() {
        String prefix = "QZ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT MAX(source_no) AS \"m\" FROM finance_receivable"
                + " WHERE tenant_id = ? AND source_type = ? AND source_no LIKE ?",
            AnalyticsSupport.tenantId(), RECONCILE_SOURCE, prefix + "%");
        int seq = 1;
        if (!rows.isEmpty() && rows.get(0).get("m") != null) {
            String last = rows.get(0).get("m").toString();
            try {
                seq = Integer.parseInt(last.substring(last.lastIndexOf('-') + 1)) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + "-" + String.format("%03d", seq);
    }

    // ════════════════════════════════════════════════════════════════
    //  工具栏「清账历史」
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> reconcileHistory(Integer page, Integer size) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT source_no AS \"docNo\", to_char(created_at,'YYYY-MM-DD') AS \"bizDate\","
                + " customer_name AS \"partnerName\", ABS(COALESCE(remaining_amount,0)) AS \"amount\""
                + " FROM finance_receivable WHERE tenant_id = ? AND source_type = ?"
                + " ORDER BY created_at DESC, id DESC LIMIT 500",
            AnalyticsSupport.tenantId(), RECONCILE_SOURCE);
        Map<String, Object> out = AnalyticsSupport.pageResult(rows,
            page == null ? 1 : page, size == null ? 20 : size);
        out.put("summary", AnalyticsSupport.sumAll(rows));
        return out;
    }
}
