package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.entity.PaymentChannel;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 支付渠道Service接口
 */
public interface PaymentChannelService extends IService<PaymentChannel> {

    /**
     * 启用/停用支付渠道
     */
    boolean updateStatus(Long id, Integer status);
}
