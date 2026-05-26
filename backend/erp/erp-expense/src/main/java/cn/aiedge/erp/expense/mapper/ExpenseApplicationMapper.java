package cn.aiedge.erp.expense.mapper;

import cn.aiedge.erp.expense.model.ExpenseApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExpenseApplicationMapper extends BaseMapper<ExpenseApplication> {

    @Select("SELECT * FROM expense_application WHERE application_number = #{applicationNumber}")
    ExpenseApplication findByApplicationNumber(@Param("applicationNumber") String applicationNumber);

    @Select("SELECT * FROM expense_application WHERE applicant_id = #{applicantId} ORDER BY application_date DESC")
    List<ExpenseApplication> findByApplicantId(@Param("applicantId") Long applicantId);

    @Select("SELECT * FROM expense_application WHERE status = #{status} ORDER BY application_date DESC")
    List<ExpenseApplication> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM expense_application WHERE application_date BETWEEN #{startDate} AND #{endDate} ORDER BY application_date DESC")
    List<ExpenseApplication> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM expense_application WHERE department = #{department} ORDER BY application_date DESC")
    List<ExpenseApplication> findByDepartment(@Param("department") String department);

    @Select("SELECT SUM(total_amount) FROM expense_application WHERE status = 'APPROVED' AND application_date BETWEEN #{startDate} AND #{endDate}")
    BigDecimal sumApprovedByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT SUM(total_amount) FROM expense_application WHERE applicant_id = #{applicantId} AND status = 'APPROVED'")
    BigDecimal sumApprovedByApplicant(@Param("applicantId") Long applicantId);

    @Select("SELECT COUNT(*) FROM expense_application WHERE status IN ('DRAFT', 'SUBMITTED', 'PENDING_APPROVAL')")
    Integer countPending();

    @Select("SELECT COUNT(*) FROM expense_application WHERE applicant_id = #{applicantId} AND status IN ('DRAFT', 'SUBMITTED', 'PENDING_APPROVAL')")
    Integer countPendingByApplicant(@Param("applicantId") Long applicantId);
}