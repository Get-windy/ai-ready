package cn.aiedge.erp.finance.expensedoc.mapper;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsAggVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsRowVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 费用统计 Mapper（只读）
 *
 * <p>P0 单一口径：唯一数据源为《费用单》erp_expense_doc + erp_expense_item，
 * 与《费用审批》共用同一状态机（approval_status），不另建统计表。</p>
 *
 * <p>统计范围：默认排除「已取消」（status=2）单据；红冲单（red_flag=1）默认计入（作为负向冲销），
 * 传 showRed=false 时排除。</p>
 *
 * <p>分组金额口径：单据数/总额取单据级（本单金额）；分组维度取明细级（费用项金额）。
 * 费用单存在 P1 守恒（本单金额 = Σ费用项金额），两者一致。</p>
 */
@Mapper
public interface ExpenseStatsMapper {

    /** 单据级过滤条件（doc 表别名 h，条件参数名 q），各查询复用保证口径唯一 */
    String DOC_FILTER =
            "<if test='q.docNo != null and q.docNo != \"\"'> AND h.doc_no LIKE CONCAT('%', #{q.docNo}, '%') </if>" +
            "<if test='q.partnerName != null and q.partnerName != \"\"'> AND h.partner_name LIKE CONCAT('%', #{q.partnerName}, '%') </if>" +
            "<if test='q.handlerName != null and q.handlerName != \"\"'> AND h.handler_name LIKE CONCAT('%', #{q.handlerName}, '%') </if>" +
            "<if test='q.deptName != null and q.deptName != \"\"'> AND h.dept_name LIKE CONCAT('%', #{q.deptName}, '%') </if>" +
            "<if test='q.creatorName != null and q.creatorName != \"\"'> AND h.creator_name LIKE CONCAT('%', #{q.creatorName}, '%') </if>" +
            "<if test='q.bookkeeperName != null and q.bookkeeperName != \"\"'> AND h.bookkeeper_name LIKE CONCAT('%', #{q.bookkeeperName}, '%') </if>" +
            "<if test='q.payAccountName != null and q.payAccountName != \"\"'> AND h.pay_account_name LIKE CONCAT('%', #{q.payAccountName}, '%') </if>" +
            "<if test='q.summary != null and q.summary != \"\"'> AND h.summary LIKE CONCAT('%', #{q.summary}, '%') </if>" +
            "<if test='q.remark != null and q.remark != \"\"'> AND h.remark LIKE CONCAT('%', #{q.remark}, '%') </if>" +
            "<if test='q.status != null'> AND h.status = #{q.status} </if>" +
            "<if test='q.status == null'> AND h.status != 2 </if>" +
            "<if test='q.expenseType != null'> AND h.expense_type = #{q.expenseType} </if>" +
            "<if test='q.approvalStatus != null'> AND h.approval_status = #{q.approvalStatus} </if>" +
            "<if test='q.dateStart != null'> AND h.doc_date &gt;= #{q.dateStart} </if>" +
            "<if test='q.dateEnd != null'> AND h.doc_date &lt;= #{q.dateEnd} </if>" +
            "<if test='q.showRed != null and q.showRed == false'> AND (h.red_flag IS NULL OR h.red_flag = 0) </if>";

    /** 明细级过滤条件（item 表别名 i + doc 表别名 h） */
    String ITEM_FILTER =
            DOC_FILTER +
            "<if test='q.expenseName != null and q.expenseName != \"\"'> AND i.expense_name LIKE CONCAT('%', #{q.expenseName}, '%') </if>" +
            "<if test='q.subjectCode != null and q.subjectCode != \"\"'> AND i.subject_code LIKE CONCAT('%', #{q.subjectCode}, '%') </if>" +
            "<if test='q.itemRemark != null and q.itemRemark != \"\"'> AND i.remark LIKE CONCAT('%', #{q.itemRemark}, '%') </if>";

    /**
     * 汇总卡片列（明细级：费用项金额口径，单据数按主表去重）
     *
     * <p>P1 守恒（本单金额 = Σ费用项金额）保证明细口径与单据口径一致；
     * 采用明细口径可在按「费用名称/科目」过滤时正确收敛到匹配的费用项。</p>
     */
    String AGG_COLUMNS =
            " COUNT(DISTINCT h.id) AS expense_count," +
            " COALESCE(SUM(i.amount), 0) AS total_amount," +
            " COALESCE(SUM(CASE WHEN h.approval_status = 2 THEN i.amount ELSE 0 END), 0) AS approved_amount," +
            " COALESCE(SUM(CASE WHEN h.approval_status = 1 THEN i.amount ELSE 0 END), 0) AS pending_amount," +
            " COALESCE(SUM(CASE WHEN h.approval_status = 3 THEN i.amount ELSE 0 END), 0) AS rejected_amount," +
            " COALESCE(SUM(CASE WHEN h.approval_status = 0 OR h.approval_status IS NULL THEN i.amount ELSE 0 END), 0) AS draft_amount," +
            " COALESCE(SUM(CASE WHEN h.status = 1 THEN i.amount ELSE 0 END), 0) AS paid_amount," +
            " COALESCE(SUM(CASE WHEN h.status = 0 THEN i.amount ELSE 0 END), 0) AS unpaid_amount," +
            " COALESCE(SUM(CASE WHEN h.expense_type = 0 THEN i.amount ELSE 0 END), 0) AS partner_amount," +
            " COALESCE(SUM(CASE WHEN h.expense_type = 1 THEN i.amount ELSE 0 END), 0) AS internal_amount";

