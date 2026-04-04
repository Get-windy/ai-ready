package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysOperLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysOperLogService extends IService<SysOperLog> {

    /**
     * 记录操作日志
     *
     * @param log 日志信息
     * @return 日志ID
     */
    Long recordLog(SysOperLog log);

    /**
     * 异步记录操作日志
     *
     * @param log 日志信息
     */
    void recordLogAsync(SysOperLog log);

    /**
     * 分页查询操作日志
     *
     * @param page      分页参数
     * @param tenantId  租户ID
     * @param userId    用户ID（可选）
     * @param module    模块（可选）
     * @param status    状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 分页结果
     */
    Page<SysOperLog> pageLogs(Page<SysOperLog> page, Long tenantId, Long userId,
                              String module, Integer status,
                              LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户最近的操作日志
     *
     * @param userId 用户ID
     * @param limit  数量限制
     * @return 日志列表
     */
    List<SysOperLog> getRecentLogs(Long userId, int limit);

    /**
     * 获取模块统计
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 模块统计
     */
    List<Map<String, Object>> getModuleStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取用户操作统计
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     返回数量
     * @return 用户统计
     */
    List<Map<String, Object>> getUserStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, int limit);

    /**
     * 清理历史日志
     *
     * @param days 保留天数
     * @return 删除数量
     */
    int cleanLogs(int days);

    /**
     * 导出日志
     *
     * @param tenantId  租户ID
     * @param userId    用户ID（可选）
     * @param module    模块（可选）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<SysOperLog> exportLogs(Long tenantId, Long userId, String module,
                                 LocalDateTime startTime, LocalDateTime endTime);
}
