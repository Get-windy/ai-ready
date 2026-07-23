package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.AccountingPeriod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 会计期间Mapper接口
 */
@Mapper
public interface AccountingPeriodMapper extends BaseMapper<AccountingPeriod> {

    /**
     * 根据租户与期间编码查询
     */
    @Select("SELECT * FROM fin_accounting_period WHERE tenant_id = #{tenantId} AND period_code = #{periodCode} AND deleted_flag = 0")
    Optional<AccountingPeriod> findByPeriodCode(Long tenantId, String periodCode);
}
