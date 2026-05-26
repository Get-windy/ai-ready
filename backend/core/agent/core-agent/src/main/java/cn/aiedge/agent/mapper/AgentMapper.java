package cn.aiedge.agent.mapper;

import cn.aiedge.agent.entity.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Agent Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

    /**
     * 根据Agent编码查询
     */
    @Select("SELECT * FROM ai_agent WHERE agent_code = #{agentCode} AND deleted = 0")
    Agent selectByAgentCode(@Param("agentCode") String agentCode);

    /**
     * 根据API Key查询
     */
    @Select("SELECT * FROM ai_agent WHERE api_key = #{apiKey} AND deleted = 0")
    Agent selectByApiKey(@Param("apiKey") String apiKey);

    /**
     * 查询活跃Agent列表
     */
    @Select("SELECT * FROM ai_agent WHERE status = 1 AND deleted = 0")
    List<Agent> selectActiveAgents();

    @Update("UPDATE ai_agent SET invoke_count = invoke_count + 1 WHERE agent_code = #{agentCode}")
    int incrementInvokeCount(@Param("agentCode") String agentCode);

    @Update("UPDATE ai_agent SET last_heartbeat = CURRENT_TIMESTAMP WHERE agent_code = #{agentCode}")
    int updateHeartbeat(@Param("agentCode") String agentCode);
}