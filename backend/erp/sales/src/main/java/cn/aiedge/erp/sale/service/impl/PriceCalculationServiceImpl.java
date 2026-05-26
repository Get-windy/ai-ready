package cn.aiedge.erp.sale.service.impl;

import cn.aiedge.erp.sale.dto.PriceCalculationRequest;
import cn.aiedge.erp.sale.dto.PriceCalculationResult;
import cn.aiedge.erp.sale.entity.PriceRule;
import cn.aiedge.erp.sale.entity.PriceStrategy;
import cn.aiedge.erp.sale.entity.PromotionActivity;
import cn.aiedge.erp.sale.mapper.PriceRuleMapper;
import cn.aiedge.erp.sale.mapper.PriceStrategyMapper;
import cn.aiedge.erp.sale.mapper.PromotionActivityMapper;
import cn.aiedge.erp.sale.service.IPriceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 价格计算服务实现
 * 核心价格计算引擎
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationServiceImpl implements IPriceCalculationService {

    private final PriceStrategyMapper priceStrategyMapper;
    private final PriceRuleMapper priceRuleMapper;
    private final PromotionActivityMapper promotionMapper;

    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        return doCalculate(request, null, false);
    }

    @Override
    public PriceCalculationResult simulatePrice(PriceCalculationRequest request) {
        return doCalculate(request, null, true);
    }

    @Override
    public PriceCalculationResult calculateWithPromotions(PriceCalculationRequest request, List<Long> promotionIds) {
        return doCalculate(request, promotionIds, false);
    }

    @Override
    public List<PriceCalculationResult.AppliedStrategyInfo> getCustomerApplicableStrategies(Long customerId, String customerLevel) {
        List<PriceStrategy> strategies = priceStrategyMapper.selectActiveStrategies();
        return strategies.stream()
                .filter(s -> isStrategyApplicable(s, customerLevel, null, null))
                .map(s -> {
                    PriceCalculationResult.AppliedStrategyInfo info = new PriceCalculationResult.AppliedStrategyInfo();
                    info.setStrategyId(s.getId());
                    info.setStrategyName(s.getName());
                    info.setStrategyType(s.getStrategyType());
                    info.setDiscountAmount(BigDecimal.ZERO);
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean validateStrategy(Long strategyId) {
        PriceStrategy strategy = priceStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            return false;
        }
        List<PriceRule> rules = priceRuleMapper.selectByStrategyId(strategyId);
        // 验证规则表达式是否有效
        for (PriceRule rule : rules) {
            if (!isValidExpression(rule.getConditionExpression()) ||
                !isValidExpression(rule.getCalculationExpression())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行价格计算
     */
    private PriceCalculationResult doCalculate(PriceCalculationRequest request,
                                               List<Long> specifiedPromotionIds,
                                               boolean isSimulation) {
        PriceCalculationResult result = new PriceCalculationResult();
        List<PriceCalculationResult.PriceCalculationItemResult> itemResults = new ArrayList<>();
        List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies = new ArrayList<>();
        List<PriceCalculationResult.AppliedPromotionInfo> appliedPromotions = new ArrayList<>();

        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal discountedTotal = BigDecimal.ZERO;

        // 1. 计算每个明细项的价格
        for (PriceCalculationRequest.PriceCalculationItem item : request.getItems()) {
            PriceCalculationResult.PriceCalculationItemResult itemResult = calculateItemPrice(
                    item, request, appliedStrategies);
            itemResults.add(itemResult);
            originalTotal = originalTotal.add(itemResult.getSubtotal());
            discountedTotal = discountedTotal.add(itemResult.getDiscountedSubtotal());
        }

        // 2. 应用促销活动
        BigDecimal promotionDiscount = applyPromotions(
                request, discountedTotal, specifiedPromotionIds, appliedPromotions);
        discountedTotal = discountedTotal.subtract(promotionDiscount);

        // 3. 计算附加费用（简化实现，实际可根据配置计算运费、安装费等）
        BigDecimal additionalCharges = BigDecimal.ZERO;

        // 4. 计算税费（假设税率13%）
        BigDecimal taxRate = new BigDecimal("0.13");
        BigDecimal taxAmount = discountedTotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        // 5. 计算最终价格
        BigDecimal finalTotal = discountedTotal.add(additionalCharges).add(taxAmount);

        // 6. 组装结果
        result.setOriginalTotalAmount(originalTotal);
        result.setDiscountedTotalAmount(discountedTotal);
        result.setAdditionalCharges(additionalCharges);
        result.setTaxAmount(taxAmount);
        result.setFinalTotalAmount(finalTotal);
        result.setTotalDiscountAmount(originalTotal.subtract(discountedTotal));
        result.setItemResults(itemResults);
        result.setAppliedStrategies(appliedStrategies);
        result.setAppliedPromotions(appliedPromotions);

        if (!isSimulation) {
            log.info("价格计算完成: customerId={}, original={}, final={}",
                    request.getCustomerId(), originalTotal, finalTotal);
        }
        return result;
    }

    /**
     * 计算单个明细项的价格
     */
    private PriceCalculationResult.PriceCalculationItemResult calculateItemPrice(
            PriceCalculationRequest.PriceCalculationItem item,
            PriceCalculationRequest request,
            List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies) {

        PriceCalculationResult.PriceCalculationItemResult result =
                new PriceCalculationResult.PriceCalculationItemResult();
        result.setProductId(item.getProductId());
        result.setProductName(item.getProductName());
        result.setBasePrice(item.getBasePrice());
        result.setQuantity(item.getQuantity());

        BigDecimal subtotal = item.getBasePrice().multiply(new BigDecimal(item.getQuantity()));
        result.setSubtotal(subtotal);

        // 查询生效的价格策略
        List<PriceStrategy> strategies = priceStrategyMapper.selectActiveStrategies();
        BigDecimal discountedPrice = item.getBasePrice();
        BigDecimal itemDiscount = BigDecimal.ZERO;
        List<String> appliedRuleNames = new ArrayList<>();

        // 按优先级排序
        strategies.sort(Comparator.comparingInt(PriceStrategy::getPriority));

        for (PriceStrategy strategy : strategies) {
            if (isStrategyApplicable(strategy, request.getCustomerLevel(),
                    request.getRegionCode(), item.getProductCategoryId())) {

                // 查询策略下的规则
                List<PriceRule> rules = priceRuleMapper.selectByStrategyId(strategy.getId());
                for (PriceRule rule : rules) {
                    if (isRuleConditionMet(rule, item.getQuantity(), subtotal)) {
                        // 应用规则
                        BigDecimal beforePrice = discountedPrice;
                        discountedPrice = applyRule(rule, discountedPrice);
                        itemDiscount = itemDiscount.add(
                                beforePrice.subtract(discountedPrice).multiply(new BigDecimal(item.getQuantity())));
                        appliedRuleNames.add(rule.getName());

                        // 记录应用的策略
                        recordAppliedStrategy(strategy, appliedStrategies);
                    }
                }
            }
        }

        result.setDiscountedUnitPrice(discountedPrice);
        result.setDiscountedSubtotal(discountedPrice.multiply(new BigDecimal(item.getQuantity())));
        result.setDiscountAmount(itemDiscount);
        result.setAppliedRules(appliedRuleNames);

        return result;
    }

    /**
     * 判断策略是否适用
     */
    private boolean isStrategyApplicable(PriceStrategy strategy, String customerLevel,
                                         String regionCode, Long productCategoryId) {
        if (!"active".equals(strategy.getStatus())) {
            return false;
        }
        // 检查时间范围
        LocalDateTime now = LocalDateTime.now();
        if (strategy.getEffectiveStartTime() != null && now.isBefore(strategy.getEffectiveStartTime())) {
            return false;
        }
        if (strategy.getEffectiveEndTime() != null && now.isAfter(strategy.getEffectiveEndTime())) {
            return false;
        }
        // 检查客户等级
        if (strategy.getCustomerLevel() != null && !strategy.getCustomerLevel().isEmpty()) {
            if (!strategy.getCustomerLevel().equals(customerLevel)) {
                return false;
            }
        }
        // 检查区域
        if (strategy.getRegionCode() != null && !strategy.getRegionCode().isEmpty()) {
            if (!strategy.getRegionCode().equals(regionCode)) {
                return false;
            }
        }
        // 检查产品类别
        if (strategy.getProductCategoryId() != null && productCategoryId != null) {
            if (!strategy.getProductCategoryId().equals(productCategoryId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断规则条件是否满足
     */
    private boolean isRuleConditionMet(PriceRule rule, int quantity, BigDecimal amount) {
        if (rule.getMinQuantity() != null && quantity < rule.getMinQuantity()) {
            return false;
        }
        if (rule.getMaxQuantity() != null && quantity > rule.getMaxQuantity()) {
            return false;
        }
        if (rule.getMinAmount() != null && amount.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        if (rule.getMaxAmount() != null && amount.compareTo(rule.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }

    /**
     * 应用价格规则
     */
    private BigDecimal applyRule(PriceRule rule, BigDecimal currentPrice) {
        BigDecimal result = currentPrice;

        if (rule.getDiscountRate() != null) {
            result = result.multiply(BigDecimal.ONE.subtract(rule.getDiscountRate()));
        }
        if (rule.getDiscountAmount() != null) {
            result = result.subtract(rule.getDiscountAmount());
        }
        if (rule.getPriceFactor() != null) {
            result = result.multiply(rule.getPriceFactor());
        }
        if (rule.getFixedAmount() != null) {
            result = rule.getFixedAmount();
        }

        // 确保价格不为负
        return result.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 记录应用的策略（去重）
     */
    private void recordAppliedStrategy(PriceStrategy strategy,
                                       List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies) {
        boolean exists = appliedStrategies.stream()
                .anyMatch(s -> s.getStrategyId().equals(strategy.getId()));
        if (!exists) {
            PriceCalculationResult.AppliedStrategyInfo info =
                    new PriceCalculationResult.AppliedStrategyInfo();
            info.setStrategyId(strategy.getId());
            info.setStrategyName(strategy.getName());
            info.setStrategyType(strategy.getStrategyType());
            info.setDiscountAmount(BigDecimal.ZERO);
            appliedStrategies.add(info);
        }
    }

    /**
     * 应用促销活动
     */
    private BigDecimal applyPromotions(PriceCalculationRequest request, BigDecimal currentTotal,
                                       List<Long> specifiedPromotionIds,
                                       List<PriceCalculationResult.AppliedPromotionInfo> appliedPromotions) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        // 简化实现：查询当前生效的促销
        // 实际应用中需要根据 specifiedPromotionIds 或自动匹配
        return totalDiscount;
    }

    /**
     * 验证表达式是否有效（简化实现）
     */
    private boolean isValidExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return true;
        }
        // 实际应用中可以集成表达式引擎（如AviatorScript、QLExpress等）
        return true;
    }
}
