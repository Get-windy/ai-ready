package cn.aiedge.report.service;

import cn.aiedge.report.model.ReportData;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 报表统计分析服务接口
 * 提供同比、环比、趋势分析等高级统计功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ReportAnalyticsService {

    /**
     * 同比分析
     * 比较当前周期与上年同期的数据变化
     *
     * @param reportId    报表ID
     * @param field       分析字段
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param tenantId    租户ID
     * @return 同比分析结果
     */
    YoYResult yearOverYearAnalysis(String reportId, String field, 
                                    LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * 环比分析
     * 比较当前周期与上一周期的数据变化
     *
     * @param reportId    报表ID
     * @param field       分析字段
     * @param period      周期类型：day/week/month/quarter
     * @param currentPeriod 当前周期起始日期
     * @param tenantId    租户ID
     * @return 环比分析结果
     */
    MoMResult monthOverMonthAnalysis(String reportId, String field, 
                                      String period, LocalDate currentPeriod, Long tenantId);

    /**
     * 趋势分析
     * 分析指定时间段内的数据趋势
     *
     * @param reportId    报表ID
     * @param field       分析字段
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param granularity 粒度：day/week/month
     * @param tenantId    租户ID
     * @return 趋势分析结果
     */
    TrendResult trendAnalysis(String reportId, String field,
                               LocalDate startDate, LocalDate endDate, 
                               String granularity, Long tenantId);

    /**
     * 排名分析
     * 按指定维度进行排名
     *
     * @param reportId    报表ID
     * @param rankField   排名字段
     * @param groupField  分组字段
     * @param topN        返回前N名
     * @param ascending   是否升序（false为降序）
     * @param tenantId    租户ID
     * @return 排名分析结果
     */
    RankingResult rankingAnalysis(String reportId, String rankField, 
                                   String groupField, int topN, 
                                   boolean ascending, Long tenantId);

    /**
     * 占比分析
     * 分析各部分占总体的比例
     *
     * @param reportId    报表ID
     * @param valueField  值字段
     * @param groupField  分组字段
     * @param tenantId    租户ID
     * @return 占比分析结果
     */
    ProportionResult proportionAnalysis(String reportId, String valueField,
                                         String groupField, Long tenantId);

    /**
     * 分布分析
     * 分析数据的分布情况
     *
     * @param reportId    报表ID
     * @param field       分析字段
     * @param buckets     分桶数量
     * @param tenantId    租户ID
     * @return 分布分析结果
     */
    DistributionResult distributionAnalysis(String reportId, String field,
                                              int buckets, Long tenantId);

    /**
     * 异常检测
     * 检测数据中的异常值
     *
     * @param reportId    报表ID
     * @param field       分析字段
     * @param sensitivity 敏感度：low/medium/high
     * @param tenantId    租户ID
     * @return 异常检测结果
     */
    AnomalyResult anomalyDetection(String reportId, String field,
                                    String sensitivity, Long tenantId);

    /**
     * 综合统计报告
     * 生成包含多种分析的综合报告
     *
     * @param reportId    报表ID
     * @param fields      分析字段列表
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param tenantId    租户ID
     * @return 综合分析报告
     */
    ComprehensiveReport generateComprehensiveReport(String reportId, List<String> fields,
                                                      LocalDate startDate, LocalDate endDate,
                                                      Long tenantId);

    // ==================== 结果类 ====================

    /**
     * 同比分析结果
     */
    record YoYResult(
        String field,
        double currentValue,
        double previousValue,
        double changeAmount,
        double changeRate,
        String period,
        String previousPeriod
    ) {}

    /**
     * 环比分析结果
     */
    record MoMResult(
        String field,
        double currentValue,
        double previousValue,
        double changeAmount,
        double changeRate,
        String periodType,
        String currentPeriod,
        String previousPeriod
    ) {}

    /**
     * 趋势分析结果
     */
    record TrendResult(
        String field,
        String granularity,
        List<DataPoint> dataPoints,
        String trend,           // up/down/stable
        double slope,           // 趋势斜率
        double r2,              // 拟合优度
        String forecast         // 预测描述
    ) {
        public record DataPoint(
            String label,
            double value,
            LocalDate date
        ) {}
    }

    /**
     * 排名分析结果
     */
    record RankingResult(
        String rankField,
        String groupField,
        List<RankItem> rankings,
        int totalItems
    ) {
        public record RankItem(
            int rank,
            String groupValue,
            double value,
            double percentage
        ) {}
    }

    /**
     * 占比分析结果
     */
    record ProportionResult(
        String valueField,
        String groupField,
        double total,
        List<ProportionItem> items
    ) {
        public record ProportionItem(
            String groupValue,
            double value,
            double percentage,
            String displayPercentage
        ) {}
    }

    /**
     * 分布分析结果
     */
    record DistributionResult(
        String field,
        double min,
        double max,
        double mean,
        double median,
        double stdDev,
        List<Bucket> buckets
    ) {
        public record Bucket(
            double lowerBound,
            double upperBound,
            int count,
            double percentage
        ) {}
    }

    /**
     * 异常检测结果
     */
    record AnomalyResult(
        String field,
        double mean,
        double stdDev,
        double lowerThreshold,
        double upperThreshold,
        List<AnomalyPoint> anomalies,
        int totalChecked,
        int anomalyCount
    ) {
        public record AnomalyPoint(
            int rowIndex,
            double value,
            double zScore,
            String severity     // low/medium/high
        ) {}
    }

    /**
     * 综合分析报告
     */
    record ComprehensiveReport(
        String reportId,
        String reportName,
        LocalDate startDate,
        LocalDate endDate,
        Map<String, Object> summary,
        Map<String, YoYResult> yoyResults,
        Map<String, MoMResult> momResults,
        Map<String, TrendResult> trends,
        List<RankingResult> rankings,
        List<AnomalyPoint> anomalies,
        String generatedAt,
        String recommendations
    ) {}
}
