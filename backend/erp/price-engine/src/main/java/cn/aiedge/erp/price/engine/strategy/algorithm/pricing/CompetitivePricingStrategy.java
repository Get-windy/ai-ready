package cn.aiedge.erp.price.engine.strategy.algorithm.pricing;

import cn.aiedge.erp.price.engine.strategy.PricingAlgorithm;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞争性定价算法
 * 基于竞争对手价格和市场位置定价，适用于竞争激烈的市场
 */
@Component
public class CompetitivePricingStrategy implements PricingAlgorithm {
    
    private static final String DEFAULT_PARAMETERS = "{\"competitionIntensity\":0.7,\"minPriceRatio\":0.8,\"maxPriceRatio\":1.2,\"marketPosition\":\"FOLLOWER\",\"aggressiveness\":0.5}";
    
    @Override
    public PricingStrategy.StrategyType getAlgorithmType() {
        return PricingStrategy.StrategyType.COMPETITIVE;
    }
    
    @Override
    public boolean isApplicable(PriceCalculationRequest request) {
        if (request == null) {
            return false;
        }
        
        // 竞争性定价适用于有竞争对手数据或高度竞争的市场
        boolean hasCompetitorData = request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty();
        boolean isCompetitiveMarket = request.getMarketFactors() != null &&
                                     request.getMarketFactors().containsKey("competitionIntensity") &&
                                     "HIGH".equalsIgnoreCase(request.getMarketFactors().get("competitionIntensity"));
        
        boolean isPriceSensitiveProduct = request.getProductType() != null &&
                                         (request.getProductType().toUpperCase().contains("COMMODITY") ||
                                          request.getProductType().toUpperCase().contains("PRICE_SENSITIVE"));
        
        return hasCompetitorData || isCompetitiveMarket || isPriceSensitiveProduct;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy) {
        if (request == null || strategy == null) {
            throw new IllegalArgumentException("计算请求和定价策略不能为空");
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        
        // 验证策略类型
        if (strategy.getStrategyType() != PricingStrategy.StrategyType.COMPETITIVE) {
            throw new IllegalArgumentException("策略类型不匹配，期望: COMPETITIVE, 实际: " + strategy.getStrategyType());
        }
        
        // 计算竞争基准价格
        BigDecimal competitionBasePrice = calculateCompetitionBasePrice(request);
        
        // 计算价格调整系数
        BigDecimal priceAdjustmentRatio = calculatePriceAdjustmentRatio(request, strategy.getParameters());
        
        // 计算最终价格
        BigDecimal finalPrice = competitionBasePrice.multiply(priceAdjustmentRatio)
                                                   .setScale(2, RoundingMode.HALF_UP);
        
        // 计算毛利润（如果有成本数据）
        BigDecimal cost = request.getProductCost();
        BigDecimal grossProfit = null;
        BigDecimal grossMargin = null;
        
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            grossProfit = finalPrice.subtract(cost);
            grossMargin = grossProfit.divide(finalPrice, 4, RoundingMode.HALF_UP);
        }
        
        // 计算与竞争对手的价格对比
        String competitionAnalysis = analyzeCompetitionPosition(request, finalPrice);
        
        // 创建计算结果
        PriceCalculationResult result = new PriceCalculationResult();
        result.setBasePrice(competitionBasePrice);
        result.setFinalPrice(finalPrice);
        result.setGrossProfit(grossProfit);
        result.setGrossMargin(grossMargin);
        result.setCalculationStartTime(startTime);
        result.setCalculationEndTime(LocalDateTime.now());
        result.setStrategyApplied("竞争性定价算法");
        result.setStrategyType(PricingStrategy.StrategyType.COMPETITIVE.name());
        result.setCalculationNotes(String.format(
            "竞争基准价: %s, 调整系数: %.2f, 最终价格: %s\n%s",
            competitionBasePrice.toPlainString(),
            priceAdjustmentRatio.doubleValue(),
            finalPrice.toPlainString(),
            competitionAnalysis
        ));
        result.setSuccess(true);
        result.setErrorCode(0);
        result.setErrorMessage("");
        
        return result;
    }
    
