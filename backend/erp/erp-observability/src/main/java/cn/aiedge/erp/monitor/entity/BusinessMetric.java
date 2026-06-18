package cn.aiedge.erp.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务指标实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_business_metric")
public class BusinessMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 指标编码 */
    private String metricCode;

    /** 指标名称 */
    private String metricName;

    /** 指标类型：order-订单, inventory-库存, user-用户, sales-销售, purchase-采购, finance-财务, system-系统 */
    private String metricType;

    /** 指标值 */
    private BigDecimal metricValue;

    /** 指标单位 */
    private String unit;

    /** 统计周期：realtime-实时, 1m-1分钟, 5m-5分钟, 15m-15分钟, 1h-1小时, 4h-4小时, 1d-1天, 1w-1周, 1M-1月 */
    private String period;

    /** 统计时间 */
    private LocalDateTime statTime;

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

    /** 环比增长率（%） */
    private BigDecimal chainRatio;

    /** 同比增长率（%） */
    private BigDecimal yearRatio;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 完成率（%） */
    private BigDecimal completionRate;

    /** 状态：normal-正常, warning-警告, critical-严重, unknown-未知 */
    private String status;

    /** 备注 */
    private String remark;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
