package cn.aiedge.base.log.service;

import cn.aiedge.base.log.entity.SystemLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统日志服务接口
 * 提供日志记录、查询、统计等功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SystemLogService extends IService<SystemLog> {

    /**
     * 保存日志
     * 
     * @param log 日志对象
     */
    void saveLog(SystemLog log);

    /**
     * 异步保存日志
     * 
     * @param log 日志对象
     */
    void saveLogAsync(SystemLog log);

    /**
     * 分页查询日志
     * 
     * @param page 分页参数
     * @param logType 日志类型
     * @param userId 用户ID
     * @param moduleName 模块名称
     * @param status 操作状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    Page<SystemLog> pageLogs(Page<SystemLog> page, Integer logType, Long userId, String moduleName, 
                            Integer status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据ID获取日志详情
     * 
     * @param id 日志ID
     * @return 日志对象
     */
    SystemLog getLogDetail(Long id);

    /**
     * 根据用户ID获取最近日志
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 日志列表
     */
    List<SystemLog> getRecentLogsByUser(Long userId, int limit);

    /**
     * 获取模块统计
     * 
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> getModuleStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户操作统计
     * 
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 统计结果
     */
    List<Map<String, Object>> getUserStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, int limit);

    /**
     * 获取操作类型统计
     * 
     * @param tenantId 租户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> getOperationTypeStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出日志
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param moduleName 模块名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<SystemLog> exportLogs(Long tenantId, Long userId, String moduleName, 
                              LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理历史日志
     * 
     * @param days 保留天数
     * @return 删除记录数
     */
    Integer cleanLogs(int days);

    /**
     * 删除指定日志
     * 
     * @param id 日志ID
     */
    void deleteLog(Long id);

    /**
     * 批量删除日志
     * 
     * @param ids 日志ID列表
     */
    void batchDeleteLogs(List<Long> ids);
}