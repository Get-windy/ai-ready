package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysProjectConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目配置Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysProjectConfigMapper extends BaseMapper<SysProjectConfig> {

    /**
     * 根据配置键获取配置值
     */
    @Select("SELECT config_value FROM sys_project_config WHERE tenant_id = #{tenantId} AND config_key = #{configKey} AND deleted = 0 AND status = 0")
    String getConfigValue(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

    /**
     * 根据分组获取配置列表
     */
    @Select("SELECT * FROM sys_project_config WHERE tenant_id = #{tenantId} AND config_group = #{configGroup} AND deleted = 0 ORDER BY id")
    List<SysProjectConfig> selectByGroup(@Param("tenantId") Long tenantId, @Param("configGroup") String configGroup);

    /**
     * 获取所有配置
     */
    @Select("SELECT * FROM sys_project_config WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY config_group, id")
    List<SysProjectConfig> selectAllConfigs(@Param("tenantId") Long tenantId);
}