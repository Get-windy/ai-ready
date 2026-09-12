package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.LedgerDetailQuery;
import cn.aiedge.erp.finance.dto.LedgerDetailRowDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 明细账Mapper
 *
 * 口径（P0 红线）：明细账只读取自凭证分录（finance_voucher_item ⨝ finance_voucher），
 * 仅取已记账凭证（posted / 勾选显示红冲时含 reversed），严禁绕过凭证直改明细。
 */
@Mapper
public interface LedgerDetailMapper {

    /**
     * 查询明细账逐笔分录（含 15 列所需全部字段）
     * 期初余额行由上层组装，不在此查询内。
     */
    @org.apache.ibatis.annotations.Select("<script>"
            + "SELECT vi.id AS id, vi.voucher_id AS voucherId, v.voucher_no AS voucherNo,"
            + " v.voucher_date AS voucherDate,"
            + " COALESCE(CAST(v.post_at AS date), v.voucher_date) AS postDate,"
            + " v.voucher_type AS voucherType,"
            + " COALESCE(NULLIF(vi.summary, ''), CASE WHEN vi.source_no IS NOT NULL AND vi.source_no &lt;&gt; ''"
            + "   THEN '由[业务单据 ' || vi.source_no || ']' ELSE '' END) AS summary,"
            + " vi.subject_id AS subjectId, vi.subject_code AS subjectCode, vi.subject_name AS subjectName,"
            + " vi.aux_unit AS auxUnit, vi.aux_dept AS auxDept, vi.aux_staff AS auxStaff,"
            + " vi.debit_amount AS debitAmount, vi.credit_amount AS creditAmount,"
            + " vi.reconcile_flag AS reconcileFlag, vi.remark AS remark,"
            + " v.handler_name AS handlerName, v.dept_name AS deptName,"
            + " vi.source_type AS sourceType, vi.source_no AS sourceNo"
            + " FROM finance_voucher_item vi"
            + " JOIN finance_voucher v ON vi.voucher_id = v.id"
            + " WHERE vi.deleted_flag = 0 AND v.deleted_flag = 0"
            + " AND v.status IN ('posted', 'reversed')"
            + "<if test=\"q.showRed == null or q.showRed == false\"> AND v.status &lt;&gt; 'reversed'</if>"
            + "<if test=\"q.subjectCodes != null and q.subjectCodes.size() &gt; 0\">"
            + " AND vi.subject_code IN <foreach collection=\"q.subjectCodes\" item=\"code\" open=\"(\" separator=\",\" close=\")\">#{code}</foreach>"
            + "</if>"
            + "<if test=\"q.subjectType != null\">"
            + " AND EXISTS (SELECT 1 FROM finance_account_subject s WHERE s.deleted_flag = 0"
            + " AND s.subject_code = vi.subject_code AND s.subject_type = #{q.subjectType})"
            + "</if>"
            + "<if test=\"q.settleUnit != null and q.settleUnit != ''\"> AND vi.aux_unit ILIKE CONCAT('%', #{q.settleUnit}, '%')</if>"
            + "<if test=\"q.settleDept != null and q.settleDept != ''\"> AND vi.aux_dept ILIKE CONCAT('%', #{q.settleDept}, '%')</if>"
            + "<if test=\"q.settleStaff != null and q.settleStaff != ''\"> AND vi.aux_staff ILIKE CONCAT('%', #{q.settleStaff}, '%')</if>"
            + "<if test=\"q.handlerName != null and q.handlerName != ''\"> AND v.handler_name ILIKE CONCAT('%', #{q.handlerName}, '%')</if>"
            + "<if test=\"q.deptName != null and q.deptName != ''\"> AND v.dept_name ILIKE CONCAT('%', #{q.deptName}, '%')</if>"
            + "<if test=\"q.reconcileFlag != null\"> AND vi.reconcile_flag = #{q.reconcileFlag}</if>"
            + "<if test=\"q.summary != null and q.summary != ''\"> AND vi.summary ILIKE CONCAT('%', #{q.summary}, '%')</if>"
            + "<if test=\"q.remark != null and q.remark != ''\"> AND vi.remark ILIKE CONCAT('%', #{q.remark}, '%')</if>"
            + "<if test=\"q.dateStart != null and q.dateStart != ''\">"
            + " AND <choose><when test=\"q.dateType == 'post'\">COALESCE(CAST(v.post_at AS date), v.voucher_date)</when>"
            + "<otherwise>v.voucher_date</otherwise></choose> &gt;= CAST(#{q.dateStart} AS date)"
            + "</if>"
            + "<if test=\"q.dateEnd != null and q.dateEnd != ''\">"
            + " AND <choose><when test=\"q.dateType == 'post'\">COALESCE(CAST(v.post_at AS date), v.voucher_date)</when>"
            + "<otherwise>v.voucher_date</otherwise></choose> &lt;= CAST(#{q.dateEnd} AS date)"
            + "</if>"
            + " ORDER BY v.voucher_date ASC, v.id ASC, vi.id ASC"
            + "</script>")
    List<LedgerDetailRowDTO> selectLedgerRows(@Param("q") LedgerDetailQuery q);

