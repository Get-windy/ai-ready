package cn.aiedge.erp.finance.expensedoc.mapper;

import cn.aiedge.erp.finance.expensedoc.entity.ExpenseApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用审批记录 Mapper
 */
@Mapper
public interface ExpenseApprovalMapper extends BaseMapper<ExpenseApproval> {
}
