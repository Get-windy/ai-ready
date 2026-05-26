package cn.aiedge.erp.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标定义实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_metric_definition")
public class MetricDefinition {

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

    /** 指标描述 */
    private String description;

    /** 指标单位 */
    private String unit;

    /** 数据类型：integer-整数, decimal-小数, percent-百分比, amount-金额, count-数量 */
    private String dataType;

    /** 计算方式：sum-求和, count-计数, avg-平均, max-最大值, min-最小值, custom-自定义 */
    private String calcMethod;

    /** 计算公式（自定义时使用） */
    private String calcFormula;

    /** 数据源表名 */
    private String sourceTable;

    /** 数据源字段 */
    private String sourceField;

    /** 过滤条件 */
    private String filterCondition;

    /** 分组字段 */
    private String groupByFields;

    /** 警告阈值 */
    private BigDecimal warningThreshold;

    /** 严重阈值 */
    private BigDecimal criticalThreshold;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 是否启用 */
    private Integer enabled;

    /** 排序号 */
    private Integer sortOrder;

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
