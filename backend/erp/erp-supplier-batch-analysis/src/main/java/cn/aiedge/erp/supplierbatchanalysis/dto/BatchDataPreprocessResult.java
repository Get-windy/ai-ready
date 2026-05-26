package cn.aiedge.erp.supplierbatchanalysis.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批次数据预处理结果DTO
 */
@Data
public class BatchDataPreprocessResult {

    /**
     * 预处理任务ID
     */
    private String preprocessTaskId;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 预处理状态：
     * SUCCESS-成功,
     * PARTIAL_SUCCESS-部分成功,
     * FAILED-失败
     */
    private String status;

    /**
     * 预处理开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预处理结束时间
     */
    private LocalDateTime endTime;

    /**
     * 预处理耗时（毫秒）
     */
    private Long durationMillis;

    /**
     * 原始数据行数
     */
    private Integer originalDataCount;

    /**
     * 处理后数据行数
     */
    private Integer processedDataCount;

    /**
     * 缺失值统计
     */
    private Map<String, Integer> missingValueStats;

    /**
     * 异常值统计
     */
    private Map<String, Integer> outlierStats;

    /**
     * 异常值索引列表
     */
    private List<Integer> outlierIndices;

    /**
     * 数据质量评分（0-100）
     */
    private Integer dataQualityScore;

    /**
     * 数据质量评价
     */
    private String dataQualityAssessment;

    /**
     * 特征数量（处理前）
     */
    private Integer originalFeatureCount;

    /**
     * 特征数量（处理后）
     */
    private Integer processedFeatureCount;

    /**
     * 选择后的特征列表
     */
    private List<String> selectedFeatures;

    /**
     * 特征重要性评分
     */
    private Map<String, Double> featureImportanceScores;

    /**
     * 标准化参数
     */
    private Map<String, Object> normalizationParams;

    /**
     * 缺失值处理参数
     */
    private Map<String, Object> missingValueParams;

    /**
     * 异常值处理参数
     */
    private Map<String, Object> outlierParams;

    /**
     * 预处理配置快照
     */
    private Map<String, Object> preprocessingConfig;

    /**
     * 预处理后的数据（可存储为JSON或文件路径）
     */
    private String processedData;

    /**
     * 数据存储路径
     */
    private String dataStoragePath;

    /**
     * 数据格式信息
     */
    private String dataFormat;

    /**
     * 特征向量维度
     */
    private Integer featureDimensions;

    /**
     * 数据分布统计
     */
    private Map<String, Object> dataDistributionStats;

    /**
     * 数据统计摘要
     */
    private Map<String, Map<String, Double>> dataSummaryStats;

    /**
     * 时间序列处理结果
     */
    private Map<String, Object> timeSeriesProcessingResult;

    /**
     * 降维结果（如果执行了降维）
     */
    private Map<String, Object> dimensionalityReductionResult;

    /**
     * 可视化数据（图表数据）
     */
    private Map<String, Object> visualizationData;

    /**
     * 预处理报告（Markdown格式）
     */
    private String preprocessingReport;

    /**
     * 报告文件路径
     */
    private String reportFilePath;

    /**
     * 警告信息列表
     */
    private List<String> warnings;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 建议信息列表
     */
    private List<String> recommendations;

    /**
     * 模型训练建议
     */
    private Map<String, Object> modelTrainingRecommendations;

    /**
     * 下一步处理建议
     */
    private List<String> nextStepRecommendations;

    /**
     * 是否可用于模型训练
     */
    private Boolean readyForModelTraining;

    /**
     * 模型训练数据路径
     */
    private String trainingDataPath;

    /**
     * 预处理元数据
     */
    private Map<String, Object> metadata;

    /**
     * 性能指标
     */
    private Map<String, Double> performanceMetrics;

    /**
     * 资源使用情况
     */
    private Map<String, Object> resourceUsage;

    /**
     * 版本信息
     */
    private String preprocessVersion = "1.0.0";

    /**
     * 预处理算法版本
     */
    private Map<String, String> algorithmVersions;

    /**
     * 是否缓存预处理结果
     */
    private Boolean cached = false;

    /**
     * 缓存有效期（毫秒）
     */
    private Long cacheExpirationMillis;

    /**
     * 任务执行者
     */
    private String executor;

    /**
     * 备注信息
     */
    private String remark;
}