package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.AuxBalanceQuery;
import cn.aiedge.erp.finance.dto.AuxBalanceRowDTO;
import cn.aiedge.erp.finance.dto.AuxBalanceSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 辅助核算余额表 Mapper
 *
 * 口径（P0 红线）：只读取自凭证分录（finance_voucher_item ⨝ finance_voucher），
 * 仅取已记账凭证（posted），严禁绕过凭证直读/直改核算项余额。
 *
 * 四段取数：
 * - 期初余额：会计月(起)之前全部期间的净额，按期初时点结转方向拆借/贷；
 * - 本期发生额：会计月(起)~会计月(止)区间内借贷发生额；
 * - 本年累计：止月所属会计年度 1 月至止月的借贷发生额；
 * - 期末余额：期初净额 + 本期借 − 本期贷，按方向拆借/贷。
 *
 * 核算项维度：往来单位=aux_unit，客户=aux_unit(应收/预收科目)，供应商=aux_unit(应付/预付科目)，
 * 职员=aux_staff，部门=aux_dept。
 */
@Mapper
public interface AuxBalanceMapper {

    /** 核算项取值表达式：按核算项类型取对应核算列 */
    String AUX_VALUE = "CASE CAST(#{q.auxType} AS varchar)"
            + " WHEN 'DEPT' THEN vi.aux_dept"
            + " WHEN 'EMPLOYEE' THEN vi.aux_staff"
            + " ELSE vi.aux_unit END";

    /** 基础明细：凭证分录 ⨝ 凭证，锁定已记账凭证 + 科目范围 + 核算项类型对应的科目范围 */
    String BASE = "SELECT vi.subject_id AS subject_id, vi.subject_code AS subject_code, vi.subject_name AS subject_name,"
            + " vi.debit_amount AS debit_amount, vi.credit_amount AS credit_amount,"
            + " v.fiscal_year AS fiscal_year, v.fiscal_period AS fiscal_period,"
            + " " + AUX_VALUE + " AS aux_value"
            + " FROM finance_voucher_item vi"
            + " JOIN finance_voucher v ON vi.voucher_id = v.id"
            + " WHERE vi.deleted_flag = 0 AND v.deleted_flag = 0 AND v.status = 'posted'"
            + "<if test='q.subjectCodes != null and q.subjectCodes.size() &gt; 0'>"
            + " AND vi.subject_code IN <foreach collection='q.subjectCodes' item='code' open='(' separator=',' close=')'>#{code}</foreach>"
            + "</if>"
            + "<if test='q.auxType == \"CUSTOMER\"'> AND (vi.subject_code LIKE '1122%' OR vi.subject_code LIKE '2203%')</if>"
            + "<if test='q.auxType == \"SUPPLIER\"'> AND (vi.subject_code LIKE '2202%' OR vi.subject_code LIKE '1123%')</if>";

    /** 按 科目 + 核算项 聚合四段金额（期初取净额，其余取借贷发生额） */
    String AGG = "SELECT MAX(b.subject_id) AS subject_id, b.subject_code AS subject_code, MAX(b.subject_name) AS subject_name,"
            + " b.aux_value AS aux_value,"
            + " COALESCE(SUM(CASE WHEN (b.fiscal_year * 100 + b.fiscal_period) &lt; #{q.startPeriodKey}"
            + "   THEN b.debit_amount - b.credit_amount ELSE 0 END), 0) AS begin_net,"
            + " COALESCE(SUM(CASE WHEN (b.fiscal_year * 100 + b.fiscal_period) &gt;= #{q.startPeriodKey}"
            + "   AND (b.fiscal_year * 100 + b.fiscal_period) &lt;= #{q.endPeriodKey}"
            + "   THEN b.debit_amount ELSE 0 END), 0) AS period_debit,"
            + " COALESCE(SUM(CASE WHEN (b.fiscal_year * 100 + b.fiscal_period) &gt;= #{q.startPeriodKey}"
            + "   AND (b.fiscal_year * 100 + b.fiscal_period) &lt;= #{q.endPeriodKey}"
            + "   THEN b.credit_amount ELSE 0 END), 0) AS period_credit,"
            + " COALESCE(SUM(CASE WHEN b.fiscal_year = #{q.endFiscalYear} AND b.fiscal_period &lt;= #{q.endFiscalMonth}"
            + "   THEN b.debit_amount ELSE 0 END), 0) AS year_debit,"
            + " COALESCE(SUM(CASE WHEN b.fiscal_year = #{q.endFiscalYear} AND b.fiscal_period &lt;= #{q.endFiscalMonth}"
            + "   THEN b.credit_amount ELSE 0 END), 0) AS year_credit"
            + " FROM (" + BASE + ") b"
            + " WHERE b.aux_value IS NOT NULL AND b.aux_value != ''"
            + " GROUP BY b.subject_code, b.aux_value";

    /** 核算项编码：按核算项类型回主数据（标量子查询，避免一码多名导致的行膨胀） */
    String AUX_CODE = "CASE CAST(#{q.auxType} AS varchar)"
            + " WHEN 'DEPT' THEN (SELECT d.dept_code FROM sys_dept d"
            + "   WHERE d.deleted = 0 AND d.dept_name = t.aux_value LIMIT 1)"
            + " WHEN 'EMPLOYEE' THEN (SELECT u.username FROM sys_user u"
            + "   WHERE u.deleted = 0 AND (u.real_name = t.aux_value OR u.nickname = t.aux_value) LIMIT 1)"
            + " ELSE (SELECT bp.party_code FROM biz_party bp"
            + "   WHERE bp.deleted = 0 AND bp.party_name = t.aux_value LIMIT 1) END";