    @Override
    public int calculatePriorityScore(PriceCalculationRequest request) {
        if (!isApplicable(request)) {
            return 0;
        }
        
        int score = 45; // 基础分数
        
        // 根据竞争对手数据调整分数
        if (request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            score += 35;
            // 竞争对手越多，竞争性定价越适用
            int competitorCount = request.getCompetitorPrices().size();
            if (competitorCount >= 5) {
                score += 20;
            } else if (competitorCount >= 3) {
                score += 15;
            } else {
                score += 10;
            }
        }
        
        // 根据市场竞争强度调整分数
        if (request.getMarketFactors() != null && 
            request.getMarketFactors().containsKey("competitionIntensity")) {
            String intensity = request.getMarketFactors().get("competitionIntensity");
            if ("VERY_HIGH".equalsIgnoreCase(intensity)) {
                score += 30;
            } else if ("HIGH".equalsIgnoreCase(intensity)) {
                score += 25;
            } else if ("MEDIUM".equalsIgnoreCase(intensity)) {
                score += 15;
            } else if ("LOW".equalsIgnoreCase(intensity)) {
                score += 5;
            }
        }
        
        // 根据产品类型调整分数
        if (request.getProductType() != null) {
            String productType = request.getProductType().toUpperCase();
            if (productType.contains("COMMODITY") || productType.contains("GENERIC")) {
                score += 25;
            } else if (productType.contains("PRICE_SENSITIVE")) {
                score += 20;
            } else if (productType.contains("STANDARD")) {
                score += 15;
            }
        }
        
        // 根据客户等级调整分数
        if (request.getCustomerGrade() != null) {
            switch (request.getCustomerGrade().toUpperCase()) {
                case "BASIC":
                case "STANDARD":
                    score += 15; // 标准客户更关注价格
                    break;
                case "SILVER":
                    score += 10;
                    break;
                case "GOLD":
                case "VIP":
                    score += 5; // 高端客户对价格敏感度较低
                    break;
            }
        }
        
        return score;
    }
    
    @Override
    public String getDescription() {
        return "竞争性定价算法：基于竞争对手价格和市场位置定价。适用于竞争激烈的市场，考虑竞争对手价格、市场竞争强度、产品价格敏感度等因素。";
    }
    
    @Override
    public String getDefaultParameters() {
        return DEFAULT_PARAMETERS;
    }
    
    /**
     * 计算竞争基准价格
     */
    private BigDecimal calculateCompetitionBasePrice(PriceCalculationRequest request) {
        BigDecimal competitionBasePrice = null;
        
        // 1. 首先使用竞争对手价格中位数
        if (request.getCompetitorPrices() != null && !request.getCompetitorPrices().isEmpty()) {
            competitionBasePrice = calculateMedianPrice(request.getCompetitorPrices());
        }
        
        // 2. 如果没有竞争对手数据，使用市场平均价
        if (competitionBasePrice == null && request.getMarketAveragePrice() != null) {
            competitionBasePrice = request.getMarketAveragePrice();
        }
        
        // 3. 如果还没有，基于成本计算（低利润率）
        if (competitionBasePrice == null && request.getProductCost() != null) {
            // 竞争性产品通常利润率较低
            competitionBasePrice = request.getProductCost().multiply(new BigDecimal("1.15"))
                                         .setScale(2, RoundingMode.HALF_UP);
        }
        
        if (competitionBasePrice == null) {
            throw new IllegalArgumentException("无法计算竞争基准价格，缺少必要的竞争数据");
        }
        
        return competitionBasePrice;
    }
    
