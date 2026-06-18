package cn.aiedge.erp.batchsn.ai.service;

import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationDto;
import cn.aiedge.erp.batchsn.ai.dto.PriceRecommendationRequestDto;

import java.util.List;
import java.util.Map;

/**
 * 价格策略智能推荐服务接口
 */
public interface PriceRecommendationService {
    
    /**
     * 推荐批次价格策略
     * @param request 价格推荐请求
     * @return 价格推荐结果
     */
    PriceRecommendationDto recommendPrice(PriceRecommendationRequestDto request);
    
    /**
     * 批量推荐价格策略
     * @param requests 价格推荐请求列表
     * @return 价格推荐结果列表
     */
    List<PriceRecommendationDto> recommendPriceBatch(List<PriceRecommendationRequestDto> requests);
    
    /**
     * 获取价格推荐解释
     * @param recommendationId 推荐ID
     * @return 推荐解释
     */
    String getRecommendationExplanation(String recommendationId);
    
    /**
     * 评估推荐效果
     * @param recommendationId 推荐ID
     * @param actualPrice 实际价格
     * @param actualProfit 实际利润
     * @return 评估结果
     */
    RecommendationEvaluation evaluateRecommendation(String recommendationId, 
                                                   Double actualPrice, 
                                                   Double actualProfit);
    
    /**
     * 获取市场价格趋势
     * @param productId 产品ID
     * @param days 天数
     * @return 价格趋势
     */
    List<PriceTrend> getMarketPriceTrend(Long productId, int days);
    
    /**
     * 获取竞争对手价格分析
     * @param productId 产品ID
     * @return 竞争对手价格分析
     */
    CompetitorPriceAnalysis getCompetitorPriceAnalysis(Long productId);
    
    /**
     * 训练价格推荐模型
     * @param historicalData 历史数据
     * @return 训练结果
     */
    boolean trainModel(List<HistoricalPriceData> historicalData);
    
    /**
     * 更新价格因子权重
     * @param factorWeights 因子权重
     * @return 更新结果
     */
    boolean updateFactorWeights(Map<String, Double> factorWeights);
    
    /**
     * 获取推荐策略统计
     * @return 策略统计
     */
    RecommendationStats getRecommendationStats();
    
    /**
     * 价格趋势
     */
    class PriceTrend {
        private String date;
        private Double averagePrice;
        private Double minPrice;
        private Double maxPrice;
        private Integer transactionCount;
        
        public PriceTrend(String date, Double averagePrice) {
            this.date = date;
            this.averagePrice = averagePrice;
        }
        
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getAveragePrice() { return averagePrice; }
        public void setAveragePrice(Double averagePrice) { this.averagePrice = averagePrice; }
        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
        public Integer getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
    }
    
    /**
     * 竞争对手价格分析
     */
    class CompetitorPriceAnalysis {
        private Long productId;
        private List<CompetitorPrice> competitorPrices;
        private Double ourPrice;
        private Double marketAverage;
        private Double pricePosition; // 价格位置百分比
        private String recommendation;
        
        // getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public List<CompetitorPrice> getCompetitorPrices() { return competitorPrices; }
        public void setCompetitorPrices(List<CompetitorPrice> competitorPrices) { this.competitorPrices = competitorPrices; }
        public Double getOurPrice() { return ourPrice; }
        public void setOurPrice(Double ourPrice) { this.ourPrice = ourPrice; }
        public Double getMarketAverage() { return marketAverage; }
        public void setMarketAverage(Double marketAverage) { this.marketAverage = marketAverage; }
        public Double getPricePosition() { return pricePosition; }
        public void setPricePosition(Double pricePosition) { this.pricePosition = pricePosition; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
    
    /**
     * 竞争对手价格
     */
    class CompetitorPrice {
        private String competitorName;
        private Double price;
        private String currency;
        private String date;
        private Double rating; // 竞争对手评分
        
        // getters and setters
        public String getCompetitorName() { return competitorName; }
        public void setCompetitorName(String competitorName) { this.competitorName = competitorName; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
    }
    
    /**
     * 历史价格数据
     */
    class HistoricalPriceData {
        private Long productId;
        private Double price;
        private Integer quantity;
        private Double cost;
        private Double profit;
        private String date;
        private Map<String, Object> features; // 特征数据
        
        // getters and setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getCost() { return cost; }
        public void setCost(Double cost) { this.cost = cost; }
        public Double getProfit() { return profit; }
        public void setProfit(Double profit) { this.profit = profit; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Map<String, Object> getFeatures() { return features; }
        public void setFeatures(Map<String, Object> features) { this.features = features; }
    }
    
    /**
     * 推荐评估结果
     */
    class RecommendationEvaluation {
        private String recommendationId;
        private Double predictedPrice;
        private Double actualPrice;
        private Double priceDeviation;
        private Double predictedProfit;
        private Double actualProfit;
        private Double profitDeviation;
        private Double accuracyScore;
        private String evaluationComment;
        
        // getters and setters
        public String getRecommendationId() { return recommendationId; }
        public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
        public Double getPredictedPrice() { return predictedPrice; }
        public void setPredictedPrice(Double predictedPrice) { this.predictedPrice = predictedPrice; }
        public Double getActualPrice() { return actualPrice; }
        public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }
        public Double getPriceDeviation() { return priceDeviation; }
        public void setPriceDeviation(Double priceDeviation) { this.priceDeviation = priceDeviation; }
        public Double getPredictedProfit() { return predictedProfit; }
        public void setPredictedProfit(Double predictedProfit) { this.predictedProfit = predictedProfit; }
        public Double getActualProfit() { return actualProfit; }
        public void setActualProfit(Double actualProfit) { this.actualProfit = actualProfit; }
        public Double getProfitDeviation() { return profitDeviation; }
        public void setProfitDeviation(Double profitDeviation) { this.profitDeviation = profitDeviation; }
        public Double getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(Double accuracyScore) { this.accuracyScore = accuracyScore; }
        public String getEvaluationComment() { return evaluationComment; }
        public void setEvaluationComment(String evaluationComment) { this.evaluationComment = evaluationComment; }
    }
    
    /**
     * 推荐策略统计
     */
    class RecommendationStats {
        private Integer totalRecommendations;
        private Integer acceptedRecommendations;
        private Double averagePriceDeviation;
        private Double averageProfitDeviation;
        private Double overallAccuracy;
        private Map<String, Integer> strategyCounts;
        private Map<String, Double> strategyAccuracy;
        
        // getters and setters
        public Integer getTotalRecommendations() { return totalRecommendations; }
        public void setTotalRecommendations(Integer totalRecommendations) { this.totalRecommendations = totalRecommendations; }
        public Integer getAcceptedRecommendations() { return acceptedRecommendations; }
        public void setAcceptedRecommendations(Integer acceptedRecommendations) { this.acceptedRecommendations = acceptedRecommendations; }
        public Double getAveragePriceDeviation() { return averagePriceDeviation; }
        public void setAveragePriceDeviation(Double averagePriceDeviation) { this.averagePriceDeviation = averagePriceDeviation; }
        public Double getAverageProfitDeviation() { return averageProfitDeviation; }
        public void setAverageProfitDeviation(Double averageProfitDeviation) { this.averageProfitDeviation = averageProfitDeviation; }
        public Double getOverallAccuracy() { return overallAccuracy; }
        public void setOverallAccuracy(Double overallAccuracy) { this.overallAccuracy = overallAccuracy; }
        public Map<String, Integer> getStrategyCounts() { return strategyCounts; }
        public void setStrategyCounts(Map<String, Integer> strategyCounts) { this.strategyCounts = strategyCounts; }
        public Map<String, Double> getStrategyAccuracy() { return strategyAccuracy; }
        public void setStrategyAccuracy(Map<String, Double> strategyAccuracy) { this.strategyAccuracy = strategyAccuracy; }
    }
}