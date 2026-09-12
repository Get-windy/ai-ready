package cn.aiedge.erp.finance.expensedoc.mapper;

import cn.aiedge.erp.finance.expensedoc.entity.ExpenseItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 费用项明细 Mapper
 */
@Mapper
public interface ExpenseItemMapper extends BaseMapper<ExpenseItem> {
}
