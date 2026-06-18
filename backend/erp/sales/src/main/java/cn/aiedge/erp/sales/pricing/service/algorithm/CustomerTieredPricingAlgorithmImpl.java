package cn.aiedge.erp.sales.pricing.service.algorithm;

import cn.aiedge.erp.sales.pricing.enums.CustomerLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户分级定价算法实现类
 */
@Slf4j
@Service
public class CustomerTieredPricingAlgorithmImpl implements ICustomerTieredPricingAlgorithm {
    
    // 客户等级定价配置
    private final Map<CustomerLevel, CustomerLevelPricingConfig> levelConfigs;
    
    // 采购金额阈值配置（单位：万元）
    private static final BigDecimal THRESHOLD_100M = BigDecimal.valueOf(100);  // 100万
    private static final BigDecimal THRESHOLD_50M = BigDecimal.valueOf(50);    // 50万
    private static final BigDecimal THRESHOLD_20M = BigDecimal.valueOf(20);    // 20万
    private static final BigDecimal THRESHOLD_5M = BigDecimal.valueOf(5);      // 5万
    
    // 采购次数阈值配置
    private static final int COUNT_THRESHOLD_HIGH = 50;    // 高频率采购
    private static final int COUNT_THRESHOLD_MEDIUM = 20;  // 中等频率采购
    private static final int COUNT_THRESHOLD_LOW = 5;      // 低频率采购
    
    public CustomerTieredPricingAlgorithmImpl() {
        this.levelConfigs = initializeCustomerLevelConfigs();
    }
    
