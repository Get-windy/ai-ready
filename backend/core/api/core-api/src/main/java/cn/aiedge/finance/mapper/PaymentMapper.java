package cn.aiedge.finance.mapper;

import cn.aiedge.finance.entity.Payment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款记录Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
