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

    /**
     * 列出「已配置启用中记录规则」的模型名（去重）。
     *
     * <p>供数据层拦截器做**零开销短路**：集合为空即「一条规则都没配」，拦截器在解析 SQL、
     * 查会话之前就返回，不影响任何查询的行为。表 0 行时本方法返回空集合。</p>
     */
    @Select("SELECT DISTINCT model_name FROM sys_record_rule WHERE active = true AND deleted = 0")
    List<String> selectActiveModelNames();
}