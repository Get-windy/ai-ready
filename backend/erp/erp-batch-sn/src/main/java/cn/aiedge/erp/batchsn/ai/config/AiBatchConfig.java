package cn.aiedge.erp.batchsn.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 批次管理AI智能配置类
 */
@Configuration
@ConfigurationProperties(prefix = "ai.batch")
public class AiBatchConfig {
    
    // 质量预测模型配置
    private QualityPrediction qualityPrediction = new QualityPrediction();
    
    // 价格推荐配置
    private PriceRecommendation priceRecommendation = new PriceRecommendation();
    
    // 异常检测配置
    private AnomalyDetection anomalyDetection = new AnomalyDetection();
    
    // 智能搜索配置
    private IntelligentSearch intelligentSearch = new IntelligentSearch();
    
    // 模型训练配置
    private ModelTraining modelTraining = new ModelTraining();
    
    public static class QualityPrediction {
        private boolean enabled = true;
        private String modelPath = "models/batch_quality_model.pkl";
        private int predictionWindowDays = 30;
        private double confidenceThreshold = 0.8;
        private int minTrainingSamples = 100;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModelPath() { return modelPath; }
        public void setModelPath(String modelPath) { this.modelPath = modelPath; }
        public int getPredictionWindowDays() { return predictionWindowDays; }
        public void setPredictionWindowDays(int predictionWindowDays) { this.predictionWindowDays = predictionWindowDays; }
        public double getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        public int getMinTrainingSamples() { return minTrainingSamples; }
        public void setMinTrainingSamples(int minTrainingSamples) { this.minTrainingSamples = minTrainingSamples; }
    }
    
    public static class PriceRecommendation {
        private boolean enabled = true;
        private String algorithm = "xgboost";
        private Map<String, Double> priceFactors;
        private double minPriceMargin = 0.1;
        private double maxPriceMargin = 0.3;
        private boolean considerMarketTrend = true;
        private boolean considerCompetitorPrice = true;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public Map<String, Double> getPriceFactors() { return priceFactors; }
        public void setPriceFactors(Map<String, Double> priceFactors) { this.priceFactors = priceFactors; }
        public double getMinPriceMargin() { return minPriceMargin; }
        public void setMinPriceMargin(double minPriceMargin) { this.minPriceMargin = minPriceMargin; }
        public double getMaxPriceMargin() { return maxPriceMargin; }
        public void setMaxPriceMargin(double maxPriceMargin) { this.maxPriceMargin = maxPriceMargin; }
        public boolean isConsiderMarketTrend() { return considerMarketTrend; }
        public void setConsiderMarketTrend(boolean considerMarketTrend) { this.considerMarketTrend = considerMarketTrend; }
        public boolean isConsiderCompetitorPrice() { return considerCompetitorPrice; }
        public void setConsiderCompetitorPrice(boolean considerCompetitorPrice) { this.considerCompetitorPrice = considerCompetitorPrice; }
    }
    
    public static class AnomalyDetection {
        private boolean enabled = true;
        private String algorithm = "isolation_forest";
        private double contamination = 0.1;
        private int windowSize = 7;
        private double threshold = 0.75;
        private boolean realTimeDetection = true;
        private int alertCooldownMinutes = 30;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public double getContamination() { return contamination; }
        public void setContamination(double contamination) { this.contamination = contamination; }
        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public boolean isRealTimeDetection() { return realTimeDetection; }
        public void setRealTimeDetection(boolean realTimeDetection) { this.realTimeDetection = realTimeDetection; }
        public int getAlertCooldownMinutes() { return alertCooldownMinutes; }
        public void setAlertCooldownMinutes(int alertCooldownMinutes) { this.alertCooldownMinutes = alertCooldownMinutes; }
    }
    
    public static class IntelligentSearch {
        private boolean enabled = true;
        private String searchAlgorithm = "semantic_similarity";
        private boolean useVectorSearch = true;
        private int maxResults = 50;
        private double similarityThreshold = 0.7;
        private boolean enableFuzzySearch = true;
        private boolean enableSemanticExpansion = true;
        
        // getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSearchAlgorithm() { return searchAlgorithm; }
        public void setSearchAlgorithm(String searchAlgorithm) { this.searchAlgorithm = searchAlgorithm; }
        public boolean isUseVectorSearch() { return useVectorSearch; }
        public void setUseVectorSearch(boolean useVectorSearch) { this.useVectorSearch = useVectorSearch; }
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        public boolean isEnableFuzzySearch() { return enableFuzzySearch; }
        public void setEnableFuzzySearch(boolean enableFuzzySearch) { this.enableFuzzySearch = enableFuzzySearch; }
        public boolean isEnableSemanticExpansion() { return enableSemanticExpansion; }
        public void setEnableSemanticExpansion(boolean enableSemanticExpansion) { this.enableSemanticExpansion = enableSemanticExpansion; }
    }
    
    public static class ModelTraining {
        private boolean autoRetrain = true;
        private int retrainIntervalDays = 7;
        private int minSamplesForRetrain = 50;
        private String trainingDataPath = "data/batch_training/";
        private String validationSplit = "0.2";
        private boolean crossValidation = true;
        private int crossValidationFolds = 5;
        
        // getters and setters
        public boolean isAutoRetrain() { return autoRetrain; }
        public void setAutoRetrain(boolean autoRetrain) { this.autoRetrain = autoRetrain; }
        public int getRetrainIntervalDays() { return retrainIntervalDays; }
        public void setRetrainIntervalDays(int retrainIntervalDays) { this.retrainIntervalDays = retrainIntervalDays; }
        public int getMinSamplesForRetrain() { return minSamplesForRetrain; }
        public void setMinSamplesForRetrain(int minSamplesForRetrain) { this.minSamplesForRetrain = minSamplesForRetrain; }
        public String getTrainingDataPath() { return trainingDataPath; }
        public void setTrainingDataPath(String trainingDataPath) { this.trainingDataPath = trainingDataPath; }
        public String getValidationSplit() { return validationSplit; }
        public void setValidationSplit(String validationSplit) { this.validationSplit = validationSplit; }
        public boolean isCrossValidation() { return crossValidation; }
        public void setCrossValidation(boolean crossValidation) { this.crossValidation = crossValidation; }
        public int getCrossValidationFolds() { return crossValidationFolds; }
        public void setCrossValidationFolds(int crossValidationFolds) { this.crossValidationFolds = crossValidationFolds; }
    }
    
    // getters and setters for main class
    public QualityPrediction getQualityPrediction() { return qualityPrediction; }
    public void setQualityPrediction(QualityPrediction qualityPrediction) { this.qualityPrediction = qualityPrediction; }
    public PriceRecommendation getPriceRecommendation() { return priceRecommendation; }
    public void setPriceRecommendation(PriceRecommendation priceRecommendation) { this.priceRecommendation = priceRecommendation; }
    public AnomalyDetection getAnomalyDetection() { return anomalyDetection; }
    public void setAnomalyDetection(AnomalyDetection anomalyDetection) { this.anomalyDetection = anomalyDetection; }
    public IntelligentSearch getIntelligentSearch() { return intelligentSearch; }
    public void setIntelligentSearch(IntelligentSearch intelligentSearch) { this.intelligentSearch = intelligentSearch; }
    public ModelTraining getModelTraining() { return modelTraining; }
    public void setModelTraining(ModelTraining modelTraining) { this.modelTraining = modelTraining; }
}