package cn.aiedge.dms.config.mapper;

import cn.aiedge.dms.config.entity.DmsConfig;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * DMS 配置 Mapper
 */
@Mapper
public interface DmsConfigMapper extends BaseMapper<DmsConfig> {

    /**
     * 查询「全局默认」配置（tenant_id = 0）
     *
     * 全局配置是全租户共享的默认值，不能受当前会话租户过滤（否则租户用户读不到默认值），
     * 故显式关闭多租户插件（@InterceptorIgnore）；本方法只读、且只返回 tenant_id=0 的行，无越权风险。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, config_key, config_value, config_desc, scope, "
            + "create_time, update_time, create_by, update_by, deleted, version "
            + "FROM dms_config WHERE deleted = 0 AND tenant_id = 0 ORDER BY config_key")
    List<DmsConfig> selectGlobalConfigs();

    /**
     * 查询指定租户的配置（显式指定租户，供「租户覆盖全局」的合并读取使用）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, config_key, config_value, config_desc, scope, "
            + "create_time, update_time, create_by, update_by, deleted, version "
            + "FROM dms_config WHERE deleted = 0 AND tenant_id = #{tenantId} ORDER BY config_key")
    List<DmsConfig> selectByTenantId(@Param("tenantId") Long tenantId);
}
