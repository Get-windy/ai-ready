package cn.aiedge.erp.expense.mapper;

import cn.aiedge.erp.expense.model.ExpenseItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ExpenseItemMapper extends BaseMapper<ExpenseItem> {

    @Select("SELECT * FROM expense_item WHERE application_id = #{applicationId} ORDER BY id")
    List<ExpenseItem> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM expense_item WHERE expense_type = #{expenseType}")
    List<ExpenseItem> findByExpenseType(@Param("expenseType") String expenseType);

    @Select("SELECT SUM(amount) FROM expense_item WHERE application_id = #{applicationId}")
    BigDecimal sumAmountByApplication(@Param("applicationId") Long applicationId);

    @Select("SELECT COUNT(*) FROM expense_item WHERE application_id = #{applicationId}")
    Integer countByApplication(@Param("applicationId") Long applicationId);

    @Select("SELECT SUM(amount) FROM expense_item WHERE application_id IN (SELECT id FROM expense_application WHERE status = 'APPROVED' AND application_date BETWEEN #{startDate} AND #{endDate})")
    BigDecimal sumApprovedByDateRange(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
}