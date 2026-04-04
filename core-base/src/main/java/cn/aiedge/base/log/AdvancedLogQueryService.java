package cn.aiedge.base.log;

import cn.aiedge.base.entity.SysOperLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 高级日志查询服务
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface AdvancedLogQueryService {

    /**
     * 高级日志查询
     *
     * @param request 查询请求
     * @return 查询响应
     */
    LogQueryResponse query(LogQueryRequest request);

    /**
     * 全文检索日志
     *
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @return 日志列表
     */
    Page<SysOperLog> fullTextSearch(String keyword, int page, int pageSize);

    /**
     * 按IP查询日志
     *
     * @param ip       IP地址
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<SysOperLog> queryByIp(String ip, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 按耗时范围查询慢操作日志
     *
     * @param minCost 最小耗时
     * @param maxCost 最大耗时
     * @param limit   限制数量
     * @return 慢操作日志
     */
    List<SysOperLog> querySlowOperations(Long minCost, Long maxCost, int limit);

    /**
     * 查询失败操作日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     限制数量
     * @return 失败日志
     */
    List<SysOperLog> queryFailedOperations(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, int limit);

    /**
     * 获取日志统计摘要
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计摘要
     */
    Map<String, Object> getLogSummary(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    /**
     * 获取操作趋势（按时间维度）
     *
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param timeUnit   时间单位：hour, day, week, month
     * @return 趋势数据
     */
    List<Map<String, Object>> getOperationTrend(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, String timeUnit);

    /**
     * 获取用户活动分析
     *
     * @param userId   用户ID
     * @param days     分析天数
     * @return 活动数据
     */
    Map<String, Object> getUserActivityAnalysis(Long userId, int days);

    /**
     * 检测异常操作模式
     *
     * @param userId 用户ID（可选）
     * @return 异常操作列表
     */
    List<Map<String, Object>> detectAnomalousOperations(Long userId);
}
