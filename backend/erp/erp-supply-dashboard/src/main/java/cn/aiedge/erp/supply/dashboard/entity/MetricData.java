package cn.aiedge.erp.supply.dashboard.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指标数据实体
 * 存储指标计算结果
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "supply_metric_data", indexes = {
    @Index(name = "idx_metric_code_date", columnList = "metric_code, metric_date"),
    @Index(name = "idx_org_date", columnList = "org_id, metric_date"),
    @Index(name = "idx_category_date", columnList = "category, metric_date"),
    @Index(name = "idx_calc_time", columnList = "calculation_time"),
    @Index(name = "idx_data_source", columnList = "data_source_id, data_source_type")
})
@Comment("供应链指标数据表")
public class MetricData {

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

    @Column(name = "category", nullable = false, length = 30)
    @Comment("指标类别")
    private String category;

    @Column(name = "sub_category", length = 30)
    @Comment("子类别")
    private String subCategory;

    @Column(name = "metric_date", nullable = false)
    @Comment("指标日期")
    private LocalDate metricDate;

    @Column(name = "period_type", length = 20)
    @Comment("周期类型：DAILY-日报，WEEKLY-周报，MONTHLY-月报，QUARTERLY-季报，YEARLY-年报")
    private String periodType;

    @Column(name = "metric_value", precision = 20, scale = 6)
    @Comment("指标值")
    private BigDecimal metricValue;

    @Column(name = "target_value", precision = 20, scale = 6)
    @Comment("目标值")
    private BigDecimal targetValue;

    @Column(name = "previous_value", precision = 20, scale = 6)
    @Comment("上一周期值")
    private BigDecimal previousValue;

    @Column(name = "change_rate", precision = 8, scale = 4)
    @Comment("变化率")
    private BigDecimal changeRate;

    @Column(name = "completion_rate", precision = 8, scale = 4)
    @Comment("完成率")
    private BigDecimal completionRate;

    @Column(name = "data_source_id")
    @Comment("数据源ID")
    private Long dataSourceId;

    @Column(name = "data_source_type", length = 50)
    @Comment("数据源类型：PURCHASE_ORDER-采购订单，INVENTORY_RECORD-库存记录，LOGISTICS_TRACK-物流跟踪")
    private String dataSourceType;

    @Column(name = "org_id")
    @Comment("组织ID")
    private Long orgId;

    @Column(name = "department_id")
    @Comment("部门ID")
    private Long departmentId;

    @Column(name = "product_category_id")
    @Comment("产品类别ID")
    private Long productCategoryId;

    @Column(name = "supplier_id")
    @Comment("供应商ID")
    private Long supplierId;

    @Column(name = "warehouse_id")
    @Comment("仓库ID")
    private Long warehouseId;

    @Column(name = "status", length = 20)
    @Comment("状态：NORMAL-正常，WARNING-预警，CRITICAL-紧急，EXCEPTION-异常")
    private String status;

    @Column(name = "confidence_level", length = 20)
    @Comment("置信度：HIGH-高，MEDIUM-中，LOW-低")
    private String confidenceLevel;

    @Column(name = "data_quality_score", precision = 5, scale = 2)
    @Comment("数据质量评分（0-100）")
    private BigDecimal dataQualityScore;

    @Column(name = "calculation_version")
    @Comment("计算版本")
    private Integer calculationVersion;

    @Column(name = "calculation_algorithm", length = 100)
    @Comment("计算算法")
    private String calculationAlgorithm;

    @Column(name = "calculation_parameters", length = 1000)
    @Comment("计算参数（JSON格式）")
    private String calculationParameters;

    @CreationTimestamp
    @Column(name = "calculation_time")
    @Comment("计算时间")
    private LocalDateTime calculationTime;

    @Column(name = "calculation_duration")
    @Comment("计算耗时（毫秒）")
    private Long calculationDuration;

    @Column(name = "data_valid_from")
    @Comment("数据有效开始时间")
    private LocalDateTime dataValidFrom;

    @Column(name = "data_valid_to")
    @Comment("数据有效结束时间")
    private LocalDateTime dataValidTo;

    @Column(name = "remark", length = 500)
    @Comment("备注")
    private String remark;

    /**
     * 周期类型枚举
     */
    public enum PeriodType {
        DAILY("日报"),
        WEEKLY("周报"),
        MONTHLY("月报"),
        QUARTERLY("季报"),
        YEARLY("年报");

        private final String description;

        PeriodType(String description) {
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
        NORMAL("正常"),
        WARNING("预警"),
        CRITICAL("紧急"),
        EXCEPTION("异常");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 置信度枚举
     */
    public enum ConfidenceLevel {
        HIGH("高"),
        MEDIUM("中"),
        LOW("低");

        private final String description;

        ConfidenceLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        PURCHASE_ORDER("采购订单"),
        INVENTORY_RECORD("库存记录"),
        LOGISTICS_TRACK("物流跟踪"),
        SALES_ORDER("销售订单"),
        FINANCIAL_STATEMENT("财务报表"),
        SUPPLIER_ASSESSMENT("供应商评估");

        private final String description;

        DataSourceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}