package cn.aiedge.erp.finance.reconciliation.mapper;

import cn.aiedge.erp.finance.reconciliation.entity.Reconciliation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账记录Mapper
 */
@Mapper
public interface ReconciliationMapper extends BaseMapper<Reconciliation> {
}