    /**
     * 计算价格调整系数
     */
    private BigDecimal calculatePriceAdjustmentRatio(PriceCalculationRequest request, String parameters) {
        BigDecimal baseRatio = BigDecimal.ONE;
        
        // 1. 市场竞争强度影响
        BigDecimal competitionIntensity = extractCompetitionIntensity(request, parameters);
        baseRatio = baseRatio.multiply(competitionIntensity);
        
        // 2. 市场位置策略影响
        BigDecimal marketPositionFactor = calculateMarketPositionFactor(parameters);
        baseRatio = baseRatio.multiply(marketPositionFactor);
        
        // 3. 产品价格敏感度影响
        BigDecimal priceSensitivityFactor = calculatePriceSensitivityFactor(request);
        baseRatio = baseRatio.multiply(priceSensitivityFactor);
        
        // 4. 客户价格敏感度影响
        BigDecimal customerSensitivityFactor = calculateCustomerSensitivityFactor(request);
        baseRatio = baseRatio.multiply(customerSensitivityFactor);
        
        // 确保系数在有效范围内
        BigDecimal minRatio = extractParameter(parameters, "minPriceRatio", new BigDecimal("0.8"));
        BigDecimal maxRatio = extractParameter(parameters, "maxPriceRatio", new BigDecimal("1.2"));
        
        if (baseRatio.compareTo(minRatio) < 0) {
            baseRatio = minRatio;
        } else if (baseRatio.compareTo(maxRatio) > 0) {
            baseRatio = maxRatio;
        }
        
        return baseRatio.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * 提取市场竞争强度系数
     */
    private BigDecimal extractCompetitionIntensity(PriceCalculationRequest request, String parameters) {
        // 首先从市场因素获取
        if (request.getMarketFactors() != null && 
            request.getMarketFactors().containsKey("competitionIntensity")) {
            String intensity = request.getMarketFactors().get("competitionIntensity");
            switch (intensity.toUpperCase()) {
                case "VERY_HIGH":
                    return new BigDecimal("0.85"); // 高度竞争，需要更低价格
                case "HIGH":
                    return new BigDecimal("0.90");
                case "MEDIUM":
                    return new BigDecimal("0.95");
                case "LOW":
                    return new BigDecimal("1.05"); // 低竞争，可以更高价格
                case "VERY_LOW":
                    return new BigDecimal("1.10");
            }
        }
        
        // 从参数中获取
        return extractParameter(parameters, "competitionIntensity", new BigDecimal("0.95"));
    }
    
    /**
     * 计算市场位置策略因子
     */
    private BigDecimal calculateMarketPositionFactor(String parameters) {
        String marketPosition = extractMarketPosition(parameters);
        BigDecimal aggressiveness = extractParameter(parameters, "aggressiveness", new BigDecimal("0.5"));
        
        switch (marketPosition.toUpperCase()) {
            case "LEADER":
                // 市场领导者可以设定更高价格
                return BigDecimal.ONE.add(new BigDecimal("0.1").multiply(aggressiveness));
            case "CHALLENGER":
                // 挑战者需要更具竞争力的价格
                return BigDecimal.ONE.subtract(new BigDecimal("0.05").multiply(aggressiveness));
            case "FOLLOWER":
                // 跟随者通常匹配市场价格
                return BigDecimal.ONE;
            case "NICHE":
                // 利基市场可以设定更高价格
                return BigDecimal.ONE.add(new BigDecimal("0.15").multiply(aggressiveness));
            default:
                return BigDecimal.ONE;
        }
    }
    
    /**
     * 计算产品价格敏感度因子
     */
    private BigDecimal calculatePriceSensitivityFactor(PriceCalculationRequest request) {
        if (request.getProductType() == null) {
            return BigDecimal.ONE;
        }
        
        String productType = request.getProductType().toUpperCase();
        if (productType.contains("COMMODITY") || productType.contains("GENERIC")) {
            return new BigDecimal("0.90"); // 大宗商品价格敏感度高
        } else if (productType.contains("PRICE_SENSITIVE")) {
            return new BigDecimal("0.92");
        } else if (productType.contains("DIFFERENTIATED") || productType.contains("PREMIUM")) {
            return new BigDecimal("1.05"); // 差异化产品价格敏感度低
        } else {
            return BigDecimal.ONE;
        }
    }
    
    /**
     * 计算客户价格敏感度因子
     */
    private BigDecimal calculateCustomerSensitivityFactor(PriceCalculationRequest request) {
        if (request.getCustomerGrade() == null) {
            return BigDecimal.ONE;
        }
        
        switch (request.getCustomerGrade().toUpperCase()) {
            case "BASIC":
            case "STANDARD":
                return new BigDecimal("0.95"); // 标准客户价格敏感度高
            case "SILVER":
                return new BigDecimal("0.97");
            case "GOLD":
                return new BigDecimal("1.02");
            case "VIP":
            case "PREMIUM":
                return new BigDecimal("1.05"); // VIP客户价格敏感度低
            default:
                return BigDecimal.ONE;
        }
    }
    
    /**
     * 分析竞争位置
     */
    private String analyzeCompetitionPosition(PriceCalculationRequest request, BigDecimal ourPrice) {
        if (request.getCompetitorPrices() == null || request.getCompetitorPrices().isEmpty()) {
            return "无竞争对手数据可用于分析";
        }
        
        BigDecimal minCompetitorPrice = request.getCompetitorPrices().stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxCompetitorPrice = request.getCompetitorPrices().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal avgCompetitorPrice = request.getCompetitorPrices().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(request.getCompetitorPrices().size()), 2, RoundingMode.HALF_UP);
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("竞争对手分析:\n");
        analysis.append(String.format("  最低价: %s\n", minCompetitorPrice.toPlainString()));
        analysis.append(String.format("  最高价: %s\n", maxCompetitorPrice.toPlainString()));
        analysis.append(String.format("  平均价: %s\n", avgCompetitorPrice.toPlainString()));
        analysis.append(String.format("  我方价格: %s\n", ourPrice.toPlainString()));
        
        if (ourPrice.compareTo(minCompetitorPrice) < 0) {
            analysis.append("  位置: 价格领导者（最低价）");
        } else if (ourPrice.compareTo(avgCompetitorPrice) < 0) {
            analysis.append("  位置: 价格挑战者（低于平均价）");
        } else if (ourPrice.compareTo(avgCompetitorPrice) == 0) {
            analysis.append("  位置: 市场跟随者（匹配平均价）");
        } else if (ourPrice.compareTo(maxCompetitorPrice) > 0) {
            analysis.append("  位置: 高端定位（最高价）");
        } else {
            analysis.append("  位置: 市场中游（在平均价和最高价之间）");
        }
        
        return analysis.toString();
    }
    
