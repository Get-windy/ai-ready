package cn.aiedge.erp.supplierbatchanalysis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批次质量预测结果实体
 * 存储基于供应商绩效的批次质量预测结果
 *
 * @author team-member
 * @date 2026-04-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("batch_quality_prediction_result")
public class BatchQualityPredictionResult {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预测批次号
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 预测批次ID
     */
    @TableField("batch_id")
    private Long batchId;

    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 产品编码
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 产品名称
     */
    @TableField("product_name")
    private String productName;

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
     * 预测日期
     */
    @TableField("prediction_date")
    private LocalDateTime predictionDate;

    /**
     * 预测模型ID
     */
    @TableField("model_id")
    private String modelId;

    /**
     * 预测模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 供应商绩效得分
     */
    @TableField("supplier_performance_score")
    private BigDecimal supplierPerformanceScore;

    /**
     * 供应商绩效等级
     */
    @TableField("supplier_performance_level")
    private String supplierPerformanceLevel;

    /**
     * 预测合格率
     */
    @TableField("predicted_qualification_rate")
    private BigDecimal predictedQualificationRate;

    /**
     * 预测质量得分
     */
    @TableField("predicted_quality_score")
    private BigDecimal predictedQualityScore;

    /**
     * 预测质量等级
     */
    @TableField("predicted_quality_level")
    private String predictedQualityLevel;

    /**
     * 预测置信度
     */
    @TableField("prediction_confidence")
    private BigDecimal predictionConfidence;

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
     * 主要风险因素
     */
    @TableField("risk_factors")
    private String riskFactors;

    /**
     * 风险等级：LOW-低风险, MEDIUM-中风险, HIGH-高风险
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 预警等级：NONE-无预警, LOW-低预警, MEDIUM-中预警, HIGH-高预警, CRITICAL-严重预警
     */
    @TableField("warning_level")
    private String warningLevel;

    /**
     * 建议措施
     */
    @TableField("recommended_actions")
    private String recommendedActions;

    /**
     * 实际合格率（后续填充）
     */
    @TableField("actual_qualification_rate")
    private BigDecimal actualQualificationRate;

    /**
     * 实际质量得分（后续填充）
     */
    @TableField("actual_quality_score")
    private BigDecimal actualQualityScore;

    /**
     * 预测准确性（实际与预测对比）
     */
    @TableField("prediction_accuracy")
    private BigDecimal predictionAccuracy;

    /**
     * 预测偏差
     */
    @TableField("prediction_bias")
    private BigDecimal predictionBias;

    /**
     * 模型评估指标（JSON格式）
     * 例如: {"mae": 0.05, "mse": 0.08, "rmse": 0.28, "r2": 0.89}
     */
    @TableField("model_evaluation_metrics")
    private String modelEvaluationMetrics;

    /**
     * 特征重要性（JSON格式）
     */
    @TableField("feature_importance")
    private String featureImportance;

    /**
     * 预测详情（JSON格式）
     */
    @TableField("prediction_details")
    private String predictionDetails;

    /**
     * 状态：0-已预测, 1-实际数据已填充, 2-已验证, 3-已完成
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