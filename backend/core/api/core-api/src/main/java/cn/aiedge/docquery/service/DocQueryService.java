package cn.aiedge.docquery.service;

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
 * 综合单据查询服务（分析 > 综合单据）
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
 * <p>取舍说明：</p>
 * <ul>
 *   <li>erp_stock_transfer 无往来单位列，partner_name 取「调出仓→调入仓」拼接</li>
 *   <li>erp_stock_damage / erp_stock_overflow 无往来单位列，partner_name 取仓库名</li>
 *   <li>expense_application 无往来单位/create_by 列，partner_name 取申请人，create_by 返回 NULL；
 *       该表无 tenant_id/deleted 列，不参与租户与删除标记过滤</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocQueryService {

    private final JdbcTemplate jdbcTemplate;

    /** 单据分支定义 */
    private record DocBranch(String code, String name, String select, String baseWhere,
                             boolean hasTenant, String pendingCond, String draftCond) {
    }

    /** 状态过滤模式 */
    public enum StatusMode {ALL, PENDING, DRAFT}

    private static final List<DocBranch> BRANCHES = List.of(
        new DocBranch("SALE_ORDER", "销售订单",
            "SELECT order_no AS doc_no, create_time AS biz_date, customer_name AS partner_name, "
            + "total_amount AS amount, status::text AS status, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审核' WHEN 2 THEN '待发货' WHEN 3 THEN '部分发货' "
            + "WHEN 4 THEN '发货完成' WHEN 5 THEN '交易完成' WHEN 6 THEN '已取消' ELSE '未知' END AS status_text, "
            + "create_by FROM erp_sale_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_OUTBOUND", "销售出库单",
            "SELECT outbound_no, create_time, customer_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 11 THEN '已完成' WHEN 12 THEN '已取消' ELSE '处理中' END, create_by FROM erp_sale_outbound",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_RETURN", "销售退货单",
            "SELECT return_no, create_time, customer_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 3 THEN '已完成' WHEN 4 THEN '已取消' ELSE '未知' END, create_by FROM erp_sale_return",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("SALE_PRE_ORDER", "销售预订单",
            "SELECT order_no, create_time, customer_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '审核中' WHEN 2 THEN '待订货' WHEN 3 THEN '部分订货' "
            + "WHEN 4 THEN '已订货' WHEN 5 THEN '已完成' WHEN -1 THEN '已取消' ELSE '未知' END, create_by FROM erp_sale_pre_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_ORDER", "采购订单",
            "SELECT order_no, create_time, supplier_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已下达' "
            + "WHEN 4 THEN '已取消' WHEN 5 THEN '履行中' WHEN 6 THEN '已完成' ELSE '未知' END, create_by FROM erp_purchase_order",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_INBOUND", "采购入库单",
            "SELECT inbound_no, create_time, supplier_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '待收货' "
            + "WHEN 4 THEN '已收货' WHEN 5 THEN '待质检' WHEN 6 THEN '已质检' WHEN 7 THEN '待入库' "
            + "WHEN 8 THEN '已入库' WHEN 9 THEN '已完成' WHEN 10 THEN '已取消' ELSE '未知' END, create_by FROM erp_purchase_inbound",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PURCHASE_RETURN", "采购退货单",
            "SELECT return_no, create_time, supplier_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' "
            + "WHEN 3 THEN '已拒绝' WHEN 4 THEN '已完成' WHEN 5 THEN '已取消' ELSE '未知' END, create_by FROM erp_purchase_return",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("RECEIPT", "收款单",
            "SELECT receipt_no, create_time, customer_name, receipt_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '待核销' WHEN 5 THEN '核销中' WHEN 6 THEN '已核销' WHEN 7 THEN '已完成' "
            + "WHEN 8 THEN '已取消' ELSE '未知' END, create_by FROM erp_receipt",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("PAYMENT", "付款单",
            "SELECT payment_no, create_time, supplier_name, payment_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '待核销' WHEN 5 THEN '核销中' WHEN 6 THEN '已核销' WHEN 7 THEN '已完成' "
            + "WHEN 8 THEN '已取消' ELSE '未知' END, create_by FROM erp_payment",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_TRANSFER", "调拨单",
            "SELECT transfer_no, create_time, "
            + "COALESCE(from_warehouse_name,'') || '→' || COALESCE(to_warehouse_name,''), total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审批' WHEN 3 THEN '已拒绝' "
            + "WHEN 4 THEN '调拨中' WHEN 5 THEN '已完成' WHEN 6 THEN '已取消' ELSE '未知' END, create_by FROM erp_stock_transfer",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_DAMAGE", "报损单",
            "SELECT damage_no, create_time, warehouse_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审核' "
            + "WHEN 3 THEN '已执行' WHEN 4 THEN '已拒绝' WHEN 5 THEN '已取消' ELSE '未知' END, create_by FROM erp_stock_damage",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        new DocBranch("STOCK_OVERFLOW", "报溢单",
            "SELECT overflow_no, create_time, warehouse_name, total_amount, status::text, "
            + "CASE status WHEN 0 THEN '草稿' WHEN 1 THEN '待审批' WHEN 2 THEN '已审核' "
            + "WHEN 3 THEN '已执行' WHEN 4 THEN '已拒绝' WHEN 5 THEN '已取消' ELSE '未知' END, create_by FROM erp_stock_overflow",
            "tenant_id = ? AND deleted = 0", true, "AND status = 1", "AND status = 0"),
        // JPA 表：无 tenant_id/deleted/create_by 列
        new DocBranch("EXPENSE", "费用申请单",
            "SELECT application_code, apply_date::timestamp, applicant_name, total_amount, status, "
            + "CASE status WHEN 'DRAFT' THEN '草稿' WHEN 'SUBMITTED' THEN '待审批' "
            + "WHEN 'DEPARTMENT_APPROVING' THEN '部门审批中' WHEN 'FINANCE_APPROVING' THEN '财务审批中' "
            + "WHEN 'GENERAL_MANAGER_APPROVING' THEN '总经理审批中' WHEN 'APPROVED' THEN '审批通过' "
            + "WHEN 'REJECTED' THEN '审批拒绝' WHEN 'PAID' THEN '已支付' WHEN 'REIMBURSED' THEN '已报销' "
            + "WHEN 'CANCELLED' THEN '已取消' ELSE status END, NULL::bigint FROM expense_application",
            "1=1", false,
            "AND status IN ('SUBMITTED','DEPARTMENT_APPROVING','FINANCE_APPROVING','GENERAL_MANAGER_APPROVING')",
            "AND status = 'DRAFT'")
    );

    /**
     * 综合单据分页查询
     *
     * @param tenantId    当前租户ID
     * @param mode        状态过滤：ALL 全部 / PENDING 待审批 / DRAFT 草稿
     * @param docType     单据类型代码（如 SALE_ORDER），空为全部
     * @param docNo       单据号模糊
     * @param partnerName 往来单位模糊
     * @param startDate   业务日期起（yyyy-MM-dd，含）
     * @param endDate     业务日期止（yyyy-MM-dd，含）
     * @param page        页码（从1开始）
     * @param size        每页条数
     * @return {list, total, page, size}
     */
    public Map<String, Object> page(Long tenantId, StatusMode mode, String docType, String docNo,
                                    String partnerName, String startDate, String endDate, int page, int size) {
        List<Object> unionParams = new ArrayList<>();
        String unionSql = buildUnion(tenantId, mode, unionParams);

        // 外层过滤条件（动态拼接）
        StringBuilder filter = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();
        if (StringUtils.hasText(docType)) {
            filter.append(" AND t.doc_type_code = ?");
            filterParams.add(docType.trim());
        }
        if (StringUtils.hasText(docNo)) {
            filter.append(" AND t.doc_no ILIKE ?");
            filterParams.add("%" + docNo.trim() + "%");
        }
        if (StringUtils.hasText(partnerName)) {
            filter.append(" AND t.partner_name ILIKE ?");
            filterParams.add("%" + partnerName.trim() + "%");
        }
        if (StringUtils.hasText(startDate)) {
            filter.append(" AND t.biz_date >= CAST(? AS timestamp)");
            filterParams.add(startDate.trim());
        }
        if (StringUtils.hasText(endDate)) {
            filter.append(" AND t.biz_date < CAST(? AS timestamp) + INTERVAL '1 day'");
            filterParams.add(endDate.trim());
        }

        // 总数
        List<Object> countParams = new ArrayList<>(unionParams);
        countParams.addAll(filterParams);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (" + unionSql + ") t WHERE 1=1" + filter,
            Long.class, countParams.toArray());

        // 分页数据（按业务日期倒序）
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> listParams = new ArrayList<>(unionParams);
        listParams.addAll(filterParams);
        listParams.add(safeSize);
        listParams.add((safePage - 1) * safeSize);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT t.doc_type_code AS \"docTypeCode\", t.doc_type AS \"docType\", t.doc_no AS \"docNo\", "
            + "to_char(t.biz_date, 'YYYY-MM-DD HH24:MI:SS') AS \"bizDate\", "
            + "t.partner_name AS \"partnerName\", t.amount, t.status, t.status_text AS \"statusText\", "
            + "t.create_by AS \"createBy\" "
            + "FROM (" + unionSql + ") t WHERE 1=1" + filter
            + " ORDER BY t.biz_date DESC NULLS LAST LIMIT ? OFFSET ?",
            listParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total != null ? total : 0L);
        result.put("page", safePage);
        result.put("size", safeSize);
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
            // 首分支补列别名，后续分支按位置对齐
            // 条件拼在子查询内部（直接作用于基表列，避免外层引用不到 tenant_id/status 原列）
            if (i == 0) {
                union.append("SELECT '").append(b.code()).append("' AS doc_type_code, '")
                     .append(b.name()).append("' AS doc_type, sub.* FROM (")
                     .append(b.select()).append(" WHERE ").append(b.baseWhere());
            } else {
                union.append("SELECT '").append(b.code()).append("', '").append(b.name())
                     .append("', sub.* FROM (").append(b.select()).append(" WHERE ").append(b.baseWhere());
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
}
