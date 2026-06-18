package cn.aiedge.erp.sales.pricing.service.algorithm;

import java.math.BigDecimal;

/**
 * 标准定价算法接口
 * 基于产品成本、市场价的标准价格计算
 */
public interface IStandardPricingAlgorithm {
    
    /**
     * 成本加成定价法
     * 价格 = 成本 × (1 + 加成率)
     * 
     * @param cost 产品成本
     * @param markupRate 加成率（例如：0.2 表示20%的加成）
     * @return 计算后的价格
     */
    BigDecimal calculateCostPlusPrice(BigDecimal cost, BigDecimal markupRate);
    
    /**
     * 目标利润率定价法
     * 价格 = 成本 / (1 - 目标利润率)
     * 
     * @param cost 产品成本
     * @param targetProfitMargin 目标利润率（例如：0.2 表示20%的利润率）
     * @return 计算后的价格
     */
    BigDecimal calculateTargetProfitPrice(BigDecimal cost, BigDecimal targetProfitMargin);
    
    /**
     * 市场参考定价法
     * 价格 = 市场参考价 × 调整系数
     * 
     * @param marketPrice 市场参考价
     * @param adjustmentFactor 调整系数（例如：1.1 表示比市场价高10%）
     * @return 计算后的价格
     */
    BigDecimal calculateMarketReferencePrice(BigDecimal marketPrice, BigDecimal adjustmentFactor);
    
    /**
     * 竞争导向定价法
     * 价格 = 竞争对手价格 × 竞争系数
     * 
     * @param competitorPrice 竞争对手价格
     * @param competitionFactor 竞争系数（小于1表示更具竞争力）
     * @return 计算后的价格
     */
    BigDecimal calculateCompetitionBasedPrice(BigDecimal competitorPrice, BigDecimal competitionFactor);
    
    /**
     * 价值定价法
     * 基于产品价值定位的价格计算
     * 
     * @param baseValue 基础价值
     * @param valueMultiplier 价值乘数
     * @param brandFactor 品牌系数
     * @param qualityFactor 质量系数
     * @return 计算后的价格
     */
    BigDecimal calculateValueBasedPrice(BigDecimal baseValue, BigDecimal valueMultiplier, 
                                       BigDecimal brandFactor, BigDecimal qualityFactor);
    
    /**
     * 分渠道定价法
     * 根据不同销售渠道定价
     * 
     * @param basePrice 基础价格
     * @param channelType 渠道类型
     * @return 计算后的价格
     */
    BigDecimal calculateChannelBasedPrice(BigDecimal basePrice, String channelType);
    
    /**
     * 季节性定价法
     * 根据季节变化定价
     * 
     * @param basePrice 基础价格
     * @param seasonFactor 季节系数
     * @param holidayFactor 节假日系数
     * @return 计算后的价格
     */
    BigDecimal calculateSeasonalPrice(BigDecimal basePrice, BigDecimal seasonFactor, BigDecimal holidayFactor);
    
    /**
     * 综合定价法（成本+市场+竞争）
     * 
     * @param cost 产品成本
     * @param marketPrice 市场参考价
     * @param competitorPrice 竞争对手价格
     * @param costWeight 成本权重（0-1）
     * @param marketWeight 市场权重（0-1）
     * @param competitionWeight 竞争权重（0-1）
     * @return 计算后的价格
     */
    BigDecimal calculateComprehensivePrice(BigDecimal cost, BigDecimal marketPrice, BigDecimal competitorPrice,
                                          BigDecimal costWeight, BigDecimal marketWeight, BigDecimal competitionWeight);
    
    /**
     * 验证价格合理性
     * 
     * @param calculatedPrice 计算出的价格
     * @param minAcceptablePrice 最低可接受价格
     * @param maxAcceptablePrice 最高可接受价格
     * @return 验证结果和调整建议
     */
    PriceValidationResult validatePrice(BigDecimal calculatedPrice, BigDecimal minAcceptablePrice, BigDecimal maxAcceptablePrice);
    
    /**
     * 价格验证结果
     */
    class PriceValidationResult {
        private final boolean valid;
        private final String message;
        private final BigDecimal adjustedPrice;
        
        public PriceValidationResult(boolean valid, String message, BigDecimal adjustedPrice) {
            this.valid = valid;
            this.message = message;
            this.adjustedPrice = adjustedPrice;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public BigDecimal getAdjustedPrice() {
            return adjustedPrice;
        }
    }
}