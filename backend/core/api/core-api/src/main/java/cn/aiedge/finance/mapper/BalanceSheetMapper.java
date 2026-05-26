package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.BalanceSheet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产负债表Mapper
 */
@Mapper
public interface BalanceSheetMapper extends BaseMapper<BalanceSheet> {
}
