package cn.aiedge.erp.finance.reconciliation.mapper;

import cn.aiedge.erp.finance.reconciliation.entity.ReconciliationItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账明细项Mapper
 */
@Mapper
public interface ReconciliationItemMapper extends BaseMapper<ReconciliationItem> {
}
