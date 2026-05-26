package cn.aiedge.report.service.impl;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.service.ReportAnalyticsService;
import cn.aiedge.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表统计分析服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalyticsServiceImpl implements ReportAnalyticsService {

    private final ReportService reportService;

    @Override
    public YoYResult yearOverYearAnalysis(String reportId, String field,
                                           LocalDate startDate, LocalDate endDate, Long tenantId) {
        log.info("同比分析: reportId={}, field={}, period={}-{}", reportId, field, startDate, endDate);
        
        // 获取当前周期数据
        Map<String, Object> currentParams = new HashMap<>();
        currentParams.put("startDate", startDate.toString());
        currentParams.put("endDate", endDate.toString());
        ReportData currentData = reportService.generateReport(reportId, currentParams, tenantId);
        double currentValue = aggregateField(currentData.getRows(), field);
        
        // 获取上年同期数据
        LocalDate prevStartDate = startDate.minusYears(1);
        LocalDate prevEndDate = endDate.minusYears(1);
        Map<String, Object> prevParams = new HashMap<>();
        prevParams.put("startDate", prevStartDate.toString());
        prevParams.put("endDate", prevEndDate.toString());
        ReportData prevData = reportService.generateReport(reportId, prevParams, tenantId);
        double previousValue = aggregateField(prevData.getRows(), field);
        
        // 计算同比变化
        double changeAmount = currentValue - previousValue;
        double changeRate = previousValue != 0 ? (changeAmount / previousValue) * 100 : 0;
        
        return new YoYResult(
            field,
            currentValue,
            previousValue,
            changeAmount,
            changeRate,
            startDate.getYear() + "年",
            prevStartDate.getYear() + "年"
        );
    }

    @Override
    public MoMResult monthOverMonthAnalysis(String reportId, String field,
                                             String period, LocalDate currentPeriod, Long tenantId) {
        log.info("环比分析: reportId={}, field={}, period={}, current={}", 
                 reportId, field, period, currentPeriod);
        
        // 计算周期范围
        DateRange currentRange = calculatePeriodRange(period, currentPeriod);
        DateRange previousRange = calculatePreviousPeriodRange(period, currentPeriod);
        
        // 获取当前周期数据
        Map<String, Object> currentParams = new HashMap<>();
        currentParams.put("startDate", currentRange.start().toString());
        currentParams.put("endDate", currentRange.end().toString());
        ReportData currentData = reportService.generateReport(reportId, currentParams, tenantId);
        double currentValue = aggregateField(currentData.getRows(), field);
        
        // 获取上一周期数据
        Map<String, Object> prevParams = new HashMap<>();
        prevParams.put("startDate", previousRange.start().toString());
        prevParams.put("endDate", previousRange.end().toString());
        ReportData prevData = reportService.generateReport(reportId, prevParams, tenantId);
        double previousValue = aggregateField(prevData.getRows(), field);
        
        // 计算环比变化
        double changeAmount = currentValue - previousValue;
        double changeRate = previousValue != 0 ? (changeAmount / previousValue) * 100 : 0;
        
        return new MoMResult(
            field,
            currentValue,
            previousValue,
            changeAmount,
            changeRate,
            period,
            formatPeriod(currentRange.start(), currentRange.end()),
            formatPeriod(previousRange.start(), previousRange.end())
        );
    }

    @Override
    public TrendResult trendAnalysis(String reportId, String field,
                                      LocalDate startDate, LocalDate endDate,
                                      String granularity, Long tenantId) {
        log.info("趋势分析: reportId={}, field={}, granularity={}", reportId, field, granularity);
        
        // 获取数据
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate.toString());
        params.put("endDate", endDate.toString());
        ReportData reportData = reportService.generateReport(reportId, params, tenantId);
        
        // 按粒度分组数据
        List<TrendResult.DataPoint> dataPoints = groupByGranularity(
            reportData.getRows(), field, granularity, startDate, endDate
        );
        
        // 计算趋势线（线性回归）
        double[] trendStats = calculateLinearRegression(dataPoints);
        double slope = trendStats[0];
        double r2 = trendStats[1];
        
        // 判断趋势方向
        String trend = slope > 0.05 ? "up" : (slope < -0.05 ? "down" : "stable");
        
        // 生成预测描述
        String forecast = generateForecast(slope, r2, dataPoints);
        
        return new TrendResult(
            field,
            granularity,
            dataPoints,
            trend,
            slope,
            r2,
            forecast
        );
    }

    @Override
    public RankingResult rankingAnalysis(String reportId, String rankField,
                                          String groupField, int topN,
                                          boolean ascending, Long tenantId) {
        log.info("排名分析: reportId={}, rankField={}, groupField={}", reportId, rankField, groupField);
        
        // 获取数据
        ReportData reportData = reportService.generateReport(reportId, new HashMap<>(), tenantId);
        
        // 按分组字段聚合
        Map<String, Double> groupedData = new HashMap<>();
        for (Map<String, Object> row : reportData.getRows()) {
            String groupValue = getStringValue(row.get(groupField));
            double value = getDoubleValue(row.get(rankField));
            groupedData.merge(groupValue, value, Double::sum);
        }
        
        // 排序
        List<Map.Entry<String, Double>> sorted = groupedData.entrySet().stream()
            .sorted((a, b) -> ascending ? 
                    Double.compare(a.getValue(), b.getValue()) : 
                    Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .collect(Collectors.toList());
        
        // 计算总额用于百分比
        double total = groupedData.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // 构建排名结果
        List<RankingResult.RankItem> rankings = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Double> entry : sorted) {
            double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
            rankings.add(new RankingResult.RankItem(
                rank++,
                entry.getKey(),
                entry.getValue(),
                percentage
            ));
        }
        
        return new RankingResult(rankField, groupField, rankings, groupedData.size());
    }

    @Override
    public ProportionResult proportionAnalysis(String reportId, String valueField,
                                                String groupField, Long tenantId) {
        log.info("占比分析: reportId={}, valueField={}, groupField={}", reportId, valueField, groupField);
        
        // 获取数据
        ReportData reportData = reportService.generateReport(reportId, new HashMap<>(), tenantId);
        
        // 按分组字段聚合
        Map<String, Double> groupedData = new LinkedHashMap<>();
        for (Map<String, Object> row : reportData.getRows()) {
            String groupValue = getStringValue(row.get(groupField));
            double value = getDoubleValue(row.get(valueField));
            groupedData.merge(groupValue, value, Double::sum);
        }
        
        // 计算总额
        double total = groupedData.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // 构建占比结果
        List<ProportionResult.ProportionItem> items = new ArrayList<>();
        for (Map.Entry<String, Double> entry : groupedData.entrySet()) {
            double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
            items.add(new ProportionResult.ProportionItem(
                entry.getKey(),
                entry.getValue(),
                percentage,
                String.format("%.2f%%", percentage)
            ));
        }
        
        // 按占比降序排列
        items.sort((a, b) -> Double.compare(b.percentage(), a.percentage()));
        
        return new ProportionResult(valueField, groupField, total, items);
    }

    @Override
    public DistributionResult distributionAnalysis(String reportId, String field,
                                                     int buckets, Long tenantId) {
        log.info("分布分析: reportId={}, field={}, buckets={}", reportId, field, buckets);
        
        // 获取数据
        ReportData reportData = reportService.generateReport(reportId, new HashMap<>(), tenantId);
        
        // 提取数值
        List<Double> values = reportData.getRows().stream()
            .map(row -> getDoubleValue(row.get(field)))
            .filter(v -> !Double.isNaN(v))
            .sorted()
            .collect(Collectors.toList());
        
        if (values.isEmpty()) {
            return new DistributionResult(field, 0, 0, 0, 0, 0, Collections.emptyList());
        }
        
        // 计算基本统计量
        double min = values.get(0);
        double max = values.get(values.size() - 1);
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = calculateMedian(values);
        double stdDev = calculateStdDev(values, mean);
        
        // 创建分桶
        List<DistributionResult.Bucket> bucketList = new ArrayList<>();
        double bucketSize = (max - min) / buckets;
        
        for (int i = 0; i < buckets; i++) {
            double lower = min + i * bucketSize;
            double upper = (i == buckets - 1) ? max : min + (i + 1) * bucketSize;
            
            int count = 0;
            for (double v : values) {
                if (v >= lower && (i == buckets - 1 ? v <= upper : v < upper)) {
                    count++;
                }
            }
            
            double percentage = (double) count / values.size() * 100;
            bucketList.add(new DistributionResult.Bucket(lower, upper, count, percentage));
        }
        
        return new DistributionResult(field, min, max, mean, median, stdDev, bucketList);
    }

    @Override
    public AnomalyResult anomalyDetection(String reportId, String field,
                                           String sensitivity, Long tenantId) {
        log.info("异常检测: reportId={}, field={}, sensitivity={}", reportId, field, sensitivity);
        
        // 获取数据
        ReportData reportData = reportService.generateReport(reportId, new HashMap<>(), tenantId);
        
        // 提取数值
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < reportData.getRows().size(); i++) {
            values.add(getDoubleValue(reportData.getRows().get(i).get(field)));
        }
        
        // 计算均值和标准差
        double mean = values.stream().filter(v -> !Double.isNaN(v))
            .mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStdDev(
            values.stream().filter(v -> !Double.isNaN(v)).collect(Collectors.toList()), 
            mean
        );
        
        // 根据敏感度确定阈值倍数
        double thresholdMultiplier = switch (sensitivity.toLowerCase()) {
            case "low" -> 3.0;
            case "high" -> 1.5;
            default -> 2.0; // medium
        };
        
        double lowerThreshold = mean - thresholdMultiplier * stdDev;
        double upperThreshold = mean + thresholdMultiplier * stdDev;
        
        // 检测异常点
        List<AnomalyResult.AnomalyPoint> anomalies = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            if (Double.isNaN(value)) continue;
            
            double zScore = stdDev > 0 ? Math.abs(value - mean) / stdDev : 0;
            
            if (value < lowerThreshold || value > upperThreshold) {
                String severity = zScore > 3 ? "high" : (zScore > 2 ? "medium" : "low");
                anomalies.add(new AnomalyResult.AnomalyPoint(i, value, zScore, severity));
            }
        }
        
        return new AnomalyResult(
            field, mean, stdDev, lowerThreshold, upperThreshold,
            anomalies, values.size(), anomalies.size()
        );
    }

    @Override
    public ComprehensiveReport generateComprehensiveReport(String reportId, List<String> fields,
                                                            LocalDate startDate, LocalDate endDate,
                                                            Long tenantId) {
        log.info("生成综合报告: reportId={}, fields={}", reportId, fields);
        
        ReportDefinition definition = reportService.getReportDefinition(reportId);
        String reportName = definition != null ? definition.getReportName() : reportId;
        
        // 生成各项分析
        Map<String, YoYResult> yoyResults = new HashMap<>();
        Map<String, MoMResult> momResults = new HashMap<>();
        Map<String, TrendResult> trends = new HashMap<>();
        List<AnomalyResult.AnomalyPoint> allAnomalies = new ArrayList<>();
        
        for (String field : fields) {
            try {
                // 同比分析
                yoyResults.put(field, yearOverYearAnalysis(reportId, field, startDate, endDate, tenantId));
                
                // 环比分析
                momResults.put(field, monthOverMonthAnalysis(reportId, field, "month", startDate, tenantId));
                
                // 趋势分析
                trends.put(field, trendAnalysis(reportId, field, startDate, endDate, "day", tenantId));
                
                // 异常检测
                AnomalyResult anomaly = anomalyDetection(reportId, field, "medium", tenantId);
                allAnomalies.addAll(anomaly.anomalies());
            } catch (Exception e) {
                log.warn("分析字段 {} 时出错: {}", field, e.getMessage());
            }
        }
        
        // 生成汇总
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFields", fields.size());
        summary.put("analysisPeriod", formatPeriod(startDate, endDate));
        summary.put("anomalyCount", allAnomalies.size());
        
        // 生成建议
        String recommendations = generateRecommendations(yoyResults, momResults, allAnomalies);
        
        return new ComprehensiveReport(
            reportId,
            reportName,
            startDate,
            endDate,
            summary,
            yoyResults,
            momResults,
            trends,
            Collections.emptyList(),
            allAnomalies,
            LocalDate.now().format(DateTimeFormatter.ISO_DATE),
            recommendations
        );
    }

    // ==================== 辅助方法 ====================

    private double aggregateField(List<Map<String, Object>> rows, String field) {
        return rows.stream()
            .mapToDouble(row -> getDoubleValue(row.get(field)))
            .sum();
    }

    private double getDoubleValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStringValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private record DateRange(LocalDate start, LocalDate end) {}

    private DateRange calculatePeriodRange(String period, LocalDate date) {
        return switch (period.toLowerCase()) {
            case "day" -> new DateRange(date, date);
            case "week" -> {
                LocalDate start = date.minusDays(date.getDayOfWeek().getValue() - 1);
                yield new DateRange(start, start.plusDays(6));
            }
            case "month" -> new DateRange(
                date.withDayOfMonth(1),
                date.withDayOfMonth(date.lengthOfMonth())
            );
            case "quarter" -> {
                int quarter = (date.getMonthValue() - 1) / 3;
                LocalDate start = date.withMonth(quarter * 3 + 1).withDayOfMonth(1);
                yield new DateRange(start, start.plusMonths(3).minusDays(1));
            }
            default -> new DateRange(date, date);
        };
    }

    private DateRange calculatePreviousPeriodRange(String period, LocalDate date) {
        LocalDate previous = switch (period.toLowerCase()) {
            case "day" -> date.minusDays(1);
            case "week" -> date.minusWeeks(1);
            case "month" -> date.minusMonths(1);
            case "quarter" -> date.minusMonths(3);
            default -> date.minusDays(1);
        };
        return calculatePeriodRange(period, previous);
    }

    private String formatPeriod(LocalDate start, LocalDate end) {
        return start.format(DateTimeFormatter.ISO_DATE) + " ~ " + end.format(DateTimeFormatter.ISO_DATE);
    }

    private List<TrendResult.DataPoint> groupByGranularity(List<Map<String, Object>> rows,
                                                            String field, String granularity,
                                                            LocalDate startDate, LocalDate endDate) {
        Map<String, Double> grouped = new TreeMap<>();
        
        // 模拟按粒度分组（实际应从数据中解析日期）
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        int points = switch (granularity.toLowerCase()) {
            case "day" -> (int) days;
            case "week" -> (int) (days / 7) + 1;
            case "month" -> (int) (days / 30) + 1;
            default -> (int) days;
        };
        
        // 使用现有数据生成趋势点
        int dataIndex = 0;
        for (int i = 0; i <= points && dataIndex < rows.size(); i++) {
            LocalDate date = switch (granularity.toLowerCase()) {
                case "day" -> startDate.plusDays(i);
                case "week" -> startDate.plusWeeks(i);
                case "month" -> startDate.plusMonths(i);
                default -> startDate.plusDays(i);
            };
            
            if (!date.isAfter(endDate)) {
                double value = getDoubleValue(rows.get(dataIndex % rows.size()).get(field));
                grouped.put(date.format(DateTimeFormatter.ISO_DATE), value);
                dataIndex++;
            }
        }
        
        return grouped.entrySet().stream()
            .map(e -> new TrendResult.DataPoint(e.getKey(), e.getValue(), LocalDate.parse(e.getKey())))
            .collect(Collectors.toList());
    }

    private double[] calculateLinearRegression(List<TrendResult.DataPoint> points) {
        int n = points.size();
        if (n < 2) return new double[]{0, 0};
        
        // 简单线性回归
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = points.get(i).value();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            sumY2 += y * y;
        }
        
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        
        // 计算R²
        double yMean = sumY / n;
        double ssTotal = 0, ssResidual = 0;
        for (int i = 0; i < n; i++) {
            double y = points.get(i).value();
            double yPred = slope * i + intercept;
            ssTotal += (y - yMean) * (y - yMean);
            ssResidual += (y - yPred) * (y - yPred);
        }
        double r2 = ssTotal > 0 ? 1 - (ssResidual / ssTotal) : 0;
        
        return new double[]{slope, Math.max(0, r2)};
    }

    private String generateForecast(double slope, double r2, List<TrendResult.DataPoint> points) {
        if (points.isEmpty()) return "数据不足，无法预测";
        
        double lastValue = points.get(points.size() - 1).value();
        
        if (r2 > 0.8) {
            if (slope > 0.1) {
                double predicted = lastValue * (1 + slope);
                return String.format("趋势明显上升，预计下一周期可达 %.2f", predicted);
            } else if (slope < -0.1) {
                double predicted = lastValue * (1 + slope);
                return String.format("趋势明显下降，预计下一周期约为 %.2f", predicted);
            } else {
                return "趋势稳定，预计将维持在当前水平";
            }
        } else {
            return "数据波动较大，建议关注异常因素";
        }
    }

    private double calculateMedian(List<Double> sortedValues) {
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2;
        } else {
            return sortedValues.get(size / 2);
        }
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> (v - mean) * (v - mean))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }

    private String generateRecommendations(Map<String, YoYResult> yoyResults,
                                            Map<String, MoMResult> momResults,
                                            List<AnomalyResult.AnomalyPoint> anomalies) {
        StringBuilder sb = new StringBuilder();
        
        // 分析同比趋势
        long growthCount = yoyResults.values().stream()
            .filter(r -> r.changeRate() > 0).count();
        if (growthCount > yoyResults.size() / 2) {
            sb.append("整体表现良好，多数指标实现同比增长。");
        } else if (growthCount < yoyResults.size() / 2) {
            sb.append("需要关注整体业务增长，多数指标同比下滑。");
        }
        
        // 分析异常
        if (!anomalies.isEmpty()) {
            sb.append(String.format("发现%d个异常数据点，建议重点排查。", anomalies.size()));
            
            long highSeverity = anomalies.stream()
                .filter(a -> "high".equals(a.severity())).count();
            if (highSeverity > 0) {
                sb.append(String.format("其中%d个为高风险异常。", highSeverity));
            }
        }
        
        return sb.length() > 0 ? sb.toString() : "数据正常，继续保持。";
    }
}
