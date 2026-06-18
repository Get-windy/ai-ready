package cn.aiedge.erp.metrics.service.impl;

import cn.aiedge.erp.metrics.dto.*;
import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.entity.MetricAggregation;
import cn.aiedge.erp.metrics.entity.MetricData;
import cn.aiedge.erp.metrics.enums.MetricPeriod;
import cn.aiedge.erp.metrics.enums.MetricStatus;
import cn.aiedge.erp.metrics.enums.MetricType;
import cn.aiedge.erp.metrics.repository.BusinessMetricRepository;
import cn.aiedge.erp.metrics.repository.MetricAggregationRepository;
import cn.aiedge.erp.metrics.repository.MetricDataRepository;
import cn.aiedge.erp.metrics.service.MetricsCalculationService;
import cn.aiedge.erp.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 业务指标服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsServiceImpl implements MetricsService {
    
    private final BusinessMetricRepository metricRepository;
    private final MetricDataRepository metricDataRepository;
    private final MetricAggregationRepository aggregationRepository;
    private final MetricsCalculationService calculationService;
    
    @Override
    public DashboardMetricsDTO getDashboardMetrics() {
        log.info("Getting dashboard metrics");
        
        DashboardMetricsDTO dashboard = new DashboardMetricsDTO();
        dashboard.setUpdateTime(LocalDateTime.now());
        
        // 获取关键指标卡片数据
        List<DashboardMetricsDTO.MetricCardDTO> cards = new ArrayList<>();
        
        // 订单指标
        cards.add(createMetricCard("order_total_today", "今日订单总数"));
        cards.add(createMetricCard("order_amount_today", "今日订单金额"));
        cards.add(createMetricCard("order_pending_count", "待处理订单"));
        
        // 库存指标
        cards.add(createMetricCard("inventory_total_value", "库存总价值"));
        cards.add(createMetricCard("inventory_low_stock_count", "低库存商品数"));
        
        // 用户指标
        cards.add(createMetricCard("user_active_today", "今日活跃用户"));
        cards.add(createMetricCard("user_new_today", "今日新增用户"));
        
        // 销售指标
        cards.add(createMetricCard("sales_revenue_today", "今日销售收入"));
        
        dashboard.setCards(cards);
        
        // 获取图表数据
        List<DashboardMetricsDTO.MetricChartDTO> charts = new ArrayList<>();
        charts.add(createOrderTrendChart());
        charts.add(createSalesDistributionChart());
        dashboard.setCharts(charts);
        
        return dashboard;
    }
    
    private DashboardMetricsDTO.MetricCardDTO createMetricCard(String metricCode, String defaultName) {
        DashboardMetricsDTO.MetricCardDTO card = new DashboardMetricsDTO.MetricCardDTO();
        card.setMetricCode(metricCode);
        
        Optional<BusinessMetric> metricOpt = metricRepository.findByMetricCode(metricCode);
        if (metricOpt.isPresent()) {
            BusinessMetric metric = metricOpt.get();
            card.setMetricName(metric.getMetricName());
            card.setUnit(metric.getUnit());
        } else {
            card.setMetricName(defaultName);
        }
        
        // 获取当前值
        Optional<MetricData> currentData = metricDataRepository.findLatestByMetricCode(metricCode);
        if (currentData.isPresent()) {
            card.setCurrentValue(currentData.get().getMetricValue());
        } else {
            // 如果没有数据，尝试实时计算
            try {
                Map<String, BigDecimal> calculated = calculationService.calculateMetricsByType(
                        getMetricTypeFromCode(metricCode), LocalDateTime.now());
                card.setCurrentValue(calculated.getOrDefault(metricCode, BigDecimal.ZERO));
            } catch (Exception e) {
                card.setCurrentValue(BigDecimal.ZERO);
            }
        }
        
        // 获取前一天数据进行对比
        card.setPreviousValue(BigDecimal.ZERO);
        card.setChangePercent(BigDecimal.ZERO);
        card.setTrend("FLAT");
        
        return card;
    }
    
    private MetricType getMetricTypeFromCode(String metricCode) {
        if (metricCode.startsWith("order_")) return MetricType.ORDER;
        if (metricCode.startsWith("inventory_")) return MetricType.INVENTORY;
        if (metricCode.startsWith("user_")) return MetricType.USER;
        if (metricCode.startsWith("sales_")) return MetricType.SALES;
        return MetricType.FINANCE;
    }
    
    private DashboardMetricsDTO.MetricChartDTO createOrderTrendChart() {
        DashboardMetricsDTO.MetricChartDTO chart = new DashboardMetricsDTO.MetricChartDTO();
        chart.setChartType("line");
        chart.setTitle("订单趋势（近7天）");
        
        // 生成模拟数据
        List<String> labels = List.of("周一", "周二", "周三", "周四", "周五", "周六", "周日");
        chart.setLabels(labels);
        
        DashboardMetricsDTO.ChartSeriesDTO series = new DashboardMetricsDTO.ChartSeriesDTO();
        series.setName("订单数");
        series.setData(List.of(
                new BigDecimal("120"),
                new BigDecimal("132"),
                new BigDecimal("101"),
                new BigDecimal("134"),
                new BigDecimal("90"),
                new BigDecimal("230"),
                new BigDecimal("210")
        ));
        
        chart.setSeries(List.of(series));
        return chart;
    }
    
    private DashboardMetricsDTO.MetricChartDTO createSalesDistributionChart() {
        DashboardMetricsDTO.MetricChartDTO chart = new DashboardMetricsDTO.MetricChartDTO();
        chart.setChartType("pie");
        chart.setTitle("销售分布");
        chart.setLabels(List.of("电子产品", "服装", "食品", "家居", "其他"));
        
        DashboardMetricsDTO.ChartSeriesDTO series = new DashboardMetricsDTO.ChartSeriesDTO();
        series.setName("销售额");
        series.setData(List.of(
                new BigDecimal("35000"),
                new BigDecimal("28000"),
                new BigDecimal("15000"),
                new BigDecimal("22000"),
                new BigDecimal("10000")
        ));
        
        chart.setSeries(List.of(series));
        return chart;
    }
    
    @Override
    public List<MetricValueDTO> getMetricsByType(MetricType type) {
        log.info("Getting metrics by type: {}", type);
        
        List<BusinessMetric> metrics = metricRepository.findActiveMetricsByType(type);
        
        return metrics.stream().map(metric -> {
            MetricValueDTO dto = new MetricValueDTO();
            dto.setMetricCode(metric.getMetricCode());
            dto.setMetricName(metric.getMetricName());
            dto.setUnit(metric.getUnit());
            dto.setPeriod(metric.getPeriod().getCode());
            
            // 获取最新值
            Optional<MetricData> latestData = metricDataRepository.findLatestByMetricCode(metric.getMetricCode());
            if (latestData.isPresent()) {
                dto.setValue(latestData.get().getMetricValue());
                dto.setTimestamp(latestData.get().getMetricTime());
            } else {
                dto.setValue(BigDecimal.ZERO);
                dto.setTimestamp(LocalDateTime.now());
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public MetricHistoryDTO getMetricHistory(MetricQueryRequest request) {
        log.info("Getting metric history for: {}", request.getMetricCode());
        
        BusinessMetric metric = metricRepository.findByMetricCode(request.getMetricCode())
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + request.getMetricCode()));
        
        LocalDateTime startTime = request.getStartTime() != null ? 
                request.getStartTime() : LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = request.getEndTime() != null ? 
                request.getEndTime() : LocalDateTime.now();
        
        String period = request.getPeriod() != null ? request.getPeriod() : "1h";
        
        MetricHistoryDTO history = new MetricHistoryDTO();
        history.setMetricCode(metric.getMetricCode());
        history.setMetricName(metric.getMetricName());
        history.setUnit(metric.getUnit());
        history.setPeriod(period);
        
        // 根据周期选择数据源
        List<MetricHistoryDTO.MetricDataPoint> dataPoints = new ArrayList<>();
        
        if ("realtime".equals(period) || "1m".equals(period)) {
            // 使用原始数据
            List<MetricData> rawData = metricDataRepository.findByMetricCodeAndTimeRange(
                    request.getMetricCode(), startTime, endTime,
                    request.getDimensionKey(), request.getDimensionValue());
            
            dataPoints = rawData.stream()
                    .map(d -> new MetricHistoryDTO.MetricDataPoint(d.getMetricTime(), d.getMetricValue()))
                    .collect(Collectors.toList());
        } else {
            // 使用聚合数据
            List<MetricAggregation> aggData = aggregationRepository.findByMetricCodePeriodAndTimeRange(
                    request.getMetricCode(), period, startTime, endTime);
            
            dataPoints = aggData.stream()
                    .map(d -> new MetricHistoryDTO.MetricDataPoint(d.getAggregationTime(), d.getAvgValue()))
                    .collect(Collectors.toList());
        }
        
        history.setDataPoints(dataPoints);
        return history;
    }
    
    @Override
    public MetricValueDTO getCurrentMetric(String metricCode) {
        log.info("Getting current metric: {}", metricCode);
        
        BusinessMetric metric = metricRepository.findByMetricCode(metricCode)
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + metricCode));
        
        MetricValueDTO dto = new MetricValueDTO();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setUnit(metric.getUnit());
        dto.setPeriod(metric.getPeriod().getCode());
        
        Optional<MetricData> latestData = metricDataRepository.findLatestByMetricCode(metricCode);
        if (latestData.isPresent()) {
            dto.setValue(latestData.get().getMetricValue());
            dto.setTimestamp(latestData.get().getMetricTime());
        } else {
            // 实时计算
            BigDecimal calculatedValue = calculationService.calculateMetric(metric, LocalDateTime.now());
            dto.setValue(calculatedValue);
            dto.setTimestamp(LocalDateTime.now());
        }
        
        return dto;
    }
    
    @Override
    public List<MetricValueDTO> getCurrentMetrics(List<String> metricCodes) {
        log.info("Getting current metrics: {}", metricCodes);
        return metricCodes.stream()
                .map(this::getCurrentMetric)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BusinessMetric> getAllActiveMetrics() {
        return metricRepository.findAllActiveMetrics();
    }
    
    @Override
    @Transactional
    public void refreshMetrics() {
        log.info("Refreshing all metrics");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 刷新所有类型的指标
        for (MetricType type : MetricType.values()) {
            try {
                Map<String, BigDecimal> calculatedMetrics = calculationService.calculateMetricsByType(type, now);
                
                // 保存计算结果
                List<MetricData> dataList = calculatedMetrics.entrySet().stream()
                        .map(entry -> MetricData.builder()
                                .metricCode(entry.getKey())
                                .metricValue(entry.getValue())
                                .metricTime(now)
                                .periodCode("realtime")
                                .build())
                        .collect(Collectors.toList());
                
                calculationService.batchSaveMetricData(dataList);
                
            } catch (Exception e) {
                log.error("Error refreshing metrics for type {}: {}", type, e.getMessage());
            }
        }
        
        log.info("Metrics refresh completed");
    }
}
