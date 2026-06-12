package cn.aiedge.dms.payment.mapper;

import cn.aiedge.dms.payment.entity.DmsPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收款记录 Mapper
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DmsPaymentMapper extends BaseMapper<DmsPayment> {
}
