package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Map;

/**
 * 价格推荐请求DTO
 */
@Data
public class PriceRecommendationRequestDto {
    
    private String requestId; // 请求ID (可选，自动生成)
    private Long productId; // 产品ID (必填)
    private String productCode; // 产品编码 (可选)
    private String productName; // 产品名称 (可选)
    
    // 基础信息
    private Integer quantity; // 数量 (可选)
    private String currency; // 货币 (默认: CNY)
    private String customerId; // 客户ID (可选)
    private String customerLevel; // 客户等级: vip, premium, regular (可选)
    private String customerSegment; // 客户细分 (可选)
    
    // 成本信息
    private Double unitCost; // 单位成本 (可选)
    private Double targetMargin; // 目标利润率 (可选，如0.2表示20%)
    private Double minMargin; // 最低利润率 (可选)
    private Double maxMargin; // 最高利润率 (可选)
    
    // 市场信息
    private String marketCondition; // 市场条件: normal, competitive, high_demand, low_demand, seasonal (可选)
    private Double competitorPrice; // 竞争对手价格 (可选)
    private Double marketAveragePrice; // 市场平均价格 (可选)
    private String marketTrend; // 市场趋势: rising, falling, stable (可选)
    
    // 策略偏好
    private String preferredStrategy; // 偏好策略: cost_plus, market_based, value_based, competitor_based (可选)
    private Map<String, Object> strategyConstraints; // 策略约束 (可选)
    private Boolean includeAlternatives; // 是否包含替代方案 (默认: true)
    private Integer alternativeCount; // 替代方案数量 (默认: 3)
    
    // 业务规则
    private Double minPrice; // 最低价格限制 (可选)
    private Double maxPrice; // 最高价格限制 (可选)
    private Boolean enforcePriceFloor; // 是否强制执行价格底线 (可选)
    private Boolean enforcePriceCeiling; // 是否强制执行价格上限 (可选)
    
    // 上下文信息
    private String salesChannel; // 销售渠道: online, offline, wholesale, retail (可选)
    private String region; // 区域 (可选)
    private String season; // 季节 (可选)
    private String promotionType; // 促销类型 (可选)
    
    // 历史数据参考
    private Boolean useHistoricalData; // 是否使用历史数据 (默认: true)
    private Integer historicalPeriodDays; // 历史数据周期(天) (默认: 90)
    
    // 高级选项
    private Map<String, Object> customParameters; // 自定义参数 (可选)
    private Boolean includeRiskAssessment; // 是否包含风险评估 (默认: true)
    private Boolean includeConfidenceScores; // 是否包含置信度分数 (默认: true)
    
    /**
     * 验证请求参数
     */
    public boolean isValid() {
        if (productId == null || productId <= 0) {
            return false;
        }
        
        if (quantity != null && quantity < 0) {
            return false;
        }
        
        if (targetMargin != null && (targetMargin < 0 || targetMargin > 1)) {
            return false;
        }
        
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取请求描述
     */
    public String getRequestDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("产品ID: ").append(productId);
        
        if (productName != null) {
            desc.append(", 产品名称: ").append(productName);
        }
        
        if (quantity != null) {
            desc.append(", 数量: ").append(quantity);
        }
        
        if (customerLevel != null) {
            desc.append(", 客户等级: ").append(customerLevel);
        }
        
        return desc.toString();
    }
    
    /**
     * 获取市场条件描述
     */
    public String getMarketConditionDescription() {
        if (marketCondition == null) return "正常市场";
        
        switch (marketCondition.toLowerCase()) {
            case "normal": return "正常市场";
            case "competitive": return "竞争激烈";
            case "high_demand": return "需求旺盛";
            case "low_demand": return "需求低迷";
            case "seasonal": return "季节性波动";
            default: return marketCondition;
        }
    }
    
    /**
     * 获取偏好策略描述
     */
    public String getPreferredStrategyDescription() {
        if (preferredStrategy == null) return "自动选择";
        
        switch (preferredStrategy.toLowerCase()) {
            case "cost_plus": return "成本加成定价";
            case "market_based": return "市场导向定价";
            case "value_based": return "价值导向定价";
            case "competitor_based": return "竞争对手导向定价";
            default: return preferredStrategy;
        }
    }
    
    /**
     * 获取货币代码
     */
    public String getCurrencyCode() {
        return currency != null ? currency.toUpperCase() : "CNY";
    }
    
    /**
     * 获取默认值
     */
    public Integer getQuantityOrDefault() {
        return quantity != null ? quantity : 1;
    }
    
    /**
     * 获取目标利润率
     */
    public Double getTargetMarginOrDefault() {
        return targetMargin != null ? targetMargin : 0.2; // 默认20%
    }
    
    /**
     * 是否包含替代方案
     */
    public boolean shouldIncludeAlternatives() {
        return includeAlternatives != null ? includeAlternatives : true;
    }
    
    /**
     * 获取替代方案数量
     */
    public int getAlternativeCountOrDefault() {
        return alternativeCount != null ? alternativeCount : 3;
    }
    
    /**
     * 是否包含风险评估
     */
    public boolean shouldIncludeRiskAssessment() {
        return includeRiskAssessment != null ? includeRiskAssessment : true;
    }
}