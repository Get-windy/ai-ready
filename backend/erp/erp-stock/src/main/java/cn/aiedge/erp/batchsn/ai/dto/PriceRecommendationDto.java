package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 价格推荐结果DTO
 */
@Data
public class PriceRecommendationDto {
    
    private String requestId; // 请求ID
    private Long productId; // 产品ID
    private String productCode; // 产品编码
    private String productName; // 产品名称
    
    // 推荐价格信息
    private Double recommendedPrice; // 推荐价格
    private Double confidenceScore; // 置信度分数 (0-1)
    private String currency; // 货币
    
    // 价格范围
    private Double minPrice; // 最低价格
    private Double maxPrice; // 最高价格
    private Double averagePrice; // 平均价格
    
    // 策略信息
    private String strategyName; // 策略名称
    private Map<String, Object> strategyParameters; // 策略参数
    
    // 市场因素
    private String marketCondition; // 市场条件
    private Double demandFactor; // 需求因子
    private Double competitionFactor; // 竞争因子
    private Double seasonalityFactor; // 季节性因子
    
    // 成本因素
    private Double costBase; // 成本基础
    private Double targetMargin; // 目标利润率
    private Double actualMargin; // 实际利润率
    
    // 客户因素
    private String customerSegment; // 客户细分
    private Double customerDiscount; // 客户折扣
    private String customerLevel; // 客户等级
    
    // 数量因素
    private Integer quantity; // 数量
    private Double quantityDiscount; // 数量折扣
    
    // 时间因素
    private Date generatedAt; // 生成时间
    private Date expiresAt; // 过期时间
    private String validityPeriod; // 有效期
    
    // 替代方案
    private List<AlternativePrice> alternativePrices; // 替代价格方案
    
    // 风险评估
    private RiskAssessment riskAssessment; // 风险评估
    
    // 计算细节
    private Map<String, Object> calculationDetails; // 计算细节
    
    // 建议
    private List<String> recommendations; // 建议列表
    
    // 元数据
    private String modelVersion; // 模型版本
    private String algorithmUsed; // 使用算法
    private Long processingTimeMs; // 处理时间(毫秒)
    
    /**
     * 替代价格方案
     */
    @Data
    public static class AlternativePrice {
        private String scenario; // 场景描述
        private Double price; // 价格
        private Double confidence; // 置信度
        private String rationale; // 理由
        private Map<String, Object> parameters; // 参数
        
        public AlternativePrice() {}
        
        public AlternativePrice(String scenario, Double price, Double confidence) {
            this.scenario = scenario;
            this.price = price;
            this.confidence = confidence;
        }
    }
    
    /**
     * 风险评估
     */
    @Data
    public static class RiskAssessment {
        private Double riskScore; // 风险分数 (0-1)
        private String riskLevel; // 风险等级: low, medium, high
        private List<String> riskFactors; // 风险因素
        private List<String> mitigationStrategies; // 缓解策略
        
        public RiskAssessment() {}
        
        public RiskAssessment(Double riskScore, String riskLevel) {
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
        }
    }
    
    /**
     * 获取推荐级别
     */
    public String getRecommendationLevel() {
        if (confidenceScore == null) return "未知";
        
        if (confidenceScore >= 0.9) return "强烈推荐";
        else if (confidenceScore >= 0.8) return "推荐";
        else if (confidenceScore >= 0.7) return "建议";
        else if (confidenceScore >= 0.6) return "谨慎考虑";
        else return "不推荐";
    }
    
    /**
     * 判断是否在合理范围内
     */
    public boolean isInReasonableRange() {
        if (recommendedPrice == null || minPrice == null || maxPrice == null) {
            return false;
        }
        return recommendedPrice >= minPrice && recommendedPrice <= maxPrice;
    }
    
    /**
     * 获取价格相对于平均值的偏差
     */
    public Double getPriceDeviationFromAverage() {
        if (recommendedPrice == null || averagePrice == null || averagePrice == 0) {
            return null;
        }
        return (recommendedPrice - averagePrice) / averagePrice;
    }
    
    /**
     * 获取风险等级描述
     */
    public String getRiskLevelDescription() {
        if (riskAssessment == null || riskAssessment.getRiskLevel() == null) {
            return "未评估";
        }
        
        switch (riskAssessment.getRiskLevel().toLowerCase()) {
            case "low": return "低风险";
            case "medium": return "中等风险";
            case "high": return "高风险";
            default: return riskAssessment.getRiskLevel();
        }
    }
    
    /**
     * 获取格式化后的推荐价格
     */
    public String getFormattedRecommendedPrice() {
        if (recommendedPrice == null || currency == null) {
            return "N/A";
        }
        return String.format("%.2f %s", recommendedPrice, currency);
    }
    
    /**
     * 获取格式化后的价格范围
     */
    public String getFormattedPriceRange() {
        if (minPrice == null || maxPrice == null || currency == null) {
            return "N/A";
        }
        return String.format("%.2f - %.2f %s", minPrice, maxPrice, currency);
    }
}