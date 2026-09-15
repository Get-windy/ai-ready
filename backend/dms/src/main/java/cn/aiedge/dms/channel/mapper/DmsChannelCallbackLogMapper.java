package cn.aiedge.dms.channel.mapper;

import cn.aiedge.dms.channel.entity.DmsChannelCallbackLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渠道回调日志 Mapper
 */
@Mapper
public interface DmsChannelCallbackLogMapper extends BaseMapper<DmsChannelCallbackLog> {
}
