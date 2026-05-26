package com.aiready.config.mapper;

import com.aiready.config.entity.ConfigAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置审计日志Mapper
 */
@Mapper
public interface ConfigAuditLogMapper extends BaseMapper<ConfigAuditLog> {
    
    /**
     * 查询配置的审计日志
     */
    @Select("SELECT * FROM sys_config_audit_log WHERE config_id = #{configId} ORDER BY operation_time DESC")
    List<ConfigAuditLog> selectByConfigId(@Param("configId") Long configId);
    
    /**
     * 根据操作类型查询
     */
    @Select("SELECT * FROM sys_config_audit_log WHERE operation_type = #{operationType} ORDER BY operation_time DESC LIMIT #{limit}")
    List<ConfigAuditLog> selectByOperationType(@Param("operationType") String operationType, @Param("limit") Integer limit);
    
    /**
     * 查询时间范围内的日志
     */
    @Select("SELECT * FROM sys_config_audit_log WHERE operation_time BETWEEN #{startTime} AND #{endTime} ORDER BY operation_time DESC")
    List<ConfigAuditLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计操作次数
     */
    @Select("SELECT COUNT(*) FROM sys_config_audit_log WHERE operation_type = #{operationType} AND operation_time BETWEEN #{startTime} AND #{endTime}")
    Integer countByOperationTypeAndTime(@Param("operationType") String operationType, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
}
