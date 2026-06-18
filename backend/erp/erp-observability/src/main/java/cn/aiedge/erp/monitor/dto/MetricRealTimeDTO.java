package cn.aiedge.erp.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时指标DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class MetricRealTimeDTO {

    /** 指标编码 */
    private String metricCode;

    /** 指标名称 */
    private String metricName;

    /** 指标类型 */
    private String metricType;

    /** 当前值 */
    private BigDecimal currentValue;

    /** 单位 */
    private String unit;

    /** 采集时间 */
    private LocalDateTime collectTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 状态 */
    private String status;

    /** 今日累计值 */
    private BigDecimal todayTotal;

    /** 昨日同期值 */
    private BigDecimal yesterdaySameTime;

    /** 日环比 */
    private BigDecimal dayChainRatio;
}
