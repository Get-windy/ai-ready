package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.LedgerEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 分类账条目Mapper接口
 */
@Mapper
public interface LedgerEntryMapper extends BaseMapper<LedgerEntry> {

    /**
     * 根据科目ID、会计年度和期间查询
     */
    @Select("SELECT * FROM finance_ledger WHERE subject_id = #{subjectId} AND fiscal_year = #{fiscalYear} AND fiscal_period = #{fiscalPeriod} AND deleted_flag = 0")
    Optional<LedgerEntry> findBySubjectIdAndFiscalYearAndFiscalPeriod(@Param("subjectId") Long subjectId, @Param("fiscalYear") Integer fiscalYear, @Param("fiscalPeriod") Integer fiscalPeriod);

    /**
     * 根据会计年度查询
     */
    @Select("SELECT * FROM finance_ledger WHERE fiscal_year = #{fiscalYear} AND deleted_flag = 0")
    List<LedgerEntry> findByFiscalYear(Integer fiscalYear);

    /**
     * 根据科目ID列表和会计年度查询
     */
    @Select("SELECT * FROM finance_ledger WHERE subject_id IN (${subjectIds}) AND fiscal_year = #{fiscalYear} AND deleted_flag = 0")
    List<LedgerEntry> findBySubjectIdInAndFiscalYear(@Param("subjectIds") List<Long> subjectIds, @Param("fiscalYear") Integer fiscalYear);
}
