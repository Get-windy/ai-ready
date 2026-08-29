package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryBalance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 辅助核算余额Mapper接口
 */
@Mapper
public interface FinanceAuxiliaryBalanceMapper extends BaseMapper<FinanceAuxiliaryBalance> {

    /**
     * 查询指定期间、科目、辅助核算类型的余额
     */
    @Select("SELECT * FROM finance_auxiliary_balance WHERE accounting_period_id = #{accountingPeriodId} AND subject_id = #{subjectId} AND auxiliary_type_id = #{auxiliaryTypeId} AND auxiliary_item_id = #{auxiliaryItemId} AND tenant_id = #{tenantId} AND deleted_flag = 0")
    FinanceAuxiliaryBalance findBalance(Long accountingPeriodId, Long subjectId, Long auxiliaryTypeId, Long auxiliaryItemId, Long tenantId);

    /**
     * 查询指定期间的所有辅助核算余额
     */
    @Select("SELECT * FROM finance_auxiliary_balance WHERE accounting_period_id = #{accountingPeriodId} AND tenant_id = #{tenantId} AND deleted_flag = 0 ORDER BY auxiliary_type_id, auxiliary_item_id")
    List<FinanceAuxiliaryBalance> findByPeriodId(Long accountingPeriodId, Long tenantId);
}
