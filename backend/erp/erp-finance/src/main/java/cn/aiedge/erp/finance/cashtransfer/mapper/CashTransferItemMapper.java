package cn.aiedge.erp.finance.cashtransfer.mapper;

import cn.aiedge.erp.finance.cashtransfer.entity.CashTransferItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提存转入账户明细 Mapper
 */
@Mapper
public interface CashTransferItemMapper extends BaseMapper<CashTransferItem> {
}
