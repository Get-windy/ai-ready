package cn.aiedge.dms.verification.mapper;

import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 核验告警记录 Mapper
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DmsVerificationAlertMapper extends BaseMapper<DmsVerificationAlert> {
}
