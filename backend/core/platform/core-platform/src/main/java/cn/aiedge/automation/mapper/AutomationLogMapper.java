package cn.aiedge.automation.mapper;

import cn.aiedge.automation.entity.AutomationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AutomationLogMapper extends BaseMapper<AutomationLog> {

    @Select("SELECT * FROM sys_automation_log WHERE model_name = #{modelName} AND record_id = #{recordId} ORDER BY execution_time DESC")
    List<AutomationLog> selectByRecord(@Param("modelName") String modelName, @Param("recordId") Long recordId);

    @Select("SELECT * FROM sys_automation_log WHERE rule_id = #{ruleId} ORDER BY execution_time DESC LIMIT #{limit}")
    List<AutomationLog> selectByRule(@Param("ruleId") Long ruleId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_automation_log WHERE rule_id = #{ruleId} AND success = true")
    int countSuccessByRule(@Param("ruleId") Long ruleId);

    @Select("SELECT COUNT(*) FROM sys_automation_log WHERE rule_id = #{ruleId} AND success = false")
    int countFailedByRule(@Param("ruleId") Long ruleId);
}