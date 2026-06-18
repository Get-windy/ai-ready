package cn.aiedge.erp.sales.pricing.service.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 标准定价算法实现类
 */
@Slf4j
@Service
public class StandardPricingAlgorithmImpl implements IStandardPricingAlgorithm {
    
    // 渠道系数定义
    private static final Map<String, BigDecimal> CHANNEL_FACTORS = new HashMap<>();
    static {
        CHANNEL_FACTORS.put("online_direct", BigDecimal.valueOf(0.95));    // 线上直销：95%
        CHANNEL_FACTORS.put("offline_store", BigDecimal.valueOf(1.05));    // 线下门店：105%
        CHANNEL_FACTORS.put("distributor", BigDecimal.valueOf(0.85));      // 经销商：85%
        CHANNEL_FACTORS.put("wholesaler", BigDecimal.valueOf(0.80));       // 批发商：80%
        CHANNEL_FACTORS.put("agent", BigDecimal.valueOf(0.90));            // 代理商：90%
        CHANNEL_FACTORS.put("export", BigDecimal.valueOf(1.10));           // 出口：110%
    }
    
    // 季节系数定义
    private static final Map<String, BigDecimal> SEASON_FACTORS = new HashMap<>();
    static {
        SEASON_FACTORS.put("spring", BigDecimal.valueOf(1.00));     // 春季：100%
        SEASON_FACTORS.put("summer", BigDecimal.valueOf(1.05));     // 夏季：105%
        SEASON_FACTORS.put("autumn", BigDecimal.valueOf(1.00));     // 秋季：100%
        SEASON_FACTORS.put("winter", BigDecimal.valueOf(0.95));     // 冬季：95%
        SEASON_FACTORS.put("peak_season", BigDecimal.valueOf(1.15)); // 旺季：115%
        SEASON_FACTORS.put("off_season", BigDecimal.valueOf(0.85));  // 淡季：85%
    }
    
    @Override
    public BigDecimal calculateCostPlusPrice(BigDecimal cost, BigDecimal markupRate) {
        if (cost == null || markupRate == null) {
            throw new IllegalArgumentException("成本和加成率不能为空");
        }
        
        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成本必须大于0");
        }
        
        // 价格 = 成本 × (1 + 加成率)
        BigDecimal price = cost.multiply(BigDecimal.ONE.add(markupRate));
        
