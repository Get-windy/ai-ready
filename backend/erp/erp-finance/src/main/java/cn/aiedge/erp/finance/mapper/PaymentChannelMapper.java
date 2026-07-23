package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付渠道Mapper接口
 */
@Mapper
public interface PaymentChannelMapper extends BaseMapper<PaymentChannel> {
}
