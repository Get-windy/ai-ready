package com.aiready.log.mapper;

import com.aiready.log.entity.OperationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    /**
     * 批量插入日志
     */
    int batchInsert(@Param("logs") List<OperationLog> logs);
    
    /**
     * 清理指定日期之前的日志
     */
    int deleteBeforeDate(@Param("date") LocalDateTime date);
    
    /**
     * 获取日志统计
     */
    Long countByCondition(@Param("module") String module,
                          @Param("operationType") String operationType,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
}
