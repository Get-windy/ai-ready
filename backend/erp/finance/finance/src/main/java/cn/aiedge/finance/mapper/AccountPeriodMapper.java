package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.AccountPeriod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AccountPeriodMapper extends BaseMapper<AccountPeriod> {
    
    @Select("SELECT * FROM finance_account_period WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY year DESC, month DESC")
    List<AccountPeriod> listAll(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_account_period WHERE tenant_id = #{tenantId} AND deleted = 0 AND year = #{year} ORDER BY month")
    List<AccountPeriod> listByYear(@Param("tenantId") Long tenantId, @Param("year") Integer year);
    
    @Select("SELECT * FROM finance_account_period WHERE tenant_id = #{tenantId} AND deleted = 0 AND year = #{year} AND month = #{month}")
    AccountPeriod getByYearMonth(@Param("tenantId") Long tenantId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Select("SELECT * FROM finance_account_period WHERE tenant_id = #{tenantId} AND deleted = 0 AND is_current = 1")
    AccountPeriod getCurrentPeriod(@Param("tenantId") Long tenantId);
    
    @Select("SELECT * FROM finance_account_period WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = #{status} ORDER BY year DESC, month DESC")
    List<AccountPeriod> listByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
    
    @Update("UPDATE finance_account_period SET is_current = 0 WHERE tenant_id = #{tenantId}")
    int clearCurrentFlag(@Param("tenantId") Long tenantId);
    
    @Update("UPDATE finance_account_period SET is_current = 1, status = 1 WHERE id = #{id}")
    int setCurrentPeriod(@Param("id") Long id);
}