    /**
     * 查询期初净额（借方 − 贷方）：科目口径、日期类型一致，仅取 beforeDate 之前的分录。
     * 过滤条件与 {@link #selectLedgerRows} 一致（除日期区间），保证期初与逐笔同口径。
     */
    @org.apache.ibatis.annotations.Select("<script>"
            + "SELECT COALESCE(SUM(vi.debit_amount - vi.credit_amount), 0)"
            + " FROM finance_voucher_item vi"
            + " JOIN finance_voucher v ON vi.voucher_id = v.id"
            + " WHERE vi.deleted_flag = 0 AND v.deleted_flag = 0"
            + " AND v.status IN ('posted', 'reversed')"
            + "<if test=\"q.showRed == null or q.showRed == false\"> AND v.status &lt;&gt; 'reversed'</if>"
            + "<if test=\"q.subjectCodes != null and q.subjectCodes.size() &gt; 0\">"
            + " AND vi.subject_code IN <foreach collection=\"q.subjectCodes\" item=\"code\" open=\"(\" separator=\",\" close=\")\">#{code}</foreach>"
            + "</if>"
            + "<if test=\"q.subjectType != null\">"
            + " AND EXISTS (SELECT 1 FROM finance_account_subject s WHERE s.deleted_flag = 0"
            + " AND s.subject_code = vi.subject_code AND s.subject_type = #{q.subjectType})"
            + "</if>"
            + "<if test=\"q.settleUnit != null and q.settleUnit != ''\"> AND vi.aux_unit ILIKE CONCAT('%', #{q.settleUnit}, '%')</if>"
            + "<if test=\"q.settleDept != null and q.settleDept != ''\"> AND vi.aux_dept ILIKE CONCAT('%', #{q.settleDept}, '%')</if>"
            + "<if test=\"q.settleStaff != null and q.settleStaff != ''\"> AND vi.aux_staff ILIKE CONCAT('%', #{q.settleStaff}, '%')</if>"
            + "<if test=\"q.handlerName != null and q.handlerName != ''\"> AND v.handler_name ILIKE CONCAT('%', #{q.handlerName}, '%')</if>"
            + "<if test=\"q.deptName != null and q.deptName != ''\"> AND v.dept_name ILIKE CONCAT('%', #{q.deptName}, '%')</if>"
            + "<if test=\"q.reconcileFlag != null\"> AND vi.reconcile_flag = #{q.reconcileFlag}</if>"
            + "<if test=\"q.summary != null and q.summary != ''\"> AND vi.summary ILIKE CONCAT('%', #{q.summary}, '%')</if>"
            + "<if test=\"q.remark != null and q.remark != ''\"> AND vi.remark ILIKE CONCAT('%', #{q.remark}, '%')</if>"
            + " AND <choose><when test=\"q.dateType == 'post'\">COALESCE(CAST(v.post_at AS date), v.voucher_date)</when>"
            + "<otherwise>v.voucher_date</otherwise></choose> &lt; CAST(#{beforeDate} AS date)"
            + "</script>")
    BigDecimal sumLedgerNetBefore(@Param("q") LedgerDetailQuery q, @Param("beforeDate") String beforeDate);
}
