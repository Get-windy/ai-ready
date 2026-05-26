package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BankUnmatchedItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BankUnmatchedItemMapper extends BaseMapper<BankUnmatchedItem> {
    
    @Select("SELECT * FROM finance_bank_unmatched_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND reconciliation_id = #{reconciliationId} ORDER BY item_date")
    List<BankUnmatchedItem> listByReconciliationId(@Param("tenantId") Long tenantId, @Param("reconciliationId") Long reconciliationId);
    
    @Select("SELECT * FROM finance_bank_unmatched_item WHERE tenant_id = #{tenantId} AND deleted = 0 AND account_id = #{accountId} AND item_type = #{itemType} ORDER BY item_date")
    List<BankUnmatchedItem> listByAccountIdAndType(@Param("tenantId") Long tenantId, @Param("accountId") Long accountId, @Param("itemType") String itemType);
}