    /** 聚合结果 + 核算项编码（供数据行 / 计数 / 合计三处复用） */
    String ENRICHED = "SELECT t.subject_id AS subject_id, t.subject_code AS subject_code, t.subject_name AS subject_name,"
            + " t.aux_value AS aux_value, " + AUX_CODE + " AS aux_code,"
            + " t.begin_net AS begin_net, t.period_debit AS period_debit, t.period_credit AS period_credit,"
            + " t.year_debit AS year_debit, t.year_credit AS year_credit"
            + " FROM (" + AGG + ") t";

    /** 结果过滤：无本期发生额不显示 / 余额为0不显示 / 关键字（作用于聚合后的别名字段） */
    String FILTERS = "<if test='q.hideNoPeriodAmount'> AND (r.period_debit != 0 OR r.period_credit != 0)</if>"
            + "<if test='q.hideZeroBalance'> AND (r.begin_net + r.period_debit - r.period_credit) != 0</if>"
            + "<if test='q.keyword != null and q.keyword != \"\"'>"
            + " AND (r.aux_value ILIKE CONCAT('%', #{q.keyword}, '%')"
            + " OR COALESCE(r.aux_code, '') ILIKE CONCAT('%', #{q.keyword}, '%')"
            + " OR r.subject_code ILIKE CONCAT('%', #{q.keyword}, '%')"
            + " OR r.subject_name ILIKE CONCAT('%', #{q.keyword}, '%'))</if>";

    /**
     * 数据行（按科目编码 + 核算项排序，内存分页切片前的全量口径由 SQL 分页承担）
     * 四段余额在 SQL 内按净额方向拆分为借/贷两列，保证 期初借−期初贷+本期借−本期贷=期末借−期末贷。
     */
    @Select("<script>"
            + "SELECT r.subject_id AS subjectId, r.subject_code AS subjectCode, r.subject_name AS subjectName,"
            + " #{q.auxType} AS auxType, r.aux_code AS auxCode, r.aux_value AS auxName,"
            + " CASE WHEN r.begin_net &gt; 0 THEN r.begin_net ELSE 0 END AS beginDebit,"
            + " CASE WHEN r.begin_net &lt; 0 THEN -r.begin_net ELSE 0 END AS beginCredit,"
            + " r.period_debit AS periodDebit, r.period_credit AS periodCredit,"
            + " r.year_debit AS yearDebit, r.year_credit AS yearCredit,"
            + " CASE WHEN (r.begin_net + r.period_debit - r.period_credit) &gt; 0"
            + "   THEN (r.begin_net + r.period_debit - r.period_credit) ELSE 0 END AS endDebit,"
            + " CASE WHEN (r.begin_net + r.period_debit - r.period_credit) &lt; 0"
            + "   THEN -(r.begin_net + r.period_debit - r.period_credit) ELSE 0 END AS endCredit"
            + " FROM (" + ENRICHED + ") r"
            + " WHERE 1 = 1 " + FILTERS
            + " ORDER BY r.subject_code, r.aux_value"
            + " LIMIT #{q.pageSize} OFFSET ((CAST(#{q.pageNum} AS int) - 1) * CAST(#{q.pageSize} AS int))"
            + "</script>")
    List<AuxBalanceRowDTO> selectRows(@Param("q") AuxBalanceQuery q);

    /** 满足条件的总行数（与数据行同口径过滤） */
    @Select("<script>"
            + "SELECT COUNT(*) FROM (" + ENRICHED + ") r WHERE 1 = 1 " + FILTERS
            + "</script>")
    long countRows(@Param("q") AuxBalanceQuery q);

    /** 表尾合计（全量口径，非当前页；各列分别求和保证与数据行逐列加总一致） */
    @Select("<script>"
            + "SELECT"
            + " COALESCE(SUM(CASE WHEN r.begin_net &gt; 0 THEN r.begin_net ELSE 0 END), 0) AS beginDebit,"
            + " COALESCE(SUM(CASE WHEN r.begin_net &lt; 0 THEN -r.begin_net ELSE 0 END), 0) AS beginCredit,"
            + " COALESCE(SUM(r.period_debit), 0) AS periodDebit,"
            + " COALESCE(SUM(r.period_credit), 0) AS periodCredit,"
            + " COALESCE(SUM(r.year_debit), 0) AS yearDebit,"
            + " COALESCE(SUM(r.year_credit), 0) AS yearCredit,"
            + " COALESCE(SUM(CASE WHEN (r.begin_net + r.period_debit - r.period_credit) &gt; 0"
            + "   THEN (r.begin_net + r.period_debit - r.period_credit) ELSE 0 END), 0) AS endDebit,"
            + " COALESCE(SUM(CASE WHEN (r.begin_net + r.period_debit - r.period_credit) &lt; 0"
            + "   THEN -(r.begin_net + r.period_debit - r.period_credit) ELSE 0 END), 0) AS endCredit"
            + " FROM (" + ENRICHED + ") r WHERE 1 = 1 " + FILTERS
            + "</script>")
    AuxBalanceSummaryDTO selectSummary(@Param("q") AuxBalanceQuery q);
}
