package cn.aiedge.erp.supplierbatchanalysis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商-批次关联分析结果实体
 * 存储供应商绩效与批次质量的关联分析结果
 *
 * @author team-member
 * @date 2026-04-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("supplier_batch_correlation_result")
public class SupplierBatchCorrelationResult {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    private Long supplierId;

    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    private String supplierName;

    /**
     * 分析时间范围开始
     */
    @TableField("analysis_start_date")
    private LocalDateTime analysisStartDate;

    /**
     * 分析时间范围结束
     */
    @TableField("analysis_end_date")
    private LocalDateTime analysisEndDate;

    /**
     * 分析类型：MONTHLY-月度分析, QUARTERLY-季度分析, YEARLY-年度分析, CUSTOM-自定义分析
     */
    @TableField("analysis_type")
    private String analysisType;

    /**
     * 分析批次总数
     */
    @TableField("total_batches")
    private Integer totalBatches;

    /**
     * 供应商绩效平均分
     */
    @TableField("supplier_score_avg")
    private BigDecimal supplierScoreAvg;

    /**
     * 批次合格率平均
     */
    @TableField("batch_qualification_rate_avg")
    private BigDecimal batchQualificationRateAvg;

    /**
     * 批次质量得分平均
     */
    @TableField("batch_quality_score_avg")
    private BigDecimal batchQualityScoreAvg;

    /**
     * 相关性系数（皮尔逊相关系数）
     * 范围: -1.0 到 1.0
     */
    @TableField("correlation_coefficient")
    private BigDecimal correlationCoefficient;

    /**
     * 相关性强度描述：STRONG_POSITIVE-强正相关, WEAK_POSITIVE-弱正相关, 
     * NO_CORRELATION-无相关, WEAK_NEGATIVE-弱负相关, STRONG_NEGATIVE-强负相关
     */
    @TableField("correlation_strength")
    private String correlationStrength;

    /**
     * 显著性水平 (p-value)
     */
    @TableField("significance_level")
    private BigDecimal significanceLevel;

    /**
     * 回归方程系数（JSON格式）
     * 例如: {"intercept": 75.2, "slope": 0.45, "r_squared": 0.89}
     */
    @TableField("regression_coefficients")
    private String regressionCoefficients;

    /**
     * 绩效得分对合格率的影响系数
     */
    @TableField("impact_coefficient")
    private BigDecimal impactCoefficient;

    /**
     * 置信区间下限
     */
    @TableField("confidence_interval_lower")
    private BigDecimal confidenceIntervalLower;

    /**
     * 置信区间上限
     */
    @TableField("confidence_interval_upper")
    private BigDecimal confidenceIntervalUpper;

    /**
     * 供应商等级分布（JSON格式）
     */
    @TableField("supplier_level_distribution")
    private String supplierLevelDistribution;

    /**
     * 批次质量等级分布（JSON格式）
     */
    @TableField("batch_quality_distribution")
    private String batchQualityDistribution;

    /**
     * 关键发现
     */
    @TableField("key_findings")
    private String keyFindings;

    /**
     * 建议措施
     */
    @TableField("recommendations")
    private String recommendations;

    /**
     * 分析报告路径
     */
    @TableField("report_path")
    private String reportPath;

    /**
     * 可视化图表路径（JSON格式，包含多个图表路径）
     */
    @TableField("visualization_paths")
    private String visualizationPaths;

    /**
     * 状态：0-分析中, 1-分析完成, 2-已验证, 3-已发布, 4-已归档
     */
    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("create_by")
    private String createBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField("update_by")
    private String updateBy;

    @Version
    @TableField("version")
    private Integer version;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("extend_info")
    private String extendInfo;
}