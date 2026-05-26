package cn.aiedge.base.log.mapper;

import cn.aiedge.base.log.entity.SystemLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统日志映射器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SystemLogMapper extends BaseMapper<SystemLog> {

    /**
     * 获取模块统计
     *
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    @Select("SELECT module_name as name, COUNT(*) as count FROM sys_system_log " +
            "WHERE tenant_id = #{tenantId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "AND deleted = 0 GROUP BY module_name ORDER BY count DESC")
    List<Map<String, Object>> getModuleStats(@Param("tenantId") Long tenantId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 获取用户操作统计
     *
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    @Select("SELECT username, COUNT(*) as count FROM sys_system_log " +
            "WHERE tenant_id = #{tenantId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "AND deleted = 0 AND username IS NOT NULL GROUP BY username ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> getUserStats(@Param("tenantId") Long tenantId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("limit") int limit);

    /**
     * 获取操作类型统计
     *
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    @Select("SELECT operation_type as type, COUNT(*) as count FROM sys_system_log " +
            "WHERE tenant_id = #{tenantId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "AND deleted = 0 GROUP BY operation_type ORDER BY count DESC")
    List<Map<String, Object>> getOperationTypeStats(@Param("tenantId") Long tenantId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);
}