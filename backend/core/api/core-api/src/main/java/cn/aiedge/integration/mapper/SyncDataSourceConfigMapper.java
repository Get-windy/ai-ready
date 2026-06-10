package cn.aiedge.integration.mapper;

import cn.aiedge.integration.model.SyncDataSourceConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 同步数据源配置 Mapper
 */
@Mapper
public interface SyncDataSourceConfigMapper extends BaseMapper<SyncDataSourceConfig> {

    /**
     * 根据租户ID查询启用的配置
     */
    @Select("SELECT * FROM sync_data_source WHERE tenant_id = #{tenantId} AND status = 1 AND deleted = 0")
    List<SyncDataSourceConfig> selectEnabledByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户和来源类型查询配置
     */
    @Select("SELECT * FROM sync_data_source WHERE tenant_id = #{tenantId} AND source_type = #{sourceType} AND deleted = 0")
    SyncDataSourceConfig selectByTenantAndSource(@Param("tenantId") Long tenantId, @Param("sourceType") String sourceType);

    /**
     * 查询所有启用的配置（供同步引擎使用）
     */
    @Select("SELECT * FROM sync_data_source WHERE status = 1 AND deleted = 0")
    List<SyncDataSourceConfig> selectAllEnabled();
}
