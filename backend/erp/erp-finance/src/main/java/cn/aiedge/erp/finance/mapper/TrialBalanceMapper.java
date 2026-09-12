package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.TrialBalanceAggDTO;
import cn.aiedge.erp.finance.dto.TrialBalanceQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 科目余额表取数 Mapper
 *
 * 口径（P0 红线）：只读取自凭证分录（finance_voucher_item ⨝ finance_voucher），
 * 仅取已记账凭证（posted），严禁绕过凭证直改余额。
 * 年初结转余额取自 finance_ledger 会计年首期期初（该值同样由凭证过账/结账产生）。
 */
@Mapper
public interface TrialBalanceMapper {

    /**
     * 按科目编码聚合四段金额：
     * openingNet = 会月起之前（本年）的借贷净额；period = 起止月区间借贷；
     * yearDebit/yearCredit = 年初至止月累计借贷。
     */
    @Select("<script>"
            + "SELECT vi.subject_code AS subjectCode,"
            + " COALESCE(SUM(CASE WHEN v.fiscal_period &lt; #{q.startPeriod}"
            + "   THEN COALESCE(vi.debit_amount, 0) - COALESCE(vi.credit_amount, 0) ELSE 0 END), 0) AS openingNet,"
            + " COALESCE(SUM(CASE WHEN v.fiscal_period &gt;= #{q.startPeriod} AND v.fiscal_period &lt;= #{q.endPeriod}"
            + "   THEN COALESCE(vi.debit_amount, 0) ELSE 0 END), 0) AS periodDebit,"
            + " COALESCE(SUM(CASE WHEN v.fiscal_period &gt;= #{q.startPeriod} AND v.fiscal_period &lt;= #{q.endPeriod}"
            + "   THEN COALESCE(vi.credit_amount, 0) ELSE 0 END), 0) AS periodCredit,"
            + " COALESCE(SUM(CASE WHEN v.fiscal_period &lt;= #{q.endPeriod}"
            + "   THEN COALESCE(vi.debit_amount, 0) ELSE 0 END), 0) AS yearDebit,"
            + " COALESCE(SUM(CASE WHEN v.fiscal_period &lt;= #{q.endPeriod}"
            + "   THEN COALESCE(vi.credit_amount, 0) ELSE 0 END), 0) AS yearCredit"
            + " FROM finance_voucher_item vi"
            + " JOIN finance_voucher v ON vi.voucher_id = v.id"
            + " WHERE vi.deleted_flag = 0 AND v.deleted_flag = 0 AND v.status = 'posted'"
            + " AND v.fiscal_year = #{q.fiscalYear}"
            + " GROUP BY vi.subject_code"
            + "</script>")
    List<TrialBalanceAggDTO> aggregateBySubject(@Param("q") TrialBalanceQuery q);

    /**
     * 会计年首期期初净额（借正贷负）：上一年结账结转而来，非本年度凭证，故单独取数。
     */
    @Select("SELECT subject_code AS subjectCode,"
            + " COALESCE(SUM(COALESCE(opening_debit, 0) - COALESCE(opening_credit, 0)), 0) AS openingNet"
            + " FROM finance_ledger"
            + " WHERE deleted_flag = 0 AND fiscal_year = #{fiscalYear} AND fiscal_period = 1"
            + " GROUP BY subject_code")
    List<TrialBalanceAggDTO> selectYearOpening(@Param("fiscalYear") Integer fiscalYear);
}
