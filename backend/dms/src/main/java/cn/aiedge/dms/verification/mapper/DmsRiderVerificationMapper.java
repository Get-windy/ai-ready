package cn.aiedge.dms.verification.mapper;

import cn.aiedge.dms.verification.entity.DmsRiderVerification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 骑手实名认证（KYC）台账 Mapper
 */
@Mapper
public interface DmsRiderVerificationMapper extends BaseMapper<DmsRiderVerification> {
}
