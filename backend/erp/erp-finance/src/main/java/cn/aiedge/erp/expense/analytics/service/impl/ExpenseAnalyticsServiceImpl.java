package cn.aiedge.erp.expense.analytics.service.impl;

import cn.aiedge.erp.expense.analytics.service.ExpenseAnalyticsService;
import cn.aiedge.erp.finance.analytics.dto.AnalyticsQuery;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查费用（菜单 80454）· 四视图账表实现。
 *
 * <p><b>数据源</b>：费用单 {@code erp_expense_doc} ⨝ 费用明细 {@code erp_expense_item}，
 * 科目名称回会计科目主数据 {@code finance_account_subject}（明细 subject_name 为空时回落到费用类型名）。</p>
 *
 * <p><b>行维度 = 费用科目</b>（对标「费用名称 = 科目名、费用编号 = 科目编码 6601/6602/6603/6711」），
 * 而非费用类型枚举；科目编码取 {@code item.subject_code}，缺失时回落到 {@code item.expense_code}。</p>
 *
 * <p><b>状态口径</b>：排除已取消（{@code status = 2}）；红冲单（{@code red_flag = 1}）
 * 由查询项「包含红冲」控制——默认<b>不并入</b>（与《分析模块·金标准开发指南》第六节「红冲默认不入列」一致），
 * 勾选后并入（负数冲减）。</p>
 *
 * <p><b>矩阵列</b>：按部门 / 按职员两 Tab 的部门列与职员列由主数据动态生成，
 * 与对标一致——<b>不进列配置弹窗</b>，仅 费用名称 / 费用编号 / 费用金额 三列可配置。</p>
 *
 * <p><b>多租户</b>：全部 JdbcTemplate 手写 SQL，每条均显式带 {@code tenant_id = ?}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseAnalyticsServiceImpl implements ExpenseAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /** 费用单 + 明细 + 科目名的公共 FROM（别名固定：d=单据头，i=明细，s=科目主数据） */
    private static final String FROM =
        " FROM erp_expense_doc d"
        + " JOIN erp_expense_item i ON i.expense_doc_id = d.id AND i.deleted = 0 AND i.tenant_id = d.tenant_id"
        + " LEFT JOIN finance_account_subject s ON s.subject_code = i.subject_code";

    /** 科目编号：科目编码优先，缺失回落费用类型编码 */
    private static final String SUBJECT_CODE = "COALESCE(NULLIF(i.subject_code,''), NULLIF(i.expense_code,''), '未指定')";
    /** 科目名称：科目主数据优先，其次明细科目名、费用类型名 */
    private static final String SUBJECT_NAME =
        "COALESCE(NULLIF(s.subject_name,''), NULLIF(i.subject_name,''), NULLIF(i.expense_name,''), '未指定')";

    /** 公共过滤：日期区间 + 已取消剔除 + 红冲开关 + 科目/部门/往来单位/关键字 */
    private SqlWhere commonFilter(AnalyticsQuery q) {
        SqlWhere w = new SqlWhere();
        w.dateFrom("d.doc_date", q.getStartDate());
        w.dateTo("d.doc_date", q.getEndDate());
        w.raw("COALESCE(d.status, 0) <> 2");
        if (!Boolean.TRUE.equals(q.getIncludeReversed())) {
            w.raw("COALESCE(d.red_flag,0) = 0");
        }
        w.like(SUBJECT_CODE, q.getKeyword());
        w.like("d.dept_name", q.getDeptName());
        w.like("d.partner_name", q.getPartnerName());
        return w;
    }

    // ════════════════════════════════════════════════════════════════
    //  按部门 / 按职员（矩阵）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> matrix(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        boolean staff = "staff".equalsIgnoreCase(q.getTab());
        String dimExpr = staff
            ? "COALESCE(NULLIF(d.handler_name,''), '未指定')"
            : "COALESCE(NULLIF(d.dept_name,''), '未指定')";

        // 1) 行维度：科目 → 金额合计
        SqlWhere w = commonFilter(q);
        String rowSql = "SELECT " + SUBJECT_CODE + " AS \"subjectCode\","
            + " MAX(" + SUBJECT_NAME + ") AS \"subjectName\","
            + " COALESCE(SUM(COALESCE(i.amount,0)),0) AS \"amount\""
            + FROM + " WHERE d.deleted = 0 AND d.tenant_id = ?" + w.sql()
            + " GROUP BY 1 ORDER BY 1";
        List<Object> rowParams = new ArrayList<>();
        rowParams.add(AnalyticsSupport.tenantId());
        rowParams.addAll(w.list());

        Map<String, Map<String, Object>> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(rowSql, rowParams.toArray())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectCode", r.get("subjectCode"));
            m.put("subjectName", r.get("subjectName"));
            m.put("amount", AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("amount")), 2));
            m.put("cells", new LinkedHashMap<String, Object>());
            rowMap.put(String.valueOf(r.get("subjectCode")), m);
        }

        // 2) 矩阵：科目 × 部门/职员 → 分摊额
        SqlWhere w2 = commonFilter(q);
        String cellSql = "SELECT " + SUBJECT_CODE + " AS \"subjectCode\","
            + " " + dimExpr + " AS \"dimName\","
            + " COALESCE(SUM(COALESCE(i.amount,0)),0) AS \"amount\""
            + FROM + " WHERE d.deleted = 0 AND d.tenant_id = ?" + w2.sql()
            + " GROUP BY 1,2 ORDER BY 2,1";
        List<Object> cellParams = new ArrayList<>();
        cellParams.add(AnalyticsSupport.tenantId());
        cellParams.addAll(w2.list());
        Set<String> dims = new LinkedHashSet<>();
        for (Map<String, Object> r : jdbcTemplate.queryForList(cellSql, cellParams.toArray())) {
            String dim = r.get("dimName") == null ? "未指定" : r.get("dimName").toString();
            dims.add(dim);
            Map<String, Object> row = rowMap.get(String.valueOf(r.get("subjectCode")));
            if (row == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> cells = (Map<String, Object>) row.get("cells");
            cells.put(dim, AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("amount")), 2));
        }

        List<Map<String, Object>> rows = new ArrayList<>(rowMap.values());
        AnalyticsSupport.sortDesc(rows, "amount");
        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        // 矩阵列顺序 = 主数据动态生成（按名称升序稳定输出，便于前端逐列渲染）
        List<String> matrixColumns = new ArrayList<>(dims);
        out.put("matrixColumns", matrixColumns);
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("subjectCode", null);
        summary.put("subjectName", null);
        summary.put("cells", null);
        out.put("summary", summary);
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  按明细（15 列）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> detail(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        SqlWhere w = commonFilter(q);
        String sql = "SELECT to_char(d.doc_date,'YYYY-MM-DD') AS \"bizDate\","
            + " d.doc_no AS \"docNo\","
            + " COALESCE(NULLIF(i.expense_name,''), NULLIF(i.subject_name,''), '费用单') AS \"docType\","
            + " COALESCE(d.partner_name,'') AS \"partnerName\","
            + " COALESCE(d.partner_code,'') AS \"partnerCode\","
            + " " + SUBJECT_NAME + " AS \"subjectName\","
            + " " + SUBJECT_CODE + " AS \"subjectCode\","
            + " COALESCE(i.amount,0) AS \"amount\","
            + " COALESCE(d.handler_name,'') AS \"handlerName\","
            + " COALESCE(d.dept_name,'') AS \"deptName\","
            + " COALESCE(d.creator_name,'') AS \"creatorName\","
            + " COALESCE(d.summary,'') AS \"docSummary\","
            + " COALESCE(d.remark,'') AS \"docRemark\","
            + " COALESCE(i.remark,'') AS \"itemRemark\","
            + " to_char(d.bookkeeping_time,'YYYY-MM-DD HH24:MI:SS') AS \"bookkeepingTime\""
            + FROM + " WHERE d.deleted = 0 AND d.tenant_id = ?" + w.sql()
            + " ORDER BY d.doc_date DESC, d.id DESC, i.line_no";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        rows.forEach(r -> r.put("amount", AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("amount")), 2)));

        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("bizDate", null);
        summary.put("docNo", null);
        out.put("summary", summary);
        return out;
    }

    // ════════════════════════════════════════════════════════════════
    //  按往来单位（4 列：单位编号 / 单位名称 / 费用金额 / 占比(%)）
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> partner(AnalyticsQuery query) {
        AnalyticsQuery q = query == null ? new AnalyticsQuery() : query;
        SqlWhere w = commonFilter(q);
        String sql = "SELECT COALESCE(NULLIF(MAX(d.partner_code),''), '') AS \"partnerCode\","
            + " COALESCE(NULLIF(d.partner_name,''), '未指定') AS \"partnerName\","
            + " COALESCE(SUM(COALESCE(i.amount,0)),0) AS \"amount\""
            + FROM + " WHERE d.deleted = 0 AND d.tenant_id = ?" + w.sql()
            + " GROUP BY COALESCE(NULLIF(d.partner_name,''), '未指定')"
            + " ORDER BY 3 DESC";
        List<Object> params = new ArrayList<>();
        params.add(AnalyticsSupport.tenantId());
        params.addAll(w.list());
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal grand = BigDecimal.ZERO;
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partnerCode", r.get("partnerCode"));
            m.put("partnerName", r.get("partnerName"));
            BigDecimal amt = AnalyticsSupport.round(AnalyticsSupport.toDecimal(r.get("amount")), 2);
            m.put("amount", amt);
            grand = grand.add(amt);
            rows.add(m);
        }
        for (Map<String, Object> m : rows) {
            m.put("ratio", AnalyticsSupport.ratio(AnalyticsSupport.toDecimal(m.get("amount")), grand, 2));
        }

        Map<String, Object> out = AnalyticsSupport.pageResult(rows, q.getPage(), q.getSize());
        Map<String, Object> summary = AnalyticsSupport.sumAll(rows);
        summary.put("partnerCode", null);
        summary.put("partnerName", null);
        summary.put("ratio", AnalyticsSupport.ratio(grand, grand, 2));
        out.put("summary", summary);
        return out;
    }
}