        log.debug("成本加成定价: cost={}, markupRate={}, price={}", 
                 cost, markupRate, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateTargetProfitPrice(BigDecimal cost, BigDecimal targetProfitMargin) {
        if (cost == null || targetProfitMargin == null) {
            throw new IllegalArgumentException("成本和目标利润率不能为空");
        }
        
        if (cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成本必须大于0");
        }
        
        if (targetProfitMargin.compareTo(BigDecimal.ONE) >= 0 || 
            targetProfitMargin.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("目标利润率必须在0到1之间");
        }
        
        // 价格 = 成本 / (1 - 目标利润率)
        try {
            BigDecimal denominator = BigDecimal.ONE.subtract(targetProfitMargin);
            BigDecimal price = cost.divide(denominator, 10, RoundingMode.HALF_UP);
            
            log.debug("目标利润定价: cost={}, profitMargin={}, price={}", 
                     cost, targetProfitMargin, price);
            
            return price.setScale(2, RoundingMode.HALF_UP);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("目标利润率不能为1或大于1");
        }
    }
    
    @Override
    public BigDecimal calculateMarketReferencePrice(BigDecimal marketPrice, BigDecimal adjustmentFactor) {
        if (marketPrice == null || adjustmentFactor == null) {
            throw new IllegalArgumentException("市场价和调整系数不能为空");
        }
        
        if (marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("市场价必须大于0");
        }
        
        // 价格 = 市场参考价 × 调整系数
        BigDecimal price = marketPrice.multiply(adjustmentFactor);
        
        log.debug("市场参考定价: marketPrice={}, adjustmentFactor={}, price={}", 
                 marketPrice, adjustmentFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateCompetitionBasedPrice(BigDecimal competitorPrice, BigDecimal competitionFactor) {
        if (competitorPrice == null || competitionFactor == null) {
            throw new IllegalArgumentException("竞争对手价格和竞争系数不能为空");
        }
        
        if (competitorPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("竞争对手价格必须大于0");
        }
        
        // 价格 = 竞争对手价格 × 竞争系数
        BigDecimal price = competitorPrice.multiply(competitionFactor);
        
        log.debug("竞争导向定价: competitorPrice={}, competitionFactor={}, price={}", 
                 competitorPrice, competitionFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateValueBasedPrice(BigDecimal baseValue, BigDecimal valueMultiplier,
                                              BigDecimal brandFactor, BigDecimal qualityFactor) {
        if (baseValue == null || valueMultiplier == null) {
            throw new IllegalArgumentException("基础价值和价值乘数不能为空");
        }
        
        if (baseValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基础价值必须大于0");
        }
        
        // 基础价值计算
        BigDecimal valuePrice = baseValue.multiply(valueMultiplier);
        
        // 应用品牌系数（如果有）
        if (brandFactor != null) {
            valuePrice = valuePrice.multiply(brandFactor);
        }
        
        // 应用质量系数（如果有）
        if (qualityFactor != null) {
            valuePrice = valuePrice.multiply(qualityFactor);
        }
        
        log.debug("价值定价: baseValue={}, valueMultiplier={}, brandFactor={}, qualityFactor={}, price={}", 
                 baseValue, valueMultiplier, brandFactor, qualityFactor, valuePrice);
        
        return valuePrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateChannelBasedPrice(BigDecimal basePrice, String channelType) {
        if (basePrice == null || channelType == null) {
            throw new IllegalArgumentException("基础价格和渠道类型不能为空");
        }
        
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基础价格必须大于0");
        }
        
        // 获取渠道系数，默认为1.0
        BigDecimal channelFactor = CHANNEL_FACTORS.getOrDefault(channelType, BigDecimal.ONE);
        
        // 价格 = 基础价格 × 渠道系数
        BigDecimal price = basePrice.multiply(channelFactor);
        
        log.debug("分渠道定价: basePrice={}, channelType={}, channelFactor={}, price={}", 
                 basePrice, channelType, channelFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateSeasonalPrice(BigDecimal basePrice, BigDecimal seasonFactor, BigDecimal holidayFactor) {
        if (basePrice == null) {
            throw new IllegalArgumentException("基础价格不能为空");
        }
        
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基础价格必须大于0");
        }
        
        BigDecimal price = basePrice;
        
        // 应用季节系数（如果有）
        if (seasonFactor != null) {
            price = price.multiply(seasonFactor);
        }
        
        // 应用节假日系数（如果有）
        if (holidayFactor != null) {
            price = price.multiply(holidayFactor);
        }
        
        log.debug("季节性定价: basePrice={}, seasonFactor={}, holidayFactor={}, price={}", 
                 basePrice, seasonFactor, holidayFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateComprehensivePrice(BigDecimal cost, BigDecimal marketPrice, BigDecimal competitorPrice,
                                                 BigDecimal costWeight, BigDecimal marketWeight, BigDecimal competitionWeight) {
        // 参数校验
        validateComprehensiveParameters(cost, marketPrice, competitorPrice, costWeight, marketWeight, competitionWeight);
        
        // 使用成本加成法（20%加成率）计算成本部分
        BigDecimal costBasedPrice = calculateCostPlusPrice(cost, BigDecimal.valueOf(0.20));
        
        // 使用市场参考法（系数1.0）计算市场部分
        BigDecimal marketBasedPrice = calculateMarketReferencePrice(marketPrice, BigDecimal.ONE);
        
        // 使用竞争导向法（系数0.95）计算竞争部分
        BigDecimal competitionBasedPrice = calculateCompetitionBasedPrice(competitorPrice, BigDecimal.valueOf(0.95));
        
        // 加权平均计算最终价格
        BigDecimal weightedPrice = costBasedPrice.multiply(costWeight)
                .add(marketBasedPrice.multiply(marketWeight))
                .add(competitionBasedPrice.multiply(competitionWeight));
        
        log.debug("综合定价: costBased={}, marketBased={}, competitionBased={}, weights=({},{},{}), price={}", 
                 costBasedPrice, marketBasedPrice, competitionBasedPrice, 
                 costWeight, marketWeight, competitionWeight, weightedPrice);
        
        return weightedPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public PriceValidationResult validatePrice(BigDecimal calculatedPrice, BigDecimal minAcceptablePrice, BigDecimal maxAcceptablePrice) {
        if (calculatedPrice == null) {
            return new PriceValidationResult(false, "计算价格不能为空", null);
        }
        
        // 检查价格是否为负数
        if (calculatedPrice.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal adjustedPrice = BigDecimal.ZERO.max(calculatedPrice);
            return new PriceValidationResult(false, "价格不能为负数，已调整为" + adjustedPrice, adjustedPrice);
        }
        
        // 检查是否低于最低可接受价格
        if (minAcceptablePrice != null && calculatedPrice.compareTo(minAcceptablePrice) < 0) {
            return new PriceValidationResult(false, 
                    "价格低于最低可接受价格: " + calculatedPrice + " < " + minAcceptablePrice,
                    minAcceptablePrice);
        }
        
        // 检查是否高于最高可接受价格
        if (maxAcceptablePrice != null && calculatedPrice.compareTo(maxAcceptablePrice) > 0) {
            return new PriceValidationResult(false, 
                    "价格高于最高可接受价格: " + calculatedPrice + " > " + maxAcceptablePrice,
                    maxAcceptablePrice);
        }
        
        // 价格在合理范围内
        return new PriceValidationResult(true, "价格验证通过: " + calculatedPrice, calculatedPrice);
    }
    
    /**
     * 验证综合定价参数
     */
    private void validateComprehensiveParameters(BigDecimal cost, BigDecimal marketPrice, BigDecimal competitorPrice,
                                                BigDecimal costWeight, BigDecimal marketWeight, BigDecimal competitionWeight) {
        // 检查权重和是否为1.0
        BigDecimal totalWeight = costWeight.add(marketWeight).add(competitionWeight);
        if (totalWeight.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("权重总和必须等于1.0，当前为: " + totalWeight);
        }
        
        // 检查权重是否在0-1之间
        if (costWeight.compareTo(BigDecimal.ZERO) < 0 || costWeight.compareTo(BigDecimal.ONE) > 0 ||
            marketWeight.compareTo(BigDecimal.ZERO) < 0 || marketWeight.compareTo(BigDecimal.ONE) > 0 ||
            competitionWeight.compareTo(BigDecimal.ZERO) < 0 || competitionWeight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("权重必须在0到1之间");
        }
        
        // 检查价格参数
        if (cost != null && cost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成本必须大于0");
        }
        if (marketPrice != null && marketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("市场参考价必须大于0");
        }
        if (competitorPrice != null && competitorPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("竞争对手价格必须大于0");
        }
    }
    
    /**
     * 获取指定季节的季节系数
     */
    public BigDecimal getSeasonFactor(String season) {
        return SEASON_FACTORS.getOrDefault(season, BigDecimal.ONE);
    }
    
    /**
     * 获取指定渠道的渠道系数
     */
    public BigDecimal getChannelFactor(String channelType) {
        return CHANNEL_FACTORS.getOrDefault(channelType, BigDecimal.ONE);
    }
}