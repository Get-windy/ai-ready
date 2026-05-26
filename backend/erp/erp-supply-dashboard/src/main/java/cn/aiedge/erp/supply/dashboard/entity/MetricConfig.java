package cn.aiedge.erp.supply.dashboard.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 指标配置实体
 * 用于配置和管理供应链仪表板中的各项指标
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "supply_metric_config", indexes = {
    @Index(name = "idx_metric_code", columnList = "metric_code", unique = true),
    @Index(name = "idx_category_status", columnList = "category, status"),
    @Index(name = "idx_org_id", columnList = "org_id")
})
@Comment("供应链指标配置表")
public class MetricConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("主键ID")
    private Long id;

    @Column(name = "metric_code", nullable = false, length = 50)
    @Comment("指标编码")
    private String metricCode;

    @Column(name = "metric_name", nullable = false, length = 100)
    @Comment("指标名称")
    private String metricName;

    @Column(name = "metric_description", length = 500)
    @Comment("指标描述")
    private String metricDescription;

    @Column(name = "category", nullable = false, length = 30)
    @Comment("指标类别：INVENTORY-库存，PURCHASE-采购，LOGISTICS-物流，SUPPLIER-供应商，FINANCE-财务")
    private String category;

    @Column(name = "sub_category", length = 30)
    @Comment("子类别")
    private String subCategory;

    @Column(name = "calculation_formula", length = 1000)
    @Comment("计算公式")
    private String calculationFormula;

    @Column(name = "data_source", length = 200)
    @Comment("数据来源，格式：模块.表名.字段名")
    private String dataSource;

    @Column(name = "refresh_frequency", length = 20)
    @Comment("刷新频率：REALTIME-实时，HOURLY-每小时，DAILY-每天，WEEKLY-每周，MONTHLY-每月")
    private String refreshFrequency;

    @Column(name = "unit", length = 20)
    @Comment("单位：PERCENT-百分比，DAY-天，TIMES-次，YUAN-元")
    private String unit;

    @Column(name = "decimal_places")
    @Comment("小数位数")
    private Integer decimalPlaces;

    @Column(name = "target_value", precision = 15, scale = 4)
    @Comment("目标值")
    private Double targetValue;

    @Column(name = "warning_threshold", precision = 15, scale = 4)
    @Comment("预警阈值")
    private Double warningThreshold;

    @Column(name = "critical_threshold", precision = 15, scale = 4)
    @Comment("紧急阈值")
    private Double criticalThreshold;

    @Column(name = "weight")
    @Comment("权重（用于综合评分计算）")
    private Integer weight;

    @Column(name = "display_order")
    @Comment("显示顺序")
    private Integer displayOrder;

    @Column(name = "chart_type", length = 30)
    @Comment("图表类型：BAR-柱状图，LINE-折线图，PIE-饼图，RADAR-雷达图，HEATMAP-热力图")
    private String chartType;

    @Column(name = "is_visible")
    @Comment("是否可见")
    private Boolean visible = true;

    @Column(name = "is_editable")
    @Comment("是否可编辑")
    private Boolean editable = true;

    @Column(name = "org_id")
    @Comment("组织ID")
    private Long orgId;

    @Column(name = "department_id")
    @Comment("部门ID")
    private Long departmentId;

    @Column(name = "status", length = 20)
    @Comment("状态：ACTIVE-激活，INACTIVE-未激活，DELETED-已删除")
    private String status = "ACTIVE";

    @Column(name = "version")
    @Comment("版本号")
    private Integer version = 1;

    @Column(name = "created_by", length = 50)
    @Comment("创建人")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_time")
    @Comment("创建时间")
    private LocalDateTime createdTime;

    @Column(name = "updated_by", length = 50)
    @Comment("更新人")
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_time")
    @Comment("更新时间")
    private LocalDateTime updatedTime;

    @Column(name = "remark", length = 500)
    @Comment("备注")
    private String remark;

    /**
     * 指标类别枚举
     */
    public enum Category {
        INVENTORY("库存指标"),
        PURCHASE("采购指标"),
        LOGISTICS("物流指标"),
        SUPPLIER("供应商指标"),
        FINANCE("财务指标");

        private final String description;

        Category(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 刷新频率枚举
     */
    public enum RefreshFrequency {
        REALTIME("实时"),
        HOURLY("每小时"),
        DAILY("每天"),
        WEEKLY("每周"),
        MONTHLY("每月");

        private final String description;

        RefreshFrequency(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 图表类型枚举
     */
    public enum ChartType {
        BAR("柱状图"),
        LINE("折线图"),
        PIE("饼图"),
        RADAR("雷达图"),
        HEATMAP("热力图"),
        GAUGE("仪表盘"),
        SCATTER("散点图");

        private final String description;

        ChartType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 状态枚举
     */
    public enum Status {
        ACTIVE("激活"),
        INACTIVE("未激活"),
        DELETED("已删除");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}