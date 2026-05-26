package com.aiready.system.service;

import com.aiready.system.entity.SystemLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志服务接口
 */
public interface SystemLogService extends IService<SystemLog> {

    /**
     * 根据日志类型查询
     */
    List<SystemLog> listByType(Integer logType);

    /**
     * 根据用户ID查询
     */
    List<SystemLog> listByUserId(Long userId);

    /**
     * 根据时间范围查询
     */
    List<SystemLog> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据模块名称查询
     */
    List<SystemLog> listByModule(String moduleName);

    /**
     * 清理指定日期之前的日志
     */
    boolean cleanLogsBefore(LocalDateTime beforeTime);

    /**
     * 根据业务类型和主键查询
     */
    List<SystemLog> listByBusiness(String businessType, String businessKey);

    /**
     * 记录操作日志
     */
    boolean recordOperationLog(SystemLog log);

    /**
     * 记录登录日志
     */
    boolean recordLoginLog(Long userId, String username, String ipAddress, Integer status, String errorMsg);

    /**
     * 记录异常日志
     */
    boolean recordExceptionLog(String title, String method, String errorMsg, String requestParams);

    /**
     * 获取用户最近的操作日志
     */
    List<SystemLog> getRecentLogsByUser(Long userId, Integer limit);

    /**
     * 统计指定时间范围内的日志数量
     */
    Long countByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定模块的操作次数
     */
    Long countByModule(String moduleName);

    /**
     * 批量删除日志
     */
    boolean batchDelete(List<Long> ids);
}
