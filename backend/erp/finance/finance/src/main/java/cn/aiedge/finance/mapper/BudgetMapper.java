package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Budget;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
    
    @Select("SELECT * FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = #{status} ORDER BY period DESC")
    List<Budget> listByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
    
    @Select("SELECT * FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period}")
    List<Budget> listByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_type = #{budgetType} ORDER BY period DESC")
    List<Budget> listByType(@Param("tenantId") Long tenantId, @Param("budgetType") Integer budgetType);
    
    @Select("SELECT * FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND department_id = #{departmentId} ORDER BY period DESC")
    List<Budget> listByDepartment(@Param("tenantId") Long tenantId, @Param("departmentId") Long departmentId);
    
    @Select("SELECT * FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_code = #{budgetCode}")
    Budget getByCode(@Param("tenantId") Long tenantId, @Param("budgetCode") String budgetCode);
    
    @Select("SELECT SUM(total_amount) FROM finance_budget WHERE tenant_id = #{tenantId} AND deleted = 0 AND period = #{period} AND status IN (2, 4)")
    java.math.BigDecimal sumTotalByPeriod(@Param("tenantId") Long tenantId, @Param("period") String period);
}