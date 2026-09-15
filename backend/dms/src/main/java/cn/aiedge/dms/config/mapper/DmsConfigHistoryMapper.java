package cn.aiedge.dms.config.mapper;

import cn.aiedge.dms.config.entity.DmsConfigHistory;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配置变更审计 Mapper
 */
@Mapper
public interface DmsConfigHistoryMapper extends BaseMapper<DmsConfigHistory> {

    /**
     * 查询某配置键的历史（全局默认行 tenant_id=0 + 指定租户，按时间倒序）
     *
     * 与配置读取同口径：租户视角应能看到全局默认键的变更历史，故显式关闭多租户过滤（只读、按 key 精确过滤）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, tenant_id, config_key, old_value, new_value, secret, change_type, "
            + "operator_id, operator_name, client_ip, change_time, remark "
            + "FROM dms_config_history "
            + "WHERE config_key = #{configKey} AND tenant_id IN (0, #{tenantId}) "
            + "ORDER BY change_time DESC, id DESC LIMIT #{limit}")
    List<DmsConfigHistory> selectByKey(@Param("configKey") String configKey,
                                       @Param("tenantId") Long tenantId,
                                       @Param("limit") int limit);

    /** 取某条历史（含明文列，仅服务端回滚使用） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM dms_config_history WHERE id = #{id}")
    DmsConfigHistory selectByIdWithCipher(@Param("id") Long id);
}
