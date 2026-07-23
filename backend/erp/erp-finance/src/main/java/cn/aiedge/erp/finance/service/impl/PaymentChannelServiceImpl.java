package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.PaymentChannelMapper;
import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import cn.aiedge.erp.finance.service.PaymentChannelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 支付渠道Service实现类
 */
@Service
public class PaymentChannelServiceImpl extends ServiceImpl<PaymentChannelMapper, PaymentChannel> implements PaymentChannelService {

    @Override
    public boolean updateStatus(Long id, Integer status) {
        PaymentChannel entity = new PaymentChannel();
        entity.setId(id);
        entity.setStatus(status);
        return updateById(entity);
    }
}
