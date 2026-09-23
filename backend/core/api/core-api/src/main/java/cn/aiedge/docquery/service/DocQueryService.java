package cn.aiedge.docquery.service;

import cn.aiedge.docquery.dto.DocQueryParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合单据查询服务（分析 &gt; 综合单据）
 *
 * <p>跨单据 UNION ALL 聚合查询，覆盖：销售订单/销售出库/销售退货/销售预订单、
 * 采购订单/采购入库/采购退货、收款单/付款单、调拨单/报损单/报溢单、费用申请单。</p>
 *
 * <p>状态码约定（逐表核实自实体注释/状态枚举/服务流转，详见各分支 CASE）：</p>
 * <ul>
 *   <li>所有 MyBatis-Plus 单据表：0=草稿，1=待审批(待审核/审核中)</li>
 *   <li>expense_application（JPA 表，无 tenant_id/deleted 列）：
 *       DRAFT=草稿，SUBMITTED/DEPARTMENT_APPROVING/FINANCE_APPROVING/GENERAL_MANAGER_APPROVING=待审批</li>
 * </ul>
 *
 * <p>列口径（对标 ql361 列配置弹窗实测）：每分支统一输出 23 列，顺序为</p>
 * <pre>
 *   doc_no, biz_date, partner_name, amount, status, status_text, create_by,
 *   id, source_order_no, warehouse_name, region, handler_name, department_name, creator_name,
 *   bookkeeper_name, summary, remark, has_attachment, doc_create_time, bookkeeping_time,
 *   print_count, account_period_days, is_cancelled
 * </pre>
 * <p>其中 <b>业务日期 biz_date 取单据自身的单据日期列</b>（如 order_date/outbound_date/receipt_date），
 * 缺列时回退 create_time —— 早期实现一律取 create_time，导致「单据日期」列语义为制单时间，已订正。</p>
 *
 * <p>取舍说明：</p>
 * <ul>
 *   <li>erp_stock_transfer 无往来单位列，partner_name 取「调出仓→调入仓」拼接</li>
 *   <li>erp_stock_damage / erp_stock_overflow 无往来单位列，partner_name 取仓库名</li>
 *   <li>expense_application 无往来单位/create_by 列，partner_name 取申请人，create_by 返回 NULL；
 *       该表无 tenant_id/deleted 列，不参与租户与删除标记过滤</li>
 *   <li>各分支缺某列时统一以 NULL 占位（如收款/付款单无所属区域、仓库、摘要、附件），
 *       不伪造数据 —— 页面该列对这些单据类型显示为空</li>
 *   <li>账期天数 = 收/付款截止日 − 单据日期，仅销售订单/销售出库/销售预订单/采购订单/采购退货/费用申请单
 *       有对应列，其余类型为 NULL</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocQueryService {

    private final JdbcTemplate jdbcTemplate;

    /** 单据分支定义 */
    private record DocBranch(String code, String name, String select, String extra, String baseWhere,
                             boolean hasTenant, String pendingCond, String draftCond) {
    }

    /** 状态过滤模式 */
    public enum StatusMode {ALL, PENDING, DRAFT}

    /** 附件标记表达式（attachment 列在各表类型不一：text / varchar / integer） */
    private static final String HAS_ATTACHMENT =
        "CASE WHEN attachment IS NOT NULL AND attachment::text <> '' AND attachment::text <> '0' "
        + "THEN true ELSE false END";

    private static final List<DocBranch> BRANCHES = List.of(
        // ⚠️ 首分支的列别名即 UNION 结果集的列名（后续分支按位置对齐），故此处必须逐个显式 AS
        new DocBranch("SALE_ORDER", "销售订单",
            "SELECT order_no AS doc_no, COALESCE(order_date, create_time) AS biz_date, "
            + "customer_name AS partner_name, total_amount AS amount, status::text AS status, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审核' WHEN 2 THEN '待发货' WHEN 3 THEN '部分发货' "
            + "WHEN 4 THEN '发货完成' WHEN 5 THEN '交易完成' WHEN 6 THEN '已取消' ELSE '未知' END AS status_text, "
            + "create_by",
            "SELECT id, NULL::varchar AS source_order_no, warehouse_name, region, "
            + "salesman_name AS handler_name, dept_name AS department_name, creator_name, "
            + "NULL::varchar AS bookkeeper_name, summary, remark, " + HAS_ATTACHMENT
            + " AS has_attachment, create_time AS doc_create_time, bookkeeping_time, print_count, "
            + "CASE WHEN payment_date IS NOT NULL THEN (payment_date - order_date::date) ELSE NULL END AS account_period_days, "
            + "(status::text = '6') AS is_cancelled FROM erp_sale_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_OUTBOUND", "销售出库单",
            "SELECT outbound_no, COALESCE(outbound_date::timestamp, create_time), customer_name, "
            + "total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 11 THEN '已完成' WHEN 12 THEN '已取消' ELSE '处理中' END, create_by",
            "SELECT id, order_no, warehouse_name, region, sales_person_name, department_name, "
            + "creator_name, bookkeeper_name, summary, remark, NULL::boolean, create_time, "
            + "bookkeeping_time, print_count, "
            + "CASE WHEN payment_date IS NOT NULL THEN (payment_date - outbound_date) ELSE NULL END, "
            + "(status::text = '12') FROM erp_sale_outbound",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_RETURN", "销售退货单",
            "SELECT return_no, COALESCE(order_date, create_time), customer_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 3 THEN '已完成' WHEN 4 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, sale_order_no, warehouse_name, region, handler_name, dept_name, creator_name, "
            + "NULL::varchar, summary, remark, " + HAS_ATTACHMENT + ", create_time, bookkeeping_time, "
            + "print_count, NULL::integer, (status::text = '4') FROM erp_sale_return",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_PRE_ORDER", "销售预订单",
            "SELECT order_no, COALESCE(order_date::timestamp, create_time), customer_name, total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '审核中' WHEN 2 THEN '待订货' WHEN 3 THEN '部分订货' "
            + "WHEN 4 THEN '已订货' WHEN 5 THEN '已完成' WHEN -1 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, NULL::varchar, warehouse_name, region, handler_name, dept_name, creator_name, "
            + "NULL::varchar, summary, remark, " + HAS_ATTACHMENT + ", create_time, NULL::timestamp, "
            + "print_count, "
            + "CASE WHEN deposit_deadline IS NOT NULL THEN (deposit_deadline - order_date) ELSE NULL END, "
            + "(status::text = '-1') FROM erp_sale_pre_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_ORDER", "采购订单",
            "SELECT order_no, COALESCE(order_date, create_time), supplier_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已下达' "
            + "WHEN 4 THEN '已取消' WHEN 5 THEN '履行中' WHEN 6 THEN '已完成' ELSE '未知' END, create_by",
            "SELECT id, source_bill_no, NULL::varchar, NULL::varchar, purchaser_name, NULL::varchar, "
            + "NULL::varchar, NULL::varchar, NULL::varchar, remark, NULL::boolean, create_time, "
            + "NULL::timestamp, NULL::integer, "
            + "CASE WHEN settle_date IS NOT NULL THEN (settle_date::date - order_date::date) ELSE NULL END, "
            + "(status::text = '4') FROM erp_purchase_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_INBOUND", "采购入库单",
            "SELECT inbound_no, COALESCE(inbound_date::timestamp, create_time), supplier_name, total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '待收货' "
            + "WHEN 4 THEN '已收货' WHEN 5 THEN '待质检' WHEN 6 THEN '已质检' WHEN 7 THEN '待入库' "
            + "WHEN 8 THEN '已入库' WHEN 9 THEN '已完成' WHEN 10 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, order_no, warehouse_name, NULL::varchar, purchaser_name, department_name, "
            + "create_by_name, NULL::varchar, summary, remark, " + HAS_ATTACHMENT + ", create_time, "
            + "NULL::timestamp, print_count, NULL::integer, (status::text = '10') FROM erp_purchase_inbound",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_RETURN", "采购退货单",
            "SELECT return_no, COALESCE(return_date::timestamp, create_time), supplier_name, total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 3 THEN '已拒绝' WHEN 4 THEN '已完成' WHEN 5 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, purchase_order_no, warehouse_name, NULL::varchar, purchaser_name, department_name, "
            + "create_by_name, NULL::varchar, summary, remark, " + HAS_ATTACHMENT + ", create_time, "
            + "NULL::timestamp, print_count, "
            + "CASE WHEN payment_deadline IS NOT NULL THEN (payment_deadline - return_date) ELSE NULL END, "
            + "(status::text = '5') FROM erp_purchase_return",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("RECEIPT", "收款单",
            "SELECT receipt_no, COALESCE(receipt_date::timestamp, create_time), customer_name, receipt_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '待核销' WHEN 5 THEN '核销中' WHEN 6 THEN '已核销' WHEN 7 THEN '已完成' "
            + "WHEN 8 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, order_no, NULL::varchar, NULL::varchar, sales_person_name, department_name, "
            + "NULL::varchar, NULL::varchar, NULL::varchar, remark, NULL::boolean, create_time, "
            + "NULL::timestamp, NULL::integer, NULL::integer, (status::text = '8') FROM erp_receipt",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PAYMENT", "付款单",
            "SELECT payment_no, COALESCE(payment_date::timestamp, create_time), supplier_name, payment_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '待核销' WHEN 5 THEN '核销中' WHEN 6 THEN '已核销' WHEN 7 THEN '已完成' "
            + "WHEN 8 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, order_no, NULL::varchar, NULL::varchar, purchaser_name, department_name, "
            + "NULL::varchar, NULL::varchar, NULL::varchar, remark, NULL::boolean, create_time, "
            + "NULL::timestamp, NULL::integer, NULL::integer, (status::text = '8') FROM erp_payment",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_TRANSFER", "调拨单",
            "SELECT transfer_no, COALESCE(bill_date::timestamp, create_time), "
            + "COALESCE(from_warehouse_name,'') || '→' || COALESCE(to_warehouse_name,''), total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '调拨中' WHEN 5 THEN '已完成' WHEN 6 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, source_bill_no, NULL::varchar, NULL::varchar, handler_name, department_name, "
            + "create_by_name, NULL::varchar, summary, remark, " + HAS_ATTACHMENT + ", create_time, "
            + "NULL::timestamp, print_count, NULL::integer, (status::text = '6') FROM erp_stock_transfer",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_DAMAGE", "报损单",
            "SELECT damage_no, COALESCE(damage_date::timestamp, create_time), warehouse_name, total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审核' "
            + "WHEN 3 THEN '已执行' WHEN 4 THEN '已拒绝' WHEN 5 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, NULL::varchar, warehouse_name, NULL::varchar, handler_name, dept_name, "
            + "creator_name, bookkeeper_name, summary, remark, " + HAS_ATTACHMENT + ", create_time, "
            + "bookkeeping_time, print_count, NULL::integer, (status::text = '5') FROM erp_stock_damage",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_OVERFLOW", "报溢单",
            "SELECT overflow_no, COALESCE(overflow_date::timestamp, create_time), warehouse_name, total_amount, "
            + "status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审核' "
            + "WHEN 3 THEN '已执行' WHEN 4 THEN '已拒绝' WHEN 5 THEN '已取消' ELSE '未知' END, create_by",
            "SELECT id, NULL::varchar, warehouse_name, NULL::varchar, handler_name, dept_name, "
            + "creator_name, bookkeeper_name, summary, remark, " + HAS_ATTACHMENT + ", create_time, "
            + "bookkeeping_time, print_count, NULL::integer, (status::text = '5') FROM erp_stock_overflow",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        // ⚠️ 2026-09-23 修复：此处原写「JPA 表：无 tenant_id/deleted/create_by 列」并把 baseWhere 置为 "1=1"、
        //    hasTenant=false —— **断言与库结构不符**：expense_application 实测两列都在
        //    （deleted boolean、tenant_id character varying，见 V11.25.0__Rebuild_Expense_Tables.sql）。
        //    后果是每个租户的「经营历程/待审批单据/业务草稿」都会列出**全部租户**的费用申请单，且已软删的也入列。
        //    注意该表 tenant_id 是 **varchar**（本文件其余 12 张表是 bigint），故比较时需显式 ::text 转换。
        new DocBranch("EXPENSE", "费用申请单",
            "SELECT application_code, apply_date::timestamp, applicant_name, total_amount, status, "
            + "CASE status WHEN 'DRAFT' THEN '草稿' WHEN 'SUBMITTED' THEN '待审批' "
            + "WHEN 'DEPARTMENT_APPROVING' THEN '部门审批中' WHEN 'FINANCE_APPROVING' THEN '财务审批中' "
            + "WHEN 'GENERAL_MANAGER_APPROVING' THEN '总经理审批中' WHEN 'APPROVED' THEN '审批通过' "
            + "WHEN 'REJECTED' THEN '审批拒绝' WHEN 'PAID' THEN '已支付' WHEN 'REIMBURSED' THEN '已报销' "
            + "WHEN 'CANCELLED' THEN '已取消' ELSE status END, NULL::bigint",
            "SELECT id, NULL::varchar, NULL::varchar, NULL::varchar, NULL::varchar, department_name, "
            + "applicant_name, NULL::varchar, NULL::varchar, remark, "
            + "CASE WHEN attachment_count IS NOT NULL AND attachment_count > 0 THEN true ELSE false END, "
            + "created_at, NULL::timestamp, NULL::integer, "
            + "CASE WHEN payment_date IS NOT NULL THEN (payment_date - apply_date) ELSE NULL END, "
            + "(status = 'CANCELLED') FROM expense_application",
            "tenant_id = ?::text AND deleted = false", true,
            "AND status IN ('SUBMITTED','DEPARTMENT_APPROVING','FINANCE_APPROVING','GENERAL_MANAGER_APPROVING')",
            "AND status = 'DRAFT'")
    );

    /**
     * 综合单据分页查询
     *
     * @param tenantId 当前租户ID
     * @param mode     状态过滤：ALL 全部 / PENDING 待审批 / DRAFT 草稿
     * @param p        查询参数（见 {@link DocQueryParams}）
     * @return {list, total, page, size, summary:{amount}}
     */
    public Map<String, Object> page(Long tenantId, StatusMode mode, DocQueryParams p) {
        List<Object> unionParams = new ArrayList<>();
        String unionSql = buildUnion(tenantId, mode, unionParams);

        StringBuilder filter = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();
        if (StringUtils.hasText(p.getDocType())) {
            filter.append(" AND t.doc_type_code = ?");
            filterParams.add(p.getDocType().trim());
        }
        appendLike(filter, filterParams, "doc_no", p.getDocNo());
        appendLike(filter, filterParams, "partner_name", p.getPartnerName());
        appendLike(filter, filterParams, "warehouse_name", p.getWarehouseName());
        appendLike(filter, filterParams, "region", p.getRegion());
        appendLike(filter, filterParams, "handler_name", p.getHandlerName());
        appendLike(filter, filterParams, "department_name", p.getDepartmentName());
        appendLike(filter, filterParams, "creator_name_resolved", p.getCreatorName());
        appendLike(filter, filterParams, "remark", p.getRemark());
        appendLike(filter, filterParams, "source_order_no", p.getSourceOrderNo());
        if (StringUtils.hasText(p.getStartDate())) {
            filter.append(" AND t.biz_date >= CAST(? AS timestamp)");
            filterParams.add(p.getStartDate().trim());
        }
        if (StringUtils.hasText(p.getEndDate())) {
            filter.append(" AND t.biz_date < CAST(? AS timestamp) + INTERVAL '1 day'");
            filterParams.add(p.getEndDate().trim());
        }
        if (p.getAccountPeriodDays() != null) {
            filter.append(" AND t.account_period_days ").append(accountPeriodOperator(p.getAccountPeriodOp())).append(" ?");
            filterParams.add(p.getAccountPeriodDays());
        }
        // 「显示红冲」未勾选时排除已取消单据（对标：被红冲/作废的单据默认不入列）
        if (!Boolean.TRUE.equals(p.getIncludeReversed())) {
            filter.append(" AND t.is_cancelled = false");
        }

        String sourceSql = "WITH docs AS (SELECT t.*, "
            + "COALESCE(NULLIF(t.creator_name, ''), u.real_name, u.nickname, u.username) AS creator_name_resolved "
            + "FROM (" + unionSql + ") t LEFT JOIN sys_user u ON u.id = t.create_by) "
            + "SELECT * FROM docs t WHERE 1=1" + filter;

        // 总数 + 金额合计（合计行口径：对当前过滤范围求和）
        List<Object> aggParams = new ArrayList<>(unionParams);
        aggParams.addAll(filterParams);
        Map<String, Object> agg = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS cnt, COALESCE(SUM(t.amount), 0) AS amt FROM (" + sourceSql + ") t",
            aggParams.toArray());

        // 分页数据（默认按单据日期倒序；金额列可排序）
        int safePage = Math.max(p.getPage(), 1);
        int safeSize = Math.min(Math.max(p.getSize(), 1), 100);
        List<Object> listParams = new ArrayList<>(aggParams);
        listParams.add(safeSize);
        listParams.add((safePage - 1) * safeSize);
        String orderBy = "amount".equalsIgnoreCase(p.getSortField()) ? "t.amount" : "t.biz_date";
        String direction = "asc".equalsIgnoreCase(p.getSortOrder()) ? "ASC" : "DESC";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT t.doc_type_code AS \"docTypeCode\", t.doc_type AS \"docType\", t.id AS \"docId\", "
            + "t.doc_no AS \"docNo\", to_char(t.biz_date, 'YYYY-MM-DD') AS \"bizDate\", "
            + "t.partner_name AS \"partnerName\", t.amount, t.status, t.status_text AS \"statusText\", "
            + "t.create_by AS \"createBy\", t.creator_name_resolved AS \"creatorName\", "
            + "t.source_order_no AS \"sourceOrderNo\", t.warehouse_name AS \"warehouseName\", t.region, "
            + "t.handler_name AS \"handlerName\", t.department_name AS \"departmentName\", "
            + "t.bookkeeper_name AS \"bookkeeperName\", t.summary, t.remark, "
            + "COALESCE(t.has_attachment, false) AS \"hasAttachment\", "
            + "to_char(t.doc_create_time, 'YYYY-MM-DD HH24:MI:SS') AS \"createTime\", "
            + "to_char(t.bookkeeping_time, 'YYYY-MM-DD HH24:MI:SS') AS \"bookkeepingTime\", "
            + "COALESCE(t.print_count, 0) AS \"printCount\", t.account_period_days AS \"accountPeriodDays\" "
            + "FROM (" + sourceSql + ") t ORDER BY " + orderBy + " " + direction + " NULLS LAST, t.doc_no LIMIT ? OFFSET ?",
            listParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", agg.get("cnt") != null ? ((Number) agg.get("cnt")).longValue() : 0L);
        result.put("page", safePage);
        result.put("size", safeSize);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("amount", agg.get("amt") != null ? agg.get("amt") : 0);
        result.put("summary", summary);
        return result;
    }

    /**
     * 待审批单据按类型计数汇总（每类待审批多少张，含 0）
     *
     * @param tenantId 当前租户ID
     * @return [{docTypeCode, docType, count}]
     */
    public List<Map<String, Object>> pendingSummary(Long tenantId) {
        List<Object> unionParams = new ArrayList<>();
        String unionSql = buildUnion(tenantId, StatusMode.PENDING, unionParams);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT t.doc_type_code, t.doc_type, COUNT(*) AS cnt FROM (" + unionSql + ") t "
            + "GROUP BY t.doc_type_code, t.doc_type",
            unionParams.toArray());

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            countMap.put((String) row.get("doc_type_code"), ((Number) row.get("cnt")).longValue());
        }
        List<Map<String, Object>> summary = new ArrayList<>();
        for (DocBranch b : BRANCHES) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("docTypeCode", b.code());
            item.put("docType", b.name());
            item.put("count", countMap.getOrDefault(b.code(), 0L));
            summary.add(item);
        }
        return summary;
    }

    /** 追加 ILIKE 过滤条件（值空白则跳过） */
    private void appendLike(StringBuilder filter, List<Object> params, String column, String value) {
        if (StringUtils.hasText(value)) {
            filter.append(" AND t.").append(column).append(" ILIKE ?");
            params.add("%" + value.trim() + "%");
        }
    }

    /** 账期比较符 → SQL 运算符（非法值一律按 ≥ 处理） */
    private String accountPeriodOperator(String op) {
        if (op == null) {
            return ">=";
        }
        return switch (op.trim()) {
            case "le" -> "<=";
            case "eq" -> "=";
            case "gt" -> ">";
            case "lt" -> "<";
            default -> ">=";
        };
    }

    private List<Object> concat(List<Object> a, List<Object> b) {
        List<Object> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    /**
     * 构建 UNION ALL 子查询
     *
     * @param tenantId    租户ID（填充到各分支占位符）
     * @param mode        状态过滤模式
     * @param unionParams 输出参数：按分支顺序收集 tenantId
     */
    private String buildUnion(Long tenantId, StatusMode mode, List<Object> unionParams) {
        StringBuilder union = new StringBuilder();
        for (int i = 0; i < BRANCHES.size(); i++) {
            DocBranch b = BRANCHES.get(i);
            if (i > 0) {
                union.append(" UNION ALL ");
            }
            // 首分支补列别名，后续分支按位置对齐；extra 段各分支列数固定 16 列
            if (i == 0) {
                union.append("SELECT '").append(b.code()).append("' AS doc_type_code, '")
                     .append(b.name()).append("' AS doc_type, sub.* FROM (")
                     .append(b.select()).append(", ").append(stripSelect(b.extra()))
                     .append(" WHERE ").append(b.baseWhere());
            } else {
                union.append("SELECT '").append(b.code()).append("', '").append(b.name())
                     .append("', sub.* FROM (").append(b.select()).append(", ").append(stripSelect(b.extra()))
                     .append(" WHERE ").append(b.baseWhere());
            }
            if (mode == StatusMode.PENDING) {
                union.append(' ').append(b.pendingCond());
            } else if (mode == StatusMode.DRAFT) {
                union.append(' ').append(b.draftCond());
            }
            union.append(") sub");
            if (b.hasTenant()) {
                unionParams.add(tenantId);
            }
        }
        return union.toString();
    }

    /** 去掉分支 extra 片段开头的 SELECT 前缀（外层的 select() 已带列，额外列直接逗号续写） */
    private String stripSelect(String extra) {
        return extra.startsWith("SELECT ") ? extra.substring("SELECT ".length()) : extra;
    }
}
