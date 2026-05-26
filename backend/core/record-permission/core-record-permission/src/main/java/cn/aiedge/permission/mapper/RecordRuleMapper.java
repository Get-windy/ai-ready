package cn.aiedge.permission.mapper;

import cn.aiedge.permission.entity.RecordRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecordRuleMapper extends BaseMapper<RecordRule> {

    @Select("SELECT * FROM sys_record_rule WHERE model_name = #{modelName} AND active = true AND deleted = 0")
    List<RecordRule> selectByModel(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_record_rule WHERE model_name = #{modelName} AND user_id = #{userId} AND active = true AND deleted = 0")
    List<RecordRule> selectByModelAndUser(@Param("modelName") String modelName, @Param("userId") Long userId);

    @Select("SELECT * FROM sys_record_rule WHERE model_name = #{modelName} AND group_id = #{groupId} AND active = true AND deleted = 0")
    List<RecordRule> selectByModelAndGroup(@Param("modelName") String modelName, @Param("groupId") Long groupId);

    @Select("SELECT * FROM sys_record_rule WHERE model_name = #{modelName} AND global = true AND active = true AND deleted = 0")
    List<RecordRule> selectGlobalRules(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_record_rule WHERE user_id = #{userId} AND active = true AND deleted = 0")
    List<RecordRule> selectByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM sys_record_rule WHERE group_id IN (${groupIds}) AND active = true AND deleted = 0")
    List<RecordRule> selectByGroups(@Param("groupIds") String groupIds);
}