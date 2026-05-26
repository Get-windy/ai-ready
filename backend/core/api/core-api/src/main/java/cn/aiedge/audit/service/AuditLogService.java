package cn.aiedge.audit.service;

import cn.aiedge.audit.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface AuditLogService {

    /**
     * 记录审计日志
     *
     * @param log 审计日志
     * @return 日志ID
     */
    Long record(AuditLog log);

    /**
     * 异步记录审计日志
     *
     * @param log 审计日志
     */
    void recordAsync(AuditLog log);

    /**
     * 查询审计日志
     *
     * @param tenantId   租户ID
     * @param auditType  审计类型（可选）
     * @param module     模块（可选）
     * @param userId     用户ID（可选）
     * @param startTime  开始时间（可选）
     * @param endTime    结束时间（可选）
     * @param page       页码
     * @param pageSize   每页大小
     * @return 日志列表
     */
    Map<String, Object> query(Long tenantId, String auditType, String module,
                               Long userId, LocalDateTime startTime, LocalDateTime endTime,
                               int page, int pageSize);

    /**
     * 获取审计日志详情
     *
     * @param logId 日志ID
     * @return 审计日志
     */
    AuditLog getDetail(Long logId);

    /**
     * 获取用户操作历史
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 日志列表
     */
    List<AuditLog> getUserHistory(Long userId, int limit);

    /**
     * 获取对象操作历史
     *
     * @param targetType 对象类型
     * @param targetId   对象ID
     * @return 日志列表
     */
    List<AuditLog> getObjectHistory(String targetType, String targetId);

    /**
     * 获取审计统计
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果
     */
    Map<String, Object> getStatistics(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 导出审计日志
     *
     * @param tenantId   租户ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 日志列表
     */
    List<AuditLog> exportLogs(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理历史日志
     *
     * @param days 保留天数
     * @return 删除数量
     */
    int cleanLogs(int days);
}
