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
     * 根据配置键获取配置值（**读生效值**：status = 0 才认）。
     *
     * <p>⚠️ 租户由调用方明确传入，本方法**绝不回落**。历史缺陷（2026-09-21 专项）：
     * 服务层曾把租户写死成常量 1，于是每个租户读到的都是租户 1 的配置。</p>
     */
    @Select("SELECT config_value FROM sys_project_config WHERE tenant_id = #{tenantId} AND config_key = #{configKey} AND deleted = 0 AND status = 0 ORDER BY id DESC LIMIT 1")
    String getConfigValue(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

    /**
     * 按「租户 + 配置键」取一行（**不筛 status**：供配置管理页的写入路径判断行是否已存在）。
     *
     * <p>与 {@link #getConfigValue} 的分工：那个读的是「生效值」（status = 0 才认），
     * 这个读的是「管理对象」—— 停用的行也是一行，保存时应当在它上面更新并恢复启用，
     * 而不是当成不存在再插一行出来（否则同一键会在同租户下堆出多行）。</p>
     *
     * <p>⚠️ 调用方必须传**明确的**租户，本方法绝不回落平台行：用「回落」的查询去找
     * 「已存在行」，会让本租户没配过时把平台那一行当成自己的并覆盖掉。</p>
     */
    @Select("SELECT * FROM sys_project_config WHERE tenant_id = #{tenantId} AND config_key = #{configKey} AND deleted = 0 ORDER BY id DESC LIMIT 1")
    SysProjectConfig selectRowByTenant(@Param("tenantId") Long tenantId, @Param("configKey") String configKey);

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