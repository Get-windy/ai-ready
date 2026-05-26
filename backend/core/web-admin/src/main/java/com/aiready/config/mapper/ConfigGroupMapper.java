package com.aiready.config.mapper;

import com.aiready.config.entity.ConfigGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配置组Mapper
 */
@Mapper
public interface ConfigGroupMapper extends BaseMapper<ConfigGroup> {
    
    /**
     * 根据组编码查询
     */
    @Select("SELECT * FROM sys_config_group WHERE group_code = #{groupCode} AND deleted = 0")
    ConfigGroup selectByCode(@Param("groupCode") String groupCode);
    
    /**
     * 根据父组ID查询子组
     */
    @Select("SELECT * FROM sys_config_group WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<ConfigGroup> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查询所有顶级组
     */
    @Select("SELECT * FROM sys_config_group WHERE parent_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<ConfigGroup> selectRootGroups();
    
    /**
     * 查询所有启用的组
     */
    @Select("SELECT * FROM sys_config_group WHERE status = 1 AND deleted = 0 ORDER BY sort_order")
    List<ConfigGroup> selectAllActive();
    
    /**
     * 检查组编码是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_config_group WHERE group_code = #{groupCode} AND deleted = 0")
    Integer existsByCode(@Param("groupCode") String groupCode);
    
    /**
     * 统计组下的配置数量
     */
    @Select("SELECT COUNT(*) FROM sys_system_config WHERE group_id = #{groupId} AND deleted = 0")
    Integer countConfigs(@Param("groupId") Long groupId);
}
