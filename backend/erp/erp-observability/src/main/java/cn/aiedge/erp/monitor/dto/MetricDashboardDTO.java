package cn.aiedge.erp.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 指标仪表盘DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class MetricDashboardDTO {

    /** 仪表盘标题 */
    private String title;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 核心指标列表 */
    private List<MetricCardDTO> coreMetrics;

    /** 趋势图表数据 */
    private List<MetricChartDTO> charts;

    /** 告警列表 */
    private List<MetricAlertDTO> alerts;

    /**
     * 指标卡片DTO
     */
    @Data
    public static class MetricCardDTO {
        /** 指标编码 */
        private String metricCode;

        /** 指标名称 */
        private String metricName;

        /** 当前值 */
        private BigDecimal currentValue;

        /** 单位 */
        private String unit;

        /** 环比增长率 */
        private BigDecimal chainRatio;

        /** 同比增长率 */
        private BigDecimal yearRatio;

        /** 状态 */
        private String status;

        /** 趋势方向：up-上升, down-下降, flat-持平 */
        private String trend;
    }

    /**
     * 指标图表DTO
     */
    @Data
    public static class MetricChartDTO {
        /** 图表标题 */
        private String title;

        /** 图表类型：line-折线图, bar-柱状图, pie-饼图 */
        private String chartType;

        /** X轴数据 */
        private List<String> xAxis;

        /** 系列数据 */
        private List<MetricSeriesDTO> series;
    }

    /**
     * 指标系列DTO
     */
    @Data
    public static class MetricSeriesDTO {
        /** 系列名称 */
        private String name;

        /** 数据 */
        private List<BigDecimal> data;
    }

    /**
     * 指标告警DTO
     */
    @Data
    public static class MetricAlertDTO {
        /** 告警ID */
        private Long id;

        /** 指标名称 */
        private String metricName;

        /** 告警级别 */
        private String level;

        /** 告警消息 */
        private String message;

        /** 当前值 */
        private BigDecimal currentValue;

        /** 阈值 */
        private BigDecimal threshold;

        /** 告警时间 */
        private LocalDateTime alertTime;
    }
}
