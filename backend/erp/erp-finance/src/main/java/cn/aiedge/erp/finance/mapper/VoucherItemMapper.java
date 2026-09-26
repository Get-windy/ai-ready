package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.VoucherItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证明细行Mapper接口
 */
@Mapper
public interface VoucherItemMapper extends BaseMapper<VoucherItem> {

    /**
     * 根据凭证ID查询
     */
    @Select("SELECT * FROM finance_voucher_item WHERE voucher_id = #{voucherId} AND deleted_flag = 0")
    List<VoucherItem> findByVoucherId(Long voucherId);

    /**
     * 根据会计科目ID查询
     */
    @Select("SELECT * FROM finance_voucher_item WHERE subject_id = #{subjectId} AND deleted_flag = 0")
    List<VoucherItem> findBySubjectId(Long subjectId);

    /**
     * 根据来源业务类型和ID查询
     */
    @Select("SELECT * FROM finance_voucher_item WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND deleted_flag = 0")
    List<VoucherItem> findBySourceTypeAndSourceId(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    /**
     * 按会计期间查询凭证明细（联查凭证号与凭证日期，用于明细账）
     */
    @Select("SELECT vi.*, v.voucher_no, v.voucher_date FROM finance_voucher_item vi " +
            "LEFT JOIN finance_voucher v ON vi.voucher_id = v.id " +
            "WHERE vi.deleted_flag = 0 " +
            "AND (#{fiscalYear} IS NULL OR v.fiscal_year = #{fiscalYear}) " +
            "AND (#{fiscalPeriod} IS NULL OR v.fiscal_period = #{fiscalPeriod}) " +
            "AND (#{subjectCode} IS NULL OR #{subjectCode} = '' OR vi.subject_code = #{subjectCode}) " +
            "ORDER BY v.voucher_date ASC, vi.id ASC")
    List<VoucherItem> findLedgerDetail(@Param("fiscalYear") Integer fiscalYear,
                                       @Param("fiscalPeriod") Integer fiscalPeriod,
                                       @Param("subjectCode") String subjectCode);

    /**
     * 汇总「某科目在某会计期间」的已过账凭证分录借方合计（用于总账重算）。
     *
     * <p>只计 {@code status='posted'}：草稿/已审核未过账的凭证尚未入账，
     * 已冲销的原凭证（reversed）也不再作为余额来源（其冲销凭证已单独入账）。</p>
     */
    @Select("SELECT COALESCE(SUM(i.debit_amount), 0) FROM finance_voucher_item i " +
            "JOIN finance_voucher v ON v.id = i.voucher_id " +
            "WHERE i.subject_id = #{subjectId} AND i.deleted_flag = 0 " +
            "AND v.status = 'posted' AND v.deleted_flag = 0 " +
            "AND v.fiscal_year = #{fiscalYear} AND v.fiscal_period = #{fiscalPeriod}")
    BigDecimal sumPostedDebitInPeriod(@Param("subjectId") Long subjectId,
                                      @Param("fiscalYear") Integer fiscalYear,
                                      @Param("fiscalPeriod") Integer fiscalPeriod);

    /**
     * 汇总「某科目在某会计期间」的已过账凭证分录贷方合计（用于总账重算）。
     */
    @Select("SELECT COALESCE(SUM(i.credit_amount), 0) FROM finance_voucher_item i " +
            "JOIN finance_voucher v ON v.id = i.voucher_id " +
            "WHERE i.subject_id = #{subjectId} AND i.deleted_flag = 0 " +
            "AND v.status = 'posted' AND v.deleted_flag = 0 " +
            "AND v.fiscal_year = #{fiscalYear} AND v.fiscal_period = #{fiscalPeriod}")
    BigDecimal sumPostedCreditInPeriod(@Param("subjectId") Long subjectId,
                                       @Param("fiscalYear") Integer fiscalYear,
                                       @Param("fiscalPeriod") Integer fiscalPeriod);

    /**
     * 汇总「某科目在该会计期间之前所有期间」的已过账凭证净额（借 − 贷），用作重算的期初。
     *
     * <p>跨年自然成立：条件按 (年, 期) 的字典序比较，无需额外处理年结。</p>
     */
    @Select("SELECT COALESCE(SUM(i.debit_amount - i.credit_amount), 0) FROM finance_voucher_item i " +
            "JOIN finance_voucher v ON v.id = i.voucher_id " +
            "WHERE i.subject_id = #{subjectId} AND i.deleted_flag = 0 " +
            "AND v.status = 'posted' AND v.deleted_flag = 0 " +
            "AND (v.fiscal_year < #{fiscalYear} " +
            "     OR (v.fiscal_year = #{fiscalYear} AND v.fiscal_period < #{fiscalPeriod}))")
    BigDecimal sumPostedNetBeforePeriod(@Param("subjectId") Long subjectId,
                                        @Param("fiscalYear") Integer fiscalYear,
                                        @Param("fiscalPeriod") Integer fiscalPeriod);
}
