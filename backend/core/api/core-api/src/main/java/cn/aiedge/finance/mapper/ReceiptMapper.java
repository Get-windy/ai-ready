package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Receipt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收款记录Mapper
 */
@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {
}
