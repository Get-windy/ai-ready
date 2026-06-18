package cn.aiedge.erp.expense.mapper;

import cn.aiedge.erp.expense.model.ExpenseApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExpenseApprovalMapper extends BaseMapper<ExpenseApproval> {

    @Select("SELECT * FROM expense_approval WHERE application_id = #{applicationId} ORDER BY approval_time DESC")
    List<ExpenseApproval> findByApplicationId(@Param("applicationId") Long applicationId);

    @Select("SELECT * FROM expense_approval WHERE approver_id = #{approverId} ORDER BY approval_time DESC")
    List<ExpenseApproval> findByApproverId(@Param("approverId") Long approverId);

    @Select("SELECT * FROM expense_approval WHERE application_id = #{applicationId} AND approval_action = #{approvalAction} ORDER BY approval_time DESC LIMIT 1")
    ExpenseApproval findLatestByApplicationAndAction(@Param("applicationId") Long applicationId, @Param("approvalAction") String approvalAction);

    @Select("SELECT COUNT(*) FROM expense_approval WHERE approver_id = #{approverId} AND approval_action = 'APPROVE'")
    Integer countApprovedByApprover(@Param("approverId") Long approverId);

    @Select("SELECT COUNT(*) FROM expense_approval WHERE approver_id = #{approverId} AND approval_action = 'REJECT'")
    Integer countRejectedByApprover(@Param("approverId") Long approverId);
}