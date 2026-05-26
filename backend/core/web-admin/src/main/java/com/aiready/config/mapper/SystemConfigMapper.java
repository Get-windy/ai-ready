package com.aiready.config.mapper;

import com.aiready.config.entity.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    
    /**
     * 根据配置键查询
     */
    @Select("SELECT * FROM sys_system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig selectByKey(@Param("configKey") String configKey);
    
    /**
     * 根据配置组查询
     */
    @Select("SELECT * FROM sys_system_config WHERE group_id = #{groupId} AND deleted = 0 ORDER BY sort_order")
    List<SystemConfig> selectByGroupId(@Param("groupId") Long groupId);
    
    /**
     * 根据配置组编码查询
     */
    @Select("SELECT * FROM sys_system_config WHERE group_code = #{groupCode} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SystemConfig> selectByGroupCode(@Param("groupCode") String groupCode);
    
    /**
     * 查询所有启用的配置
     */
    @Select("SELECT * FROM sys_system_config WHERE status = 1 AND deleted = 0")
    List<SystemConfig> selectAllActive();
    
    /**
     * 检查配置键是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_system_config WHERE config_key = #{configKey} AND deleted = 0")
    Integer existsByKey(@Param("configKey") String configKey);
    
    /**
     * 更新配置值
     */
    @Update("UPDATE sys_system_config SET config_value = #{value}, update_time = NOW() WHERE config_key = #{configKey}")
    int updateValueByKey(@Param("configKey") String configKey, @Param("value") String value);
    
    /**
     * 获取最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM sys_system_config WHERE group_id = #{groupId}")
    Integer getMaxSortOrder(@Param("groupId") Long groupId);
}
