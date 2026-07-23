package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 支付方式Service接口
 */
public interface PaymentMethodService extends IService<PaymentMethod> {

    /**
     * 启用/停用支付方式
     */
    boolean updateStatus(Long id, Integer status);
}
