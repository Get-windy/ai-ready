package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.mapper.PaymentMethodMapper;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import cn.aiedge.erp.finance.service.PaymentMethodService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 支付方式Service实现类
 */
@Service
public class PaymentMethodServiceImpl extends ServiceImpl<PaymentMethodMapper, PaymentMethod> implements PaymentMethodService {

    @Override
    public boolean updateStatus(Long id, Integer status) {
        PaymentMethod entity = new PaymentMethod();
        entity.setId(id);
        entity.setStatus(status);
        return updateById(entity);
    }
}
