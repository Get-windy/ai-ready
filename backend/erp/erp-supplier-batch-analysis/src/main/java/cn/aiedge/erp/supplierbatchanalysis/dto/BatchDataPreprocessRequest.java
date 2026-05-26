package cn.aiedge.erp.supplierbatchanalysis.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 批次数据预处理请求DTO
 */
@Data
public class BatchDataPreprocessRequest {

    /**
     * 批次ID
     */
    @NotBlank(message = "批次ID不能为空")
    private String batchId;

    /**
     * 批次号
     */
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    /**
     * 产品ID
     */
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    /**
     * 原始数据（JSON格式）
     */
    @NotNull(message = "原始数据不能为空")
    private String rawData;

    /**
     * 数据类型：batch_quality-批次质量, inspection-检测数据, process-生产过程数据
     */
    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    /**
     * 数据清洗策略：
     * simple_cleaning-简单清洗, 
     * advanced_cleaning-高级清洗,
     * customized-自定义清洗规则
     */
    private String cleaningStrategy = "simple_cleaning";

    /**
     * 缺失值处理策略：
     * mean-平均值填充, 
     * median-中位数填充,
     * mode-众数填充,
     * interpolation-插值法,
     * delete-删除缺失值
     */
    private String missingValueStrategy = "mean";

    /**
     * 异常值检测方法：
     * z_score-Z分数法,
     * iqr-四分位距法,
     * isolation_forest-孤立森林,
     * autoencoder-自编码器
     */
    private String outlierDetectionMethod = "iqr";

    /**
     * 数据标准化方法：
     * min_max-最小最大标准化,
     * z_score-Z分数标准化,
     * decimal_scaling-小数定标标准化,
     * none-不标准化
     */
    private String normalizationMethod = "z_score";

    /**
     * 特征工程策略：
     * basic-基础特征,
     * polynomial-多项式特征,
     * interaction-交互特征,
     * statistical-统计特征
     */
    private String featureEngineeringStrategy = "basic";

    /**
     * 特征选择方法：
     * variance_threshold-方差阈值,
     * correlation-相关性,
     * mutual_info-互信息,
     * rf_feature_importance-随机森林特征重要性,
     * lasso-套索回归
     */
    private String featureSelectionMethod = "correlation";

    /**
     * 选择前N个特征
     */
    private Integer featureSelectionTopN = 10;

    /**
     * 时间序列处理策略：
     * smoothing-平滑处理,
     * differencing-差分处理,
     * decomposition-分解处理,
     * seasonal_adjustment-季节性调整
     */
    private String timeSeriesStrategy = "smoothing";

    /**
     * 数据分割比例（训练集:验证集:测试集）
     */
    private String splitRatio = "0.7:0.15:0.15";

    /**
     * 是否执行降维
     */
    private Boolean enableDimensionalityReduction = false;

    /**
     * 降维方法：
     * pca-主成分分析,
     * t_sne-t-SNE,
     * lda-线性判别分析,
     * umap-UMAP
     */
    private String dimensionalityReductionMethod = "pca";

    /**
     * 降维维度
     */
    private Integer targetDimensions = 3;

    /**
     * 是否保存预处理结果
     */
    private Boolean saveResult = true;

    /**
     * 是否生成预处理报告
     */
    private Boolean generateReport = true;

    /**
     * 预处理配置参数（JSON格式）
     */
    private Map<String, Object> preprocessingConfig;

    /**
     * 备注信息
     */
    private String remark;
}