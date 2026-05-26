package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BudgetItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BudgetItemMapper extends BaseMapper<BudgetItem> {
    
    @Select("SELECT * FROM finance_budget_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_id = #{budgetId} ORDER BY subject_code")
    List<BudgetItem> listByBudgetId(@Param("tenantId") Long tenantId, @Param("budgetId") Long budgetId);
    
    @Select("SELECT * FROM finance_budget_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND subject_id = #{subjectId}")
    List<BudgetItem> listBySubjectId(@Param("tenantId") Long tenantId, @Param("subjectId") Long subjectId);
    
    @Select("SELECT * FROM finance_budget_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND alert_flag = 1")
    List<BudgetItem> listAlertItems(@Param("tenantId") Long tenantId);
    
    @Select("SELECT SUM(budget_amount) FROM finance_budget_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_id = #{budgetId}")
    BigDecimal sumBudgetAmountByBudgetId(@Param("tenantId") Long tenantId, @Param("budgetId") Long budgetId);
    
    @Select("SELECT SUM(used_amount) FROM finance_budget_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND budget_id = #{budgetId}")
    BigDecimal sumUsedAmountByBudgetId(@Param("tenantId") Long tenantId, @Param("budgetId") Long budgetId);
}