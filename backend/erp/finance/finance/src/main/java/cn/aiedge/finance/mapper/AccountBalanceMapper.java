package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AccountBalance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountBalanceMapper extends BaseMapper<AccountBalance> {
    
    @Select("SELECT * FROM finance_account_balance WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} ORDER BY subject_code")
    List<AccountBalance> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_account_balance WHERE tenant_id = #{tenantId} AND deleted = 0 AND subject_id = #{subjectId} AND period = #{period}")
    AccountBalance getBySubjectAndPeriod(@Param("tenantId") Long tenantId, @Param("subjectId") Long subjectId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_account_balance WHERE tenant_id = #{tenantId} AND deleted = 0 AND subject_code = #{subjectCode} AND period = #{period}")
    AccountBalance getByCodeAndPeriod(@Param("tenantId") Long tenantId, @Param("subjectCode") String subjectCode, @Param("period") String period);
    
    @Select("SELECT SUM(ending_debit) - SUM(ending_credit) FROM finance_account_balance WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND subject_type IN (1, 4)")
    BigDecimal calculateTotalAssets(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT SUM(ending_credit) - SUM(ending_debit) FROM finance_account_balance WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND subject_type IN (2, 3)")
    BigDecimal calculateTotalLiabilitiesAndEquity(@Param("tenantId") Long tenantId, @Param("period") String period);
}