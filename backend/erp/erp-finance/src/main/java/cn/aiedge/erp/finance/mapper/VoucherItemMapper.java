package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.VoucherItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
