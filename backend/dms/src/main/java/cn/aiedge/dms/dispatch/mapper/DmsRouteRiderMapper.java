package cn.aiedge.dms.dispatch.mapper;

import cn.aiedge.dms.dispatch.entity.DmsRouteRider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线路-配送员绑定 Mapper（智能调度「区域分包」）
 *
 * @author AI-Ready Team
 */
@Mapper
public interface DmsRouteRiderMapper extends BaseMapper<DmsRouteRider> {
}