    @Override
    public BigDecimal calculatePriceByCustomerLevel(BigDecimal basePrice, CustomerLevel customerLevel) {
        validateBasePrice(basePrice);
        
        // 获取客户等级配置
        CustomerLevelPricingConfig config = levelConfigs.get(customerLevel);
        if (config == null) {
            config = levelConfigs.get(CustomerLevel.BRONZE); // 默认使用青铜等级配置
        }
        
        // 价格 = 基础价格 × (1 - 折扣率)
        BigDecimal discountRate = config.getDiscountRate();
        BigDecimal price = basePrice.multiply(BigDecimal.ONE.subtract(discountRate));
        
        log.info("客户等级定价: basePrice={}, level={}, discountRate={}, price={}", 
                 basePrice, customerLevel, discountRate, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateDiscountByCustomerLevel(CustomerLevel customerLevel) {
        CustomerLevelPricingConfig config = levelConfigs.get(customerLevel);
        if (config == null) {
            config = levelConfigs.get(CustomerLevel.BRONZE);
        }
        return config.getDiscountRate();
    }
    
    @Override
    public BigDecimal calculatePriceByPurchaseHistory(BigDecimal basePrice, 
                                                     BigDecimal historicalPurchaseAmount, 
                                                     int purchaseCount) {
        validateBasePrice(basePrice);
        
        if (historicalPurchaseAmount == null) {
            historicalPurchaseAmount = BigDecimal.ZERO;
        }
        
        // 计算采购金额折扣
        BigDecimal amountDiscountFactor = calculateAmountDiscountFactor(historicalPurchaseAmount);
        
        // 计算采购次数折扣
        BigDecimal countDiscountFactor = calculateCountDiscountFactor(purchaseCount);
        
        // 综合折扣率 = 金额折扣因子 × 次数折扣因子
        BigDecimal combinedDiscountFactor = amountDiscountFactor.multiply(countDiscountFactor);
        
        // 价格 = 基础价格 × 综合折扣因子
        BigDecimal price = basePrice.multiply(combinedDiscountFactor);
        
        log.debug("历史采购定价: basePrice={}, amount={}, count={}, amountFactor={}, countFactor={}, price={}", 
                 basePrice, historicalPurchaseAmount, purchaseCount, 
                 amountDiscountFactor, countDiscountFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculatePriceByCustomerLoyalty(BigDecimal basePrice, 
                                                     int loyaltyScore, 
                                                     int customerAgeMonths) {
        validateBasePrice(basePrice);
        
        // 计算忠诚度折扣因子
        BigDecimal loyaltyDiscountFactor = calculateLoyaltyDiscountFactor(loyaltyScore);
        
        // 计算客户时长折扣因子
        BigDecimal ageDiscountFactor = calculateAgeDiscountFactor(customerAgeMonths);
        
        // 综合折扣因子 = 忠诚度折扣因子 × 时长折扣因子
        BigDecimal combinedDiscountFactor = loyaltyDiscountFactor.multiply(ageDiscountFactor);
        
        // 价格 = 基础价格 × 综合折扣因子
        BigDecimal price = basePrice.multiply(combinedDiscountFactor);
        
        log.debug("客户忠诚度定价: basePrice={}, loyaltyScore={}, ageMonths={}, loyaltyFactor={}, ageFactor={}, price={}", 
                 basePrice, loyaltyScore, customerAgeMonths, 
                 loyaltyDiscountFactor, ageDiscountFactor, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculatePriceForLevelUpgrade(BigDecimal oldPrice, 
                                                   CustomerLevel oldLevel, 
                                                   CustomerLevel newLevel) {
        validateBasePrice(oldPrice);
        
        // 计算原等级折扣率
        BigDecimal oldDiscountRate = calculateDiscountByCustomerLevel(oldLevel);
        // 计算新等级折扣率
        BigDecimal newDiscountRate = calculateDiscountByCustomerLevel(newLevel);
        
        // 恢复原价 = 原价格 / (1 - 原折扣率)
        BigDecimal originalPrice = oldPrice.divide(
            BigDecimal.ONE.subtract(oldDiscountRate), 10, RoundingMode.HALF_UP);
        
        // 新价格 = 原价 × (1 - 新折扣率)
        BigDecimal newPrice = originalPrice.multiply(BigDecimal.ONE.subtract(newDiscountRate));
        
        log.info("客户等级升级定价: oldPrice={}, oldLevel={}, newLevel={}, originalPrice={}, newPrice={}", 
                 oldPrice, oldLevel, newLevel, originalPrice, newPrice);
        
        return newPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateComprehensiveCustomerPrice(BigDecimal basePrice, 
                                                         CustomerLevel customerLevel,
                                                         BigDecimal historicalPurchaseAmount, 
                                                         int purchaseCount,
                                                         int loyaltyScore, 
                                                         int customerAgeMonths) {
        validateBasePrice(basePrice);
        
        // 计算各维度折扣因子
        BigDecimal levelDiscountFactor = BigDecimal.ONE.subtract(calculateDiscountByCustomerLevel(customerLevel));
        BigDecimal amountDiscountFactor = calculateAmountDiscountFactor(
            historicalPurchaseAmount != null ? historicalPurchaseAmount : BigDecimal.ZERO);
        BigDecimal countDiscountFactor = calculateCountDiscountFactor(purchaseCount);
        BigDecimal loyaltyDiscountFactor = calculateLoyaltyDiscountFactor(loyaltyScore);
        BigDecimal ageDiscountFactor = calculateAgeDiscountFactor(customerAgeMonths);
        
        // 综合折扣因子 = 各维度折扣因子的加权平均
        // 权重分配：客户等级40%，采购金额20%，采购次数15%，忠诚度15%，客户时长10%
        BigDecimal combinedDiscountFactor = 
            levelDiscountFactor.multiply(BigDecimal.valueOf(0.40))
                .add(amountDiscountFactor.multiply(BigDecimal.valueOf(0.20)))
                .add(countDiscountFactor.multiply(BigDecimal.valueOf(0.15)))
                .add(loyaltyDiscountFactor.multiply(BigDecimal.valueOf(0.15)))
                .add(ageDiscountFactor.multiply(BigDecimal.valueOf(0.10)));
        
        // 价格 = 基础价格 × 综合折扣因子
        BigDecimal price = basePrice.multiply(combinedDiscountFactor);
        
        log.info("综合客户定价: basePrice={}, level={}, amount={}, count={}, loyalty={}, age={}, price={}", 
                 basePrice, customerLevel, historicalPurchaseAmount, purchaseCount, 
                 loyaltyScore, customerAgeMonths, price);
        
        return price.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public CustomerPriceValidationResult validateCustomerPrice(BigDecimal price, 
                                                              CustomerLevel customerLevel, 
                                                              BigDecimal basePrice) {
        if (price == null || basePrice == null) {
            return new CustomerPriceValidationResult(false, 
                "价格和基础价格不能为空", null, "参数验证");
        }
        
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal adjustedPrice = BigDecimal.ZERO.max(price);
            return new CustomerPriceValidationResult(false, 
                "价格不能为负数，已调整为" + adjustedPrice, 
                adjustedPrice, "价格下限验证");
        }
        
        // 获取客户等级配置
        CustomerLevelPricingConfig config = levelConfigs.get(customerLevel);
        if (config == null) {
            config = levelConfigs.get(CustomerLevel.BRONZE);
        }
        
        // 计算价格因子 = 价格 / 基础价格
        BigDecimal priceFactor = price.divide(basePrice, 4, RoundingMode.HALF_UP);
        
        // 检查是否在允许的价格因子范围内
        if (priceFactor.compareTo(config.getMinPriceFactor()) < 0) {
            BigDecimal adjustedPrice = basePrice.multiply(config.getMinPriceFactor());
            return new CustomerPriceValidationResult(false,
                String.format("价格因子%s低于最低允许值%s，已调整为%s", 
                    priceFactor, config.getMinPriceFactor(), adjustedPrice),
                adjustedPrice, "客户等级价格下限");
        }
        
        if (priceFactor.compareTo(config.getMaxPriceFactor()) > 0) {
            BigDecimal adjustedPrice = basePrice.multiply(config.getMaxPriceFactor());
            return new CustomerPriceValidationResult(false,
                String.format("价格因子%s高于最高允许值%s，已调整为%s", 
                    priceFactor, config.getMaxPriceFactor(), adjustedPrice),
                adjustedPrice, "客户等级价格上限");
        }
        
        // 验证通过
        return new CustomerPriceValidationResult(true,
            String.format("客户等级价格验证通过: %s (因子: %s)", price, priceFactor),
            price, "客户等级价格验证");
    }
    
    @Override
    public Map<CustomerLevel, CustomerLevelPricingConfig> getCustomerLevelPricingConfigs() {
        return new HashMap<>(levelConfigs);
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 初始化客户等级定价配置
     */
    private Map<CustomerLevel, CustomerLevelPricingConfig> initializeCustomerLevelConfigs() {
        Map<CustomerLevel, CustomerLevelPricingConfig> configs = new HashMap<>();
        
        configs.put(CustomerLevel.DIAMOND, new CustomerLevelPricingConfig(
            BigDecimal.valueOf(0.20),  // 20% 折扣
            BigDecimal.valueOf(0.75),  // 最低价格因子 0.75
            BigDecimal.valueOf(0.95),  // 最高价格因子 0.95
            "钻石客户：年采购额≥100万或战略合作客户，享受最大优惠"
        ));
        
        configs.put(CustomerLevel.PLATINUM, new CustomerLevelPricingConfig(
            BigDecimal.valueOf(0.15),  // 15% 折扣
            BigDecimal.valueOf(0.80),  // 最低价格因子 0.80
            BigDecimal.valueOf(0.97),  // 最高价格因子 0.97
            "白金客户：年采购额50-100万或长期稳定客户"
        ));
        
        configs.put(CustomerLevel.GOLD, new CustomerLevelPricingConfig(
            BigDecimal.valueOf(0.10),  // 10% 折扣
            BigDecimal.valueOf(0.85),  // 最低价格因子 0.85
            BigDecimal.valueOf(1.00),  // 最高价格因子 1.00
            "黄金客户：年采购额20-50万或有良好付款记录的客户"
        ));
        
        configs.put(CustomerLevel.SILVER, new CustomerLevelPricingConfig(
            BigDecimal.valueOf(0.05),  // 5% 折扣
            BigDecimal.valueOf(0.90),  // 最低价格因子 0.90
            BigDecimal.valueOf(1.05),  // 最高价格因子 1.05
            "白银客户：年采购额5-20万的新客户或一般客户"
        ));
        
        configs.put(CustomerLevel.BRONZE, new CustomerLevelPricingConfig(
            BigDecimal.valueOf(0.00),  // 0% 折扣
            BigDecimal.valueOf(0.95),  // 最低价格因子 0.95
            BigDecimal.valueOf(1.10),  // 最高价格因子 1.10
            "青铜客户：新注册客户或年采购额<5万的客户"
        ));
        
        return configs;
    }
    
    /**
     * 根据采购金额计算折扣因子
     */
    private BigDecimal calculateAmountDiscountFactor(BigDecimal amount) {
        if (amount.compareTo(THRESHOLD_100M) >= 0) {
            return BigDecimal.valueOf(0.80);  // 100万以上：8折
        } else if (amount.compareTo(THRESHOLD_50M) >= 0) {
            return BigDecimal.valueOf(0.85);  // 50-100万：85折
        } else if (amount.compareTo(THRESHOLD_20M) >= 0) {
            return BigDecimal.valueOf(0.90);  // 20-50万：9折
        } else if (amount.compareTo(THRESHOLD_5M) >= 0) {
            return BigDecimal.valueOf(0.95);  // 5-20万：95折
        } else {
            return BigDecimal.valueOf(1.00);  // 5万以下：无折扣
        }
    }
    
    /**
     * 根据采购次数计算折扣因子
     */
    private BigDecimal calculateCountDiscountFactor(int count) {
        if (count >= COUNT_THRESHOLD_HIGH) {
            return BigDecimal.valueOf(0.85);  // 50次以上：85折
        } else if (count >= COUNT_THRESHOLD_MEDIUM) {
            return BigDecimal.valueOf(0.90);  // 20-50次：9折
        } else if (count >= COUNT_THRESHOLD_LOW) {
            return BigDecimal.valueOf(0.95);  // 5-20次：95折
        } else {
            return BigDecimal.valueOf(1.00);  // 5次以下：无折扣
        }
    }
    
    /**
     * 根据忠诚度分数计算折扣因子
     */
    private BigDecimal calculateLoyaltyDiscountFactor(int loyaltyScore) {
        if (loyaltyScore >= 90) {
            return BigDecimal.valueOf(0.80);  // 90-100分：8折
        } else if (loyaltyScore >= 80) {
            return BigDecimal.valueOf(0.85);  // 80-89分：85折
        } else if (loyaltyScore >= 70) {
            return BigDecimal.valueOf(0.90);  // 70-79分：9折
        } else if (loyaltyScore >= 60) {
            return BigDecimal.valueOf(0.95);  // 60-69分：95折
        } else {
            return BigDecimal.valueOf(1.00);  // 60分以下：无折扣
        }
    }
    
    /**
     * 根据客户时长计算折扣因子
     */
    private BigDecimal calculateAgeDiscountFactor(int ageMonths) {
        if (ageMonths >= 36) {
            return BigDecimal.valueOf(0.85);  // 3年以上：85折
        } else if (ageMonths >= 24) {
            return BigDecimal.valueOf(0.90);  // 2-3年：9折
        } else if (ageMonths >= 12) {
            return BigDecimal.valueOf(0.95);  // 1-2年：95折
        } else {
            return BigDecimal.valueOf(1.00);  // 1年以下：无折扣
        }
    }
    
    /**
     * 验证基础价格
     */
    private void validateBasePrice(BigDecimal basePrice) {
        if (basePrice == null) {
            throw new IllegalArgumentException("基础价格不能为空");
        }
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("基础价格必须大于0");
        }
    }
}