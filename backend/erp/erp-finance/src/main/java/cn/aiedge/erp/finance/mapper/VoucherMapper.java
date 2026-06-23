package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.Voucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 记账凭证Mapper接口
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 根据凭证编号查询
     */
    @Select("SELECT * FROM finance_voucher WHERE voucher_no = #{voucherNo} AND deleted_flag = 0")
    Optional<Voucher> findByVoucherNo(String voucherNo);

    /**
     * 根据凭证状态查询
     */
    @Select("SELECT * FROM finance_voucher WHERE status = #{status} AND deleted_flag = 0")
    List<Voucher> findByStatus(String status);

    /**
     * 根据会计年度和期间查询
     */
    @Select("SELECT * FROM finance_voucher WHERE fiscal_year = #{fiscalYear} AND fiscal_period = #{fiscalPeriod} AND deleted_flag = 0")
    List<Voucher> findByFiscalYearAndFiscalPeriod(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalPeriod") Integer fiscalPeriod);

    /**
     * 根据凭证日期范围查询
     */
    @Select("SELECT * FROM finance_voucher WHERE voucher_date BETWEEN #{startDate} AND #{endDate} AND deleted_flag = 0 ORDER BY voucher_date ASC")
    List<Voucher> findByVoucherDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 统计指定会计年度和期间的凭证数量
     */
    @Select("SELECT COUNT(*) FROM finance_voucher WHERE fiscal_year = #{fiscalYear} AND fiscal_period = #{fiscalPeriod} AND deleted_flag = 0")
    long countByFiscalYearAndFiscalPeriod(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalPeriod") Integer fiscalPeriod);
}
