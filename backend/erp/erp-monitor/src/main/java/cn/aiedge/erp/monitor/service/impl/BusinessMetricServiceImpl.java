package cn.aiedge.erp.monitor.service.impl;

import cn.aiedge.erp.monitor.dto.MetricDashboardDTO;
import cn.aiedge.erp.monitor.dto.MetricQueryDTO;
import cn.aiedge.erp.monitor.dto.MetricRealTimeDTO;
import cn.aiedge.erp.monitor.entity.BusinessMetric;
import cn.aiedge.erp.monitor.entity.MetricDefinition;
import cn.aiedge.erp.monitor.enums.MetricStatus;
import cn.aiedge.erp.monitor.enums.MetricType;
import cn.aiedge.erp.monitor.mapper.BusinessMetricMapper;
import cn.aiedge.erp.monitor.mapper.MetricDefinitionMapper;
import cn.aiedge.erp.monitor.service.BusinessMetricService;
import cn.aiedge.erp.monitor.service.MetricCollectorService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务指标服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessMetricServiceImpl implements BusinessMetricService {

    private final BusinessMetricMapper businessMetricMapper;
    private final MetricDefinitionMapper metricDefinitionMapper;
    private final MetricCollectorService metricCollectorService;

    @Override
    public List<MetricRealTimeDTO> getRealTimeMetrics(Long tenantId, List<String> metricTypes) {
        List<MetricRealTimeDTO> result = new ArrayList<>();
        
        for (String type : metricTypes) {
            List<BusinessMetric> metrics = businessMetricMapper.selectRealTimeMetrics(
                tenantId, type, 10);
            
            for (BusinessMetric metric : metrics) {
                MetricRealTimeDTO dto = convertToRealTimeDTO(metric);
                result.add(dto);
            }
        }
        
        return result;
    }

    @Override
    public MetricDashboardDTO getDashboard(Long tenantId) {
        MetricDashboardDTO dashboard = new MetricDashboardDTO();
        dashboard.setTitle("业务指标监控大盘");
        dashboard.setUpdateTime(LocalDateTime.now());
        
        // 核心指标卡片
        List<MetricDashboardDTO.MetricCardDTO> coreMetrics = new ArrayList<>();
        
        // 订单相关指标
        Map<String, Object> orderMetrics = getOrderMetrics(tenantId, "realtime");
        coreMetrics.add(createMetricCard("order_today", "今日订单数", 
            (BigDecimal) orderMetrics.getOrDefault("todayCount", BigDecimal.ZERO), 
            "笔", (BigDecimal) orderMetrics.getOrDefault("dayChainRatio", BigDecimal.ZERO)));
        
        coreMetrics.add(createMetricCard("order_amount_today", "今日订单金额", 
            (BigDecimal) orderMetrics.getOrDefault("todayAmount", BigDecimal.ZERO), 
            "元", (BigDecimal) orderMetrics.getOrDefault("amountChainRatio", BigDecimal.ZERO)));
        
        // 库存相关指标
        Map<String, Object> inventoryMetrics = getInventoryMetrics(tenantId, "realtime");
        coreMetrics.add(createMetricCard("inventory_total", "库存总量", 
            (BigDecimal) inventoryMetrics.getOrDefault("totalQuantity", BigDecimal.ZERO), 
            "件", BigDecimal.ZERO));
        
        coreMetrics.add(createMetricCard("inventory_value", "库存价值", 
            (BigDecimal) inventoryMetrics.getOrDefault("totalValue", BigDecimal.ZERO), 
            "元", BigDecimal.ZERO));
        
        // 用户相关指标
        Map<String, Object> userMetrics = getUserMetrics(tenantId, "realtime");
        coreMetrics.add(createMetricCard("user_total", "用户总数", 
            (BigDecimal) userMetrics.getOrDefault("totalCount", BigDecimal.ZERO), 
            "人", (BigDecimal) userMetrics.getOrDefault("dayGrowthRate", BigDecimal.ZERO)));
        
        coreMetrics.add(createMetricCard("user_active_today", "今日活跃用户", 
            (BigDecimal) userMetrics.getOrDefault("activeToday", BigDecimal.ZERO), 
            "人", (BigDecimal) userMetrics.getOrDefault("activeChainRatio", BigDecimal.ZERO)));
        
        dashboard.setCoreMetrics(coreMetrics);
        
        // 趋势图表
        List<MetricDashboardDTO.MetricChartDTO> charts = new ArrayList<>();
        charts.add(createTrendChart(tenantId, "order_trend", "订单趋势", "line"));
        charts.add(createTrendChart(tenantId, "sales_trend", "销售趋势", "line"));
        dashboard.setCharts(charts);
        
        // 告警列表（模拟数据）
        dashboard.setAlerts(new ArrayList<>());
        
        return dashboard;
    }

    private MetricDashboardDTO.MetricCardDTO createMetricCard(String code, String name, 
                                                               BigDecimal value, String unit, 
                                                               BigDecimal chainRatio) {
        MetricDashboardDTO.MetricCardDTO card = new MetricDashboardDTO.MetricCardDTO();
        card.setMetricCode(code);
        card.setMetricName(name);
        card.setCurrentValue(value);
        card.setUnit(unit);
        card.setChainRatio(chainRatio);
        
        // 设置状态
        if (chainRatio.compareTo(new BigDecimal("-20")) < 0) {
            card.setStatus(MetricStatus.CRITICAL.getCode());
        } else if (chainRatio.compareTo(new BigDecimal("-10")) < 0) {
            card.setStatus(MetricStatus.WARNING.getCode());
        } else {
            card.setStatus(MetricStatus.NORMAL.getCode());
        }
        
        // 设置趋势
        if (chainRatio.compareTo(BigDecimal.ZERO) > 0) {
            card.setTrend("up");
        } else if (chainRatio.compareTo(BigDecimal.ZERO) < 0) {
            card.setTrend("down");
        } else {
            card.setTrend("flat");
        }
        
        return card;
    }

    private MetricDashboardDTO.MetricChartDTO createTrendChart(Long tenantId, String metricCode, 
                                                                  String title, String chartType) {
        MetricDashboardDTO.MetricChartDTO chart = new MetricDashboardDTO.MetricChartDTO();
        chart.setTitle(title);
        chart.setChartType(chartType);
        
        // 生成最近7天的X轴数据
        List<String> xAxis = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 6; i >= 0; i--) {
            xAxis.add(now.minusDays(i).toLocalDate().toString());
        }
        chart.setXAxis(xAxis);
        
        // 模拟趋势数据
        List<MetricDashboardDTO.MetricSeriesDTO> series = new ArrayList<>();
        MetricDashboardDTO.MetricSeriesDTO seriesData = new MetricDashboardDTO.MetricSeriesDTO();
        seriesData.setName(title);
        List<BigDecimal> data = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 7; i++) {
            data.add(new BigDecimal(random.nextInt(100) + 50));
        }
        seriesData.setData(data);
        series.add(seriesData);
        chart.setSeries(series);
        
        return chart;
    }

    @Override
    public IPage<BusinessMetric> queryMetrics(Page<BusinessMetric> page, MetricQueryDTO query) {
        return businessMetricMapper.selectMetricPage(page, query);
    }

    @Override
    public List<BusinessMetric> getHistoryMetrics(Long tenantId, String metricCode, 
                                                   String period, LocalDateTime startTime, 
                                                   LocalDateTime endTime) {
        return businessMetricMapper.selectHistoryMetrics(tenantId, metricCode, period, startTime, endTime);
    }

    @Override
    public Map<String, Object> getMetricTrend(Long tenantId, String metricCode, 
                                               String period, int hours) {
        List<BusinessMetric> metrics = businessMetricMapper.selectMetricTrend(
            tenantId, metricCode, period, hours);
        
        Map<String, Object> result = new HashMap<>();
        result.put("metricCode", metricCode);
        result.put("period", period);
        result.put("hours", hours);
        result.put("data", metrics);
        
        if (!metrics.isEmpty()) {
            BigDecimal avg = metrics.stream()
                .map(BusinessMetric::getMetricValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(metrics.size()), 2, RoundingMode.HALF_UP);
            result.put("average", avg);
        }
        
        return result;
    }

    @Override
    @Transactional
    public void refreshMetrics(Long tenantId) {
        log.info("Refreshing metrics for tenant: {}", tenantId);
        
        LocalDateTime now = LocalDateTime.now();
        List<String> metricTypes = Arrays.asList(
            MetricType.ORDER.getCode(),
            MetricType.INVENTORY.getCode(),
            MetricType.USER.getCode(),
            MetricType.SALES.getCode()
        );
        
        metricCollectorService.batchCollectAndSave(tenantId, metricTypes);
        
        log.info("Metrics refreshed successfully for tenant: {}", tenantId);
    }

    @Override
    public List<MetricDefinition> getMetricDefinitions(Long tenantId, String metricType) {
        if (metricType != null && !metricType.isEmpty()) {
            return metricDefinitionMapper.selectEnabledByType(tenantId, metricType);
        }
        return metricDefinitionMapper.selectAllEnabled(tenantId);
    }

    @Override
    @Transactional
    public MetricDefinition saveMetricDefinition(MetricDefinition definition) {
        if (definition.getId() == null) {
            metricDefinitionMapper.insert(definition);
        } else {
            metricDefinitionMapper.updateById(definition);
        }
        return definition;
    }

    @Override
    @Transactional
    public boolean deleteMetricDefinition(Long id) {
        return metricDefinitionMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> getCoreBusinessMetrics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("order", getOrderMetrics(tenantId, "realtime"));
        result.put("inventory", getInventoryMetrics(tenantId, "realtime"));
        result.put("user", getUserMetrics(tenantId, "realtime"));
        result.put("sales", getSalesMetrics(tenantId, "realtime"));
        
        return result;
    }

    @Override
    public Map<String, Object> getOrderMetrics(Long tenantId, String period) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 模拟订单指标数据
        metrics.put("todayCount", new BigDecimal("156"));
        metrics.put("todayAmount", new BigDecimal("45680.50"));
        metrics.put("dayChainRatio", new BigDecimal("12.5"));
        metrics.put("amountChainRatio", new BigDecimal("8.3"));
        metrics.put("pendingCount", new BigDecimal("23"));
        metrics.put("completedCount", new BigDecimal("133"));
        metrics.put("cancelledCount", new BigDecimal("0"));
        
        return metrics;
    }

    @Override
    public Map<String, Object> getInventoryMetrics(Long tenantId, String period) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 模拟库存指标数据
        metrics.put("totalQuantity", new BigDecimal("12580"));
        metrics.put("totalValue", new BigDecimal("568420.00"));
        metrics.put("skuCount", new BigDecimal("356"));
        metrics.put("lowStockCount", new BigDecimal("12"));
        metrics.put("outOfStockCount", new BigDecimal("3"));
        metrics.put("turnoverRate", new BigDecimal("4.2"));
        
        return metrics;
    }

    @Override
    public Map<String, Object> getUserMetrics(Long tenantId, String period) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 模拟用户指标数据
        metrics.put("totalCount", new BigDecimal("2580"));
        metrics.put("activeToday", new BigDecimal("186"));
        metrics.put("newToday", new BigDecimal("12"));
        metrics.put("dayGrowthRate", new BigDecimal("0.47"));
        metrics.put("activeChainRatio", new BigDecimal("5.2"));
        metrics.put("onlineCount", new BigDecimal("45"));
        
        return metrics;
    }

    @Override
    public Map<String, Object> getSalesMetrics(Long tenantId, String period) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 模拟销售指标数据
        metrics.put("todaySales", new BigDecimal("45680.50"));
        metrics.put("monthSales", new BigDecimal("1256800.00"));
        metrics.put("yearSales", new BigDecimal("12568000.00"));
        metrics.put("monthTarget", new BigDecimal("1500000.00"));
        metrics.put("completionRate", new BigDecimal("83.8"));
        metrics.put("avgOrderAmount", new BigDecimal("292.8"));
        
        return metrics;
    }

    private MetricRealTimeDTO convertToRealTimeDTO(BusinessMetric metric) {
        MetricRealTimeDTO dto = new MetricRealTimeDTO();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setMetricType(metric.getMetricType());
        dto.setCurrentValue(metric.getMetricValue());
        dto.setUnit(metric.getUnit());
        dto.setCollectTime(metric.getStatTime());
        dto.setUpdateTime(metric.getUpdateTime());
        dto.setStatus(metric.getStatus());
        dto.setDayChainRatio(metric.getChainRatio());
        return dto;
    }
}
