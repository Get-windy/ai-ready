package cn.aiedge.erp.monitor.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标查询DTO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class MetricQueryDTO {

    /** 租户ID */
    private Long tenantId;

    /** 指标编码 */
    private String metricCode;

    /** 指标类型 */
    private String metricType;

    /** 指标编码列表 */
    private List<String> metricCodes;

    /** 指标类型列表 */
    private List<String> metricTypes;

    /** 统计周期 */
    private String period;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 维度1类型 */
    private String dimension1Type;

    /** 维度1值 */
    private String dimension1Value;

    /** 维度2类型 */
    private String dimension2Type;

    /** 维度2值 */
    private String dimension2Value;

    /** 维度3类型 */
    private String dimension3Type;

    /** 维度3值 */
    private String dimension3Value;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;
}
