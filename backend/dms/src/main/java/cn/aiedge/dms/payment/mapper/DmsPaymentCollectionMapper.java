package cn.aiedge.dms.payment.mapper;

import cn.aiedge.dms.payment.entity.DmsPaymentCollection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 未付催收 / 核销流水 Mapper
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DmsPaymentCollectionMapper extends BaseMapper<DmsPaymentCollection> {
}
