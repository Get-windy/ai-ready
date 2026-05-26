package cn.aiedge.base.log;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志查询响应
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Builder
public class LogQueryResponse {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 日志列表
     */
    private List<?> records;

    /**
     * 聚合统计
     */
    private LogAggregation aggregation;

    /**
     * 聚合统计信息
     */
    @Data
    @Builder
    public static class LogAggregation {
        /**
         * 总操作数
         */
        private Long totalCount;

        /**
         * 成功数
         */
        private Long successCount;

        /**
         * 失败数
         */
        private Long failCount;

        /**
         * 平均耗时(ms)
         */
        private Double avgCostTime;

        /**
         * 最大耗时(ms)
         */
        private Long maxCostTime;

        /**
         * 最小耗时(ms)
         */
        private Long minCostTime;

        /**
         * 模块统计
         */
        private List<Map<String, Object>> moduleStats;

        /**
         * 用户统计
         */
        private List<Map<String, Object>> userStats;

        /**
         * 时间趋势（按天/小时）
         */
        private List<Map<String, Object>> timeTrend;
    }
}
