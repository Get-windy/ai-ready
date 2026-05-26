package cn.aiedge.erp.supplierbatchanalysis.service;

import cn.aiedge.erp.supplierbatchanalysis.dto.BatchDataPreprocessRequest;
import cn.aiedge.erp.supplierbatchanalysis.dto.BatchDataPreprocessResult;

import java.util.List;

/**
 * 批次数据预处理服务接口
 * 提供批次数据清洗、标准化、特征工程等功能
 */
public interface DataPreprocessService {

    /**
     * 批量数据预处理
     * @param request 预处理请求
     * @return 预处理结果
     */
    BatchDataPreprocessResult preprocessBatchData(BatchDataPreprocessRequest request);

    /**
     * 数据清洗
     * @param rawData 原始数据列表
     * @param cleaningStrategy 清洗策略
     * @return 清洗后的数据
     */
    List<Object> dataCleaning(List<Object> rawData, String cleaningStrategy);

    /**
     * 缺失值处理
     * @param data 数据列表
     * @param missingValueStrategy 缺失值处理策略
     * @return 处理后的数据
     */
    List<Object> handleMissingValues(List<Object> data, String missingValueStrategy);

    /**
     * 异常值检测
     * @param data 数据列表
     * @param outlierDetectionMethod 异常值检测方法
     * @return 异常值索引列表
     */
    List<Integer> detectOutliers(List<Double> data, String outlierDetectionMethod);

    /**
     * 数据标准化
     * @param data 数据列表
     * @param normalizationMethod 标准化方法
     * @return 标准化后的数据
     */
    List<Double> dataNormalization(List<Double> data, String normalizationMethod);

    /**
     * 特征工程
     * @param rawFeatures 原始特征
     * @param featureEngineeringStrategy 特征工程策略
     * @return 特征工程后的特征
     */
    List<Object> featureEngineering(List<Object> rawFeatures, String featureEngineeringStrategy);

    /**
     * 特征选择
     * @param features 特征列表
     * @param featureSelectionMethod 特征选择方法
     * @param topN 选择前N个特征
     * @return 选择后的特征索引
     */
    List<Integer> featureSelection(List<List<Double>> features, String featureSelectionMethod, int topN);

    /**
     * 时间序列数据处理
     * @param timeSeriesData 时间序列数据
     * @param processingStrategy 处理策略
     * @return 处理后的时间序列数据
     */
    List<Object> processTimeSeriesData(List<Object> timeSeriesData, String processingStrategy);

    /**
     * 数据分割
     * @param data 原始数据
     * @param trainRatio 训练集比例
     * @param validationRatio 验证集比例
     * @param testRatio 测试集比例
     * @return 分割后的数据集
     */
    List<List<Object>> splitData(List<Object> data, double trainRatio, double validationRatio, double testRatio);
}