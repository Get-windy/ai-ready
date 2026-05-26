package cn.aiedge.agent.mapper;

import cn.aiedge.agent.entity.AgentCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent调用审计日志Mapper
 */
@Mapper
public interface AgentCallLogMapper extends BaseMapper<AgentCallLog> {
}
