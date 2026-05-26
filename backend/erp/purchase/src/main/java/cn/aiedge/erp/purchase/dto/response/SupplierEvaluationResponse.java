package cn.aiedge.erp.purchase.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商评估响应DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商评估响应")
public class SupplierEvaluationResponse {

    @Schema(description = "评估记录ID")
    private Long id;

    @Schema(description = "评估编号")
    private String evaluationNo;

    @Schema(description = "供应商ID")
    private String supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "供应商等级")
    private String supplierLevel;

    @Schema(description = "评估类型")
    private String evaluationType;

    @Schema(description = "评估类型名称")
    private String evaluationTypeName;

    @Schema(description = "评估周期开始日期")
    private LocalDate periodStart;

    @Schema(description = "评估周期结束日期")
    private LocalDate periodEnd;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "评估标准版本")
    private String evaluationStandardVersion;

    @Schema(description = "质量评分（0-100）")
    private BigDecimal qualityScore;

    @Schema(description = "价格评分（0-100）")
    private BigDecimal priceScore;

    @Schema(description = "交期评分（0-100）")
    private BigDecimal deliveryScore;

    @Schema(description = "服务评分（0-100）")
    private BigDecimal serviceScore;

    @Schema(description = "技术评分（0-100）")
    private BigDecimal technicalScore;

    @Schema(description = "综合评分（0-100）")
    private BigDecimal overallScore;

    @Schema(description = "信用评分（0-100）")
    private BigDecimal creditScore;

    @Schema(description = "风险评分（0-100，越高风险越大）")
    private BigDecimal riskScore;

    @Schema(description = "推荐等级")
    private String recommendationLevel;

    @Schema(description = "推荐等级名称")
    private String recommendationLevelName;

    @Schema(description = "总体评价")
    private String overallAssessment;

    @Schema(description = "质量评价")
    private String qualityAssessment;

    @Schema(description = "价格评价")
    private String priceAssessment;

    @Schema(description = "交期评价")
    private String deliveryAssessment;

    @Schema(description = "服务评价")
    private String serviceAssessment;

    @Schema(description = "技术评价")
    private String technicalAssessment;

    @Schema(description = "信用评价")
    private String creditAssessment;

    @Schema(description = "风险评价")
    private String riskAssessment;

    @Schema(description = "优势")
    private String strengths;

    @Schema(description = "劣势")
    private String weaknesses;

    @Schema(description = "改进建议")
    private String improvementSuggestions;

    @Schema(description = "合作建议")
    private String cooperationSuggestions;

    @Schema(description = "质量关键指标（JSON）")
    private String qualityMetrics;

    @Schema(description = "交付关键指标（JSON）")
    private String deliveryMetrics;

    @Schema(description = "价格关键指标（JSON）")
    private String priceMetrics;

    @Schema(description = "服务关键指标（JSON）")
    private String serviceMetrics;

    @Schema(description = "技术关键指标（JSON）")
    private String technicalMetrics;

    @Schema(description = "信用关键指标（JSON）")
    private String creditMetrics;

    @Schema(description = "风险关键指标（JSON）")
    private String riskMetrics;

    @Schema(description = "关键指标详情")
    private MetricsDetail metricsDetail;

    @Schema(description = "评估结论")
    private String evaluationConclusion;

    @Schema(description = "评估结论名称")
    private String evaluationConclusionName;

    @Schema(description = "评估有效期至")
    private LocalDate evaluationValidUntil;

    @Schema(description = "评估人ID")
    private String evaluatorId;

    @Schema(description = "评估人姓名")
    private String evaluatorName;

    @Schema(description = "评估部门")
    private String evaluationDepartment;

    @Schema(description = "评估时间")
    private LocalDateTime evaluationTime;

    @Schema(description = "附件列表")
    private List<AttachmentInfo> attachments;

    @Schema(description = "评估报告URL")
    private String reportUrl;

    @Schema(description = "评估报告名称")
    private String reportName;

    @Schema(description = "评估报告生成时间")
    private LocalDateTime reportGeneratedTime;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "采购需求ID")
    private Long demandId;

    @Schema(description = "采购需求编号")
    private String demandNo;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "更新人ID")
    private Long updatedBy;

    @Schema(description = "创建人姓名")
    private String createdByName;

    @Schema(description = "更新人姓名")
    private String updatedByName;

    @Schema(description = "分数趋势分析")
    private ScoreTrendAnalysis scoreTrendAnalysis;

    @Schema(description = "与上次评估对比")
    private ComparisonWithPrevious comparisonWithPrevious;

    /**
     * 附件信息
     */
    @Data
    @Schema(description = "附件信息")
    public static class AttachmentInfo {
        
        @Schema(description = "附件ID")
        private Long id;
        
        @Schema(description = "附件名称")
        private String fileName;
        
        @Schema(description = "附件类型")
        private String fileType;
        
        @Schema(description = "附件大小（字节）")
        private Long fileSize;
        
        @Schema(description = "附件URL")
        private String fileUrl;
        
        @Schema(description = "附件说明")
        private String description;
        
        @Schema(description = "上传时间")
        private LocalDateTime uploadTime;
    }

    /**
     * 关键指标详情
     */
    @Data
    @Schema(description = "关键指标详情")
    public static class MetricsDetail {
        
        @Schema(description = "质量关键指标列表")
        private List<MetricItem> qualityMetrics;
        
        @Schema(description = "交付关键指标列表")
        private List<MetricItem> deliveryMetrics;
        
        @Schema(description = "价格关键指标列表")
        private List<MetricItem> priceMetrics;
        
        @Schema(description = "服务关键指标列表")
        private List<MetricItem> serviceMetrics;
        
        @Schema(description = "技术关键指标列表")
        private List<MetricItem> technicalMetrics;
        
        @Schema(description = "信用关键指标列表")
        private List<MetricItem> creditMetrics;
        
        @Schema(description = "风险关键指标列表")
        private List<MetricItem> riskMetrics;
    }

    /**
     * 指标项
     */
    @Data
    @Schema(description = "指标项")
    public static class MetricItem {
        
        @Schema(description = "指标名称")
        private String name;
        
        @Schema(description = "指标值")
        private String value;
        
        @Schema(description = "单位")
        private String unit;
        
        @Schema(description = "目标值")
        private String target;
        
        @Schema(description = "是否达标")
        private Boolean meetsTarget;
        
        @Schema(description = "权重（0-100）")
        private BigDecimal weight;
        
        @Schema(description = "得分（0-100）")
        private BigDecimal score;
        
        @Schema(description = "趋势")
        private String trend;
        
        @Schema(description = "说明")
        private String description;
    }

    /**
     * 分数趋势分析
     */
    @Data
    @Schema(description = "分数趋势分析")
    public static class ScoreTrendAnalysis {
        
        @Schema(description = "综合评分趋势")
        private TrendData overallScoreTrend;
        
        @Schema(description = "质量评分趋势")
        private TrendData qualityScoreTrend;
        
        @Schema(description = "交期评分趋势")
        private TrendData deliveryScoreTrend;
        
        @Schema(description = "价格评分趋势")
        private TrendData priceScoreTrend;
        
        @Schema(description = "服务评分趋势")
        private TrendData serviceScoreTrend;
        
        @Schema(description = "技术评分趋势")
        private TrendData technicalScoreTrend;
    }

    /**
     * 趋势数据
     */
    @Data
    @Schema(description = "趋势数据")
    public static class TrendData {
        
        @Schema(description = "当前值")
        private BigDecimal currentValue;
        
        @Schema(description = "上次值")
        private BigDecimal previousValue;
        
        @Schema(description = "历史最高值")
        private BigDecimal historicalMax;
        
        @Schema(description = "历史最低值")
        private BigDecimal historicalMin;
        
        @Schema(description = "变化值")
        private BigDecimal changeValue;
        
        @Schema(description = "变化百分比")
        private BigDecimal changePercentage;
        
        @Schema(description = "趋势（上升/下降/稳定）")
        private String trend;
        
        @Schema(description = "趋势强度（强/中/弱）")
        private String trendStrength;
        
        @Schema(description = "历史数据点")
        private List<HistoricalDataPoint> historicalData;
    }

    /**
     * 历史数据点
     */
    @Data
    @Schema(description = "历史数据点")
    public static class HistoricalDataPoint {
        
        @Schema(description = "时间点")
        private String timePoint;
        
        @Schema(description = "值")
        private BigDecimal value;
        
        @Schema(description = "评估编号")
        private String evaluationNo;
        
        @Schema(description = "评估类型")
        private String evaluationType;
    }

    /**
     * 与上次评估对比
     */
    @Data
    @Schema(description = "与上次评估对比")
    public static class ComparisonWithPrevious {
        
        @Schema(description = "上次评估ID")
        private Long previousEvaluationId;
        
        @Schema(description = "上次评估编号")
        private String previousEvaluationNo;
        
        @Schema(description = "上次评估时间")
        private LocalDateTime previousEvaluationTime;
        
        @Schema(description = "综合评分变化")
        private ScoreChange overallScoreChange;
        
        @Schema(description = "质量评分变化")
        private ScoreChange qualityScoreChange;
        
        @Schema(description = "交期评分变化")
        private ScoreChange deliveryScoreChange;
        
        @Schema(description = "价格评分变化")
        private ScoreChange priceScoreChange;
        
        @Schema(description = "服务评分变化")
        private ScoreChange serviceScoreChange;
        
        @Schema(description = "技术评分变化")
        private ScoreChange technicalScoreChange;
        
        @Schema(description = "信用评分变化")
        private ScoreChange creditScoreChange;
        
        @Schema(description = "风险评分变化")
        private ScoreChange riskScoreChange;
        
        @Schema(description = "主要改进点")
        private List<String> mainImprovements;
        
        @Schema(description = "主要退步点")
        private List<String> mainDeclines;
        
        @Schema(description = "等级变化")
        private String levelChange;
        
        @Schema(description = "推荐等级变化")
        private String recommendationLevelChange;
    }

    /**
     * 分数变化
     */
    @Data
    @Schema(description = "分数变化")
    public static class ScoreChange {
        
        @Schema(description = "当前值")
        private BigDecimal currentValue;
        
        @Schema(description = "上次值")
        private BigDecimal previousValue;
        
        @Schema(description = "变化值")
        private BigDecimal changeValue;
        
        @Schema(description = "变化百分比")
        private BigDecimal changePercentage;
        
        @Schema(description = "变化方向")
        private String direction;
        
        @Schema(description = "变化幅度")
        private String magnitude;
        
        @Schema(description = "是否显著变化")
        private Boolean significantChange;
    }
}