    /**
     * 计算价格中位数
     */
    private BigDecimal calculateMedianPrice(List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return null;
        }
        
        List<BigDecimal> sortedPrices = new java.util.ArrayList<>(prices);
        sortedPrices.sort(BigDecimal::compareTo);
        
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            // 偶数个，取中间两个的平均值
            BigDecimal middle1 = sortedPrices.get(size / 2 - 1);
            BigDecimal middle2 = sortedPrices.get(size / 2);
            return middle1.add(middle2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else {
            // 奇数个，取中间值
            return sortedPrices.get(size / 2);
        }
    }
    
    /**
     * 提取市场位置
     */
    private String extractMarketPosition(String parameters) {
        if (parameters == null || !parameters.contains("\"marketPosition\":")) {
            return "FOLLOWER";
        }
        
        try {
            String[] parts = parameters.split("\"marketPosition\":");
            if (parts.length > 1) {
                String valuePart = parts[1].split(",")[0].trim();
                if (valuePart.endsWith("}") || valuePart.endsWith("\"")) {
                    valuePart = valuePart.replaceAll("[\"}]", "");
                }
                return valuePart;
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return "FOLLOWER";
    }
    
    /**
     * 从参数中提取指定字段的值
     */
    private BigDecimal extractParameter(String parameters, String fieldName, BigDecimal defaultValue) {
        if (parameters == null || !parameters.contains("\"" + fieldName + "\":")) {
            return defaultValue;
        }
        
        try {
            String[] parts = parameters.split("\"" + fieldName + "\":");
            if (parts.length > 1) {
                String valuePart = parts[1].split(",")[0].trim();
                if (valuePart.endsWith("}") || valuePart.endsWith("]")) {
                    valuePart = valuePart.substring(0, valuePart.length() - 1);
                }
                return new BigDecimal(valuePart);
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return defaultValue;
    }
}