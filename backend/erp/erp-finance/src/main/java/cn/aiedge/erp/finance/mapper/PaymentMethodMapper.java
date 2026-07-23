package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付方式Mapper接口
 */
@Mapper
public interface PaymentMethodMapper extends BaseMapper<PaymentMethod> {
}
