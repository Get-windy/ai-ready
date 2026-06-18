package cn.aiedge.automation.mapper;

import cn.aiedge.automation.entity.AutomationRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AutomationRuleMapper extends BaseMapper<AutomationRule> {

    @Select("SELECT * FROM sys_automation_rule WHERE model_name = #{modelName} AND trigger_type = #{triggerType} AND active = true AND deleted = 0 ORDER BY priority DESC")
    List<AutomationRule> selectByModelAndTrigger(@Param("modelName") String modelName, @Param("triggerType") String triggerType);

    @Select("SELECT * FROM sys_automation_rule WHERE model_name = #{modelName} AND active = true AND deleted = 0 ORDER BY priority DESC")
    List<AutomationRule> selectByModel(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_automation_rule WHERE trigger_type = 'ON_TIME' AND active = true AND deleted = 0")
    List<AutomationRule> selectScheduledRules();
}