    /**
     * 明细级聚合：汇总卡片（总额/已审批/待审批/已驳回/已记账/未记账/单据数）
     */
    @Select("<script>" +
            "SELECT " + AGG_COLUMNS +
            " FROM erp_expense_item i " +
            "INNER JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " + ITEM_FILTER +
            "</script>")
    ExpenseStatsAggVO aggregate(@Param("q") ExpenseDocQuery q);

    /**
     * 费用笔数（明细级去重统计，与分组维度同口径）
     */
    @Select("<script>" +
            "SELECT COUNT(i.id) FROM erp_expense_item i " +
            "INNER JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " + ITEM_FILTER +
            "</script>")
    Integer countItems(@Param("q") ExpenseDocQuery q);

    /**
     * 按部门分组（明细级）：部门费用金额/笔数/占比
     */
    @Select("<script>" +
            "SELECT COALESCE(NULLIF(TRIM(h.dept_name), ''), '未指定部门') AS group_key," +
            "       NULL AS group_code, NULL AS subject_name, NULL AS subject_code," +
            "       COUNT(DISTINCT h.id) AS doc_count, COUNT(i.id) AS item_count," +
            "       COALESCE(SUM(i.amount), 0) AS total_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 2 THEN i.amount ELSE 0 END), 0) AS approved_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 1 THEN i.amount ELSE 0 END), 0) AS pending_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 3 THEN i.amount ELSE 0 END), 0) AS rejected_amount," +
            "       COALESCE(SUM(CASE WHEN h.status = 1 THEN i.amount ELSE 0 END), 0) AS paid_amount," +
            "       NULL AS expense_type " +
            "FROM erp_expense_item i " +
            "INNER JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " + ITEM_FILTER +
            " GROUP BY COALESCE(NULLIF(TRIM(h.dept_name), ''), '未指定部门')" +
            " ORDER BY total_amount DESC, group_key ASC" +
            "</script>")
    List<ExpenseStatsRowVO> listByDepartment(@Param("q") ExpenseDocQuery q);

    /**
     * 按费用类型分组（明细级）：费用名称/科目维度金额、笔数与占比
     */
    @Select("<script>" +
            "SELECT COALESCE(NULLIF(TRIM(i.expense_name), ''), '未指定费用项') AS group_key," +
            "       MAX(i.expense_code) AS group_code," +
            "       MAX(i.subject_name) AS subject_name, MAX(i.subject_code) AS subject_code," +
            "       COUNT(DISTINCT h.id) AS doc_count, COUNT(i.id) AS item_count," +
            "       COALESCE(SUM(i.amount), 0) AS total_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 2 THEN i.amount ELSE 0 END), 0) AS approved_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 1 THEN i.amount ELSE 0 END), 0) AS pending_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 3 THEN i.amount ELSE 0 END), 0) AS rejected_amount," +
            "       COALESCE(SUM(CASE WHEN h.status = 1 THEN i.amount ELSE 0 END), 0) AS paid_amount," +
            "       MAX(h.expense_type) AS expense_type " +
            "FROM erp_expense_item i " +
            "INNER JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 " + ITEM_FILTER +
            " GROUP BY COALESCE(NULLIF(TRIM(i.expense_name), ''), '未指定费用项')" +
            " ORDER BY total_amount DESC, group_key ASC" +
            "</script>")
    List<ExpenseStatsRowVO> listByType(@Param("q") ExpenseDocQuery q);

    /**
     * 月度趋势（明细级，同汇总口径）：按单据日期月份汇总
     */
    @Select("<script>" +
            "SELECT to_char(h.doc_date, 'YYYY-MM') AS month," +
            "       COUNT(DISTINCT h.id) AS doc_count," +
            "       COALESCE(SUM(i.amount), 0) AS total_amount," +
            "       COALESCE(SUM(CASE WHEN h.approval_status = 2 THEN i.amount ELSE 0 END), 0) AS approved_amount," +
            "       COALESCE(SUM(CASE WHEN h.status = 1 THEN i.amount ELSE 0 END), 0) AS paid_amount " +
            "FROM erp_expense_item i " +
            "INNER JOIN erp_expense_doc h ON h.id = i.expense_doc_id AND h.deleted = 0 " +
            "WHERE i.deleted = 0 AND h.doc_date IS NOT NULL " + ITEM_FILTER +
            " GROUP BY to_char(h.doc_date, 'YYYY-MM')" +
            " ORDER BY month ASC" +
            "</script>")
    List<ExpenseStatsTrendVO> listMonthlyTrend(@Param("q") ExpenseDocQuery q);
}
