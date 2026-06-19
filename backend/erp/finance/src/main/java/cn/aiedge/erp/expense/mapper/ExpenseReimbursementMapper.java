package cn.aiedge.erp.expense.mapper;

import cn.aiedge.erp.expense.model.ExpenseReimbursement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExpenseReimbursementMapper extends BaseMapper<ExpenseReimbursement> {

    @Select("SELECT * FROM expense_reimbursement WHERE application_id = #{applicationId} ORDER BY created_at DESC")
    List<ExpenseReimbursement> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM expense_reimbursement WHERE applicant_id = #{applicantId} ORDER BY created_at DESC")
    List<ExpenseReimbursement> findByApplicantId(@Param("applicantId") String applicantId);

    @Select("SELECT * FROM expense_reimbursement WHERE status = #{status} ORDER BY created_at DESC")
    List<ExpenseReimbursement> findByStatus(@Param("status") String status);
}
