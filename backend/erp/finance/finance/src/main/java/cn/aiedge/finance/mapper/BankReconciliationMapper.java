package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BankReconciliation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BankReconciliationMapper extends BaseMapper<BankReconciliation> {
    
    @Select("SELECT * FROM finance_bank_reconciliation WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} ORDER BY reconciliation_date DESC")
    List<BankReconciliation> listByAccountId(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId);
    
    @Select("SELECT * FROM finance_bank_reconciliation WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND period = #{period}")
    BankReconciliation getByAccountIdAndPeriod(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId, @Param("period") String period);
    
    @Select("SELECT * FROM finance_bank_reconciliation WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = #{status} ORDER BY reconciliation_date DESC")
    List<BankReconciliation> listByStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
}