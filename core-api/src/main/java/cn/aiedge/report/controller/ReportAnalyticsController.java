package cn.aiedge.report.controller;

import cn.aiedge.report.service.ReportAnalyticsService;
import cn.aiedge.report.service.ReportAnalyticsService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表统计分析控制器
 * 提供同比、环比、趋势分析等高级统计API
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/report/analytics")
@RequiredArgsConstructor
@Tag(name = "报表统计分析", description = "高级统计分析功能：同比、环比、趋势、排名、分布分析等")
public class ReportAnalyticsController {

    private final ReportAnalyticsService analyticsService;

    /**
     * 同比分析
     */
    @PostMapping("/{reportId}/yoy")
    @Operation(summary = "同比分析", description = "比较当前周期与上年同期的数据变化")
    public ResponseEntity<YoYResult> yearOverYearAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("同比分析请求: reportId={}, field={}", reportId, field);
        
        YoYResult result = analyticsService.yearOverYearAnalysis(reportId, field, startDate, endDate, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 环比分析
     */
    @PostMapping("/{reportId}/mom")
    @Operation(summary = "环比分析", description = "比较当前周期与上一周期的数据变化")
    public ResponseEntity<MoMResult> monthOverMonthAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(description = "周期类型: day/week/month/quarter") @RequestParam(defaultValue = "month") String period,
            @Parameter(description = "当前周期起始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentPeriod,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("环比分析请求: reportId={}, field={}, period={}", reportId, field, period);
        
        MoMResult result = analyticsService.monthOverMonthAnalysis(reportId, field, period, currentPeriod, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 趋势分析
     */
    @PostMapping("/{reportId}/trend")
    @Operation(summary = "趋势分析", description = "分析指定时间段内的数据趋势")
    public ResponseEntity<TrendResult> trendAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "粒度: day/week/month") @RequestParam(defaultValue = "day") String granularity,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("趋势分析请求: reportId={}, field={}, granularity={}", reportId, field, granularity);
        
        TrendResult result = analyticsService.trendAnalysis(reportId, field, startDate, endDate, granularity, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 排名分析
     */
    @PostMapping("/{reportId}/ranking")
    @Operation(summary = "排名分析", description = "按指定维度进行排名")
    public ResponseEntity<RankingResult> rankingAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "排名字段") @RequestParam String rankField,
            @Parameter(description = "分组字段") @RequestParam String groupField,
            @Parameter(description = "返回前N名") @RequestParam(defaultValue = "10") int topN,
            @Parameter(description = "是否升序") @RequestParam(defaultValue = "false") boolean ascending,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("排名分析请求: reportId={}, rankField={}, groupField={}", reportId, rankField, groupField);
        
        RankingResult result = analyticsService.rankingAnalysis(reportId, rankField, groupField, topN, ascending, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 占比分析
     */
    @PostMapping("/{reportId}/proportion")
    @Operation(summary = "占比分析", description = "分析各部分占总体的比例")
    public ResponseEntity<ProportionResult> proportionAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "值字段") @RequestParam String valueField,
            @Parameter(description = "分组字段") @RequestParam String groupField,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("占比分析请求: reportId={}, valueField={}, groupField={}", reportId, valueField, groupField);
        
        ProportionResult result = analyticsService.proportionAnalysis(reportId, valueField, groupField, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 分布分析
     */
    @PostMapping("/{reportId}/distribution")
    @Operation(summary = "分布分析", description = "分析数据的分布情况")
    public ResponseEntity<DistributionResult> distributionAnalysis(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(description = "分桶数量") @RequestParam(defaultValue = "10") int buckets,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("分布分析请求: reportId={}, field={}, buckets={}", reportId, field, buckets);
        
        DistributionResult result = analyticsService.distributionAnalysis(reportId, field, buckets, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 异常检测
     */
    @PostMapping("/{reportId}/anomaly")
    @Operation(summary = "异常检测", description = "检测数据中的异常值")
    public ResponseEntity<AnomalyResult> anomalyDetection(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(description = "敏感度: low/medium/high") @RequestParam(defaultValue = "medium") String sensitivity,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("异常检测请求: reportId={}, field={}, sensitivity={}", reportId, field, sensitivity);
        
        AnomalyResult result = analyticsService.anomalyDetection(reportId, field, sensitivity, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 综合统计报告
     */
    @PostMapping("/{reportId}/comprehensive")
    @Operation(summary = "综合统计报告", description = "生成包含多种分析的综合报告")
    public ResponseEntity<ComprehensiveReport> generateComprehensiveReport(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段列表") @RequestParam List<String> fields,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("综合报告请求: reportId={}, fields={}", reportId, fields);
        
        ComprehensiveReport result = analyticsService.generateComprehensiveReport(reportId, fields, startDate, endDate, tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 快速统计概览
     */
    @GetMapping("/{reportId}/overview")
    @Operation(summary = "快速统计概览", description = "获取报表的快速统计概览")
    public ResponseEntity<Map<String, Object>> getOverview(
            @Parameter(description = "报表ID") @PathVariable String reportId,
            @Parameter(description = "分析字段") @RequestParam String field,
            @Parameter(hidden = true) @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        
        log.info("快速概览请求: reportId={}, field={}", reportId, field);
        
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        
        // 获取多种分析结果
        YoYResult yoy = analyticsService.yearOverYearAnalysis(reportId, field, monthStart, today, tenantId);
        MoMResult mom = analyticsService.monthOverMonthAnalysis(reportId, field, "month", today, tenantId);
        TrendResult trend = analyticsService.trendAnalysis(reportId, field, monthStart, today, "day", tenantId);
        AnomalyResult anomaly = analyticsService.anomalyDetection(reportId, field, "medium", tenantId);
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("currentValue", yoy.currentValue());
        overview.put("yoyChangeRate", yoy.changeRate());
        overview.put("momChangeRate", mom.changeRate());
        overview.put("trend", trend.trend());
        overview.put("anomalyCount", anomaly.anomalyCount());
        overview.put("lastUpdated", today.toString());
        
        return ResponseEntity.ok(overview);
    }
}
