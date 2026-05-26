package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BankStatement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BankStatementMapper extends BaseMapper<BankStatement> {
    
    @Select("SELECT * FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} ORDER BY transaction_date DESC")
    List<BankStatement> listByAccountId(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId);
    
    @Select("SELECT * FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND status = #{status} ORDER BY transaction_date DESC")
    List<BankStatement> listByAccountIdAndStatus(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId, @Param("status") Integer status);
    
    @Select("SELECT * FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND transaction_date BETWEEN #{startDate} AND #{endDate} ORDER BY transaction_date DESC")
    List<BankStatement> listByAccountIdAndDateRange(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId, @Param("startDate") String startDate, @Param("endDate") String endDate);
    
    @Select("SELECT MAX(import_batch) FROM finance_bank_statement WHERE tenant_id = #{tenantId}")
    Integer getMaxImportBatch(@Param("tenantId") Long tenantId);
    
    @Select("SELECT SUM(amount) FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND transaction_type IN (1, 3, 5)")
    BigDecimal sumDepositByAccountId(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId);
    
    @Select("SELECT SUM(amount) FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND transaction_type IN (2, 4, 6)")
    BigDecimal sumWithdrawByAccountId(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId);
    
    @Select("SELECT COUNT(*) FROM finance_bank_statement WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND status = 0")
    Integer countUnmatchedByAccountId(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId);
}