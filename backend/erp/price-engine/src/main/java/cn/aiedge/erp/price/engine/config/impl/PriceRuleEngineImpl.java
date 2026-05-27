package cn.aiedge.erp.price.engine.config.impl;

import cn.aiedge.erp.price.engine.config.IPriceRuleEngine;
import cn.aiedge.erp.price.engine.config.dto.PriceCondition;
import cn.aiedge.erp.price.engine.config.dto.PriceRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PriceRuleEngineImpl implements IPriceRuleEngine {
    
    private final List<PriceRule> loadedRules = new ArrayList<>();
    private final List<PriceCondition> loadedConditions = new ArrayList<>();
    private final Map<String, Long> ruleExecutionCount = new ConcurrentHashMap<>();
    private long totalExecutions = 0;
    private long successfulExecutions = 0;
    private long failedExecutions = 0;
    private double totalExecutionTimeMs = 0;
    
    @Override
    public void loadRules(List<PriceRule> rules) {
        this.loadedRules.clear();
        if (rules != null) {
            this.loadedRules.addAll(rules);
            rules.forEach(r -> ruleExecutionCount.put(r.ruleId(), 0L));
        }
    }
    
    @Override
    public void loadConditions(List<PriceCondition> conditions) {
        this.loadedConditions.clear();
        if (conditions != null) {
            this.loadedConditions.addAll(conditions);
        }
    }
    
    @Override
    public List<PriceCondition> evaluateConditions(Map<String, Object> context) {
        if (loadedConditions.isEmpty() || context == null) {
            return Collections.emptyList();
        }
        
        return loadedConditions.stream()
                .filter(condition -> evaluateSingleCondition(condition, context))
                .collect(Collectors.toList());
    }
    
    private boolean evaluateSingleCondition(PriceCondition condition, Map<String, Object> context) {
        String field = condition.field();
        Object contextValue = context.get(field);
        
        if (contextValue == null) {
            return false;
        }
        
        PriceCondition.Operator operator = condition.operator();
        Object conditionValue = condition.value();
        
        switch (operator) {
            case EQUALS:
                return contextValue.equals(conditionValue);
            case NOT_EQUALS:
                return !contextValue.equals(conditionValue);
            case GREATER_THAN:
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() > ((Number) conditionValue).doubleValue();
                }
                return false;
            case LESS_THAN:
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() < ((Number) conditionValue).doubleValue();
                }
                return false;
            case GREATER_THAN_OR_EQUALS:
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() >= ((Number) conditionValue).doubleValue();
                }
                return false;
            case LESS_THAN_OR_EQUALS:
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() <= ((Number) conditionValue).doubleValue();
                }
                return false;
            case CONTAINS:
                if (contextValue instanceof String && conditionValue instanceof String) {
                    return ((String) contextValue).contains((String) conditionValue);
                }
                if (contextValue instanceof Collection && conditionValue != null) {
                    return ((Collection<?>) contextValue).contains(conditionValue);
                }
                return false;
            case IN:
                if (conditionValue instanceof Collection) {
                    return ((Collection<?>) conditionValue).contains(contextValue);
                }
                return false;
            case NOT_IN:
                if (conditionValue instanceof Collection) {
                    return !((Collection<?>) conditionValue).contains(contextValue);
                }
                return false;
            case BETWEEN:
                if (contextValue instanceof Number && condition.numericValue() != null) {
                    double val = ((Number) contextValue).doubleValue();
                    double target = condition.numericValue().doubleValue();
                    return val >= target * 0.9 && val <= target * 1.1;
                }
                return false;
            default:
                return false;
        }
    }
    
    @Override
    public double applyRules(double basePrice, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();
        totalExecutions++;
        
        try {
            List<PriceRule> matchingRules = getMatchingRules(context);
            
            if (matchingRules.isEmpty()) {
                successfulExecutions++;
                return basePrice;
            }
            
            double adjustedPrice = basePrice;
            
            matchingRules.sort((r1, r2) -> Integer.compare(
                    r2.executionOrder() != null ? r2.executionOrder() : 0,
                    r1.executionOrder() != null ? r1.executionOrder() : 0
            ));
            
            for (PriceRule rule : matchingRules) {
                double adjustment = calculateRuleAdjustment(rule, adjustedPrice, context);
                adjustedPrice = applyAdjustment(adjustedPrice, adjustment, rule.adjustmentType().name());
                
                ruleExecutionCount.merge(rule.ruleId(), 1L, Long::sum);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            totalExecutionTimeMs += executionTime;
            successfulExecutions++;
            
            return Math.max(0, adjustedPrice);
        } catch (Exception e) {
            failedExecutions++;
            return basePrice;
        }
    }
    
    private double calculateRuleAdjustment(PriceRule rule, double currentPrice, Map<String, Object> context) {
        BigDecimal adjustmentValue = rule.adjustmentValue();
        if (adjustmentValue == null) {
            return 0;
        }
        
        PriceRule.AdjustmentType adjustmentType = rule.adjustmentType();
        if (adjustmentType == PriceRule.AdjustmentType.ADD) {
            return adjustmentValue.doubleValue();
        } else if (adjustmentType == PriceRule.AdjustmentType.SUBTRACT) {
            return -adjustmentValue.doubleValue();
        } else if (adjustmentType == PriceRule.AdjustmentType.MULTIPLY) {
            return currentPrice * (adjustmentValue.doubleValue() - 1);
        } else if (adjustmentType == PriceRule.AdjustmentType.DIVIDE) {
            return currentPrice * (1.0 / adjustmentValue.doubleValue() - 1);
        } else if (adjustmentType == PriceRule.AdjustmentType.OVERRIDE) {
            return adjustmentValue.doubleValue() - currentPrice;
        }
        return adjustmentValue.doubleValue();
    }
    
    private double applyAdjustment(double currentPrice, double adjustment, String adjustmentType) {
        if ("discount".equals(adjustmentType) || "subtract".equals(adjustmentType)) {
            return currentPrice - adjustment;
        } else if ("increase".equals(adjustmentType) || "add".equals(adjustmentType)) {
            return currentPrice + adjustment;
        } else if ("percentage_discount".equals(adjustmentType)) {
            return currentPrice - adjustment;
        } else if ("percentage_increase".equals(adjustmentType)) {
            return currentPrice + adjustment;
        }
        
        return currentPrice - adjustment;
    }
    
    @Override
    public List<PriceRule> getMatchingRules(Map<String, Object> context) {
        if (loadedRules.isEmpty() || context == null) {
            return Collections.emptyList();
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        return loadedRules.stream()
                .filter(rule -> isRuleActive(rule, now))
                .filter(rule -> matchesRuleConditions(rule, context))
                .collect(Collectors.toList());
    }
    
    private boolean isRuleActive(PriceRule rule, LocalDateTime now) {
        return true;
    }
    
    private boolean matchesRuleConditions(PriceRule rule, Map<String, Object> context) {
        return true;
    }
    
    @Override
    public ConflictDetectionResult detectConflicts(List<PriceRule> rules) {
        List<Conflict> conflicts = new ArrayList<>();
        
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                PriceRule rule1 = rules.get(i);
                PriceRule rule2 = rules.get(j);
                
                if (hasTimeOverlap(rule1, rule2)) {
                    if (hasSameTarget(rule1, rule2)) {
                        conflicts.add(new Conflict(rule1, rule2, "target_overlap",
                                "规则目标重叠，可能导致重复折扣"));
                    }
                }
            }
        }
        
        String recommendation = conflicts.isEmpty() 
                ? "无冲突，规则可以正常应用" 
                : "建议调整规则优先级或设置互斥标记";
        
        return new ConflictDetectionResult(!conflicts.isEmpty(), conflicts, recommendation);
    }
    
    private boolean hasTimeOverlap(PriceRule rule1, PriceRule rule2) {
        return true;
    }
    
    private boolean hasSameTarget(PriceRule rule1, PriceRule rule2) {
        return rule1.ruleId().equals(rule2.ruleId());
    }
    
    @Override
    public List<PriceRule> resolveConflicts(List<PriceRule> conflictingRules, String resolutionStrategy) {
        if (conflictingRules == null || conflictingRules.isEmpty()) {
            return Collections.emptyList();
        }
        
        switch (resolutionStrategy) {
            case "highest_priority":
                return conflictingRules.stream()
                        .sorted((r1, r2) -> Integer.compare(
                                r2.executionOrder() != null ? r2.executionOrder() : 0,
                                r1.executionOrder() != null ? r1.executionOrder() : 0))
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "most_recent":
                return conflictingRules.stream()
                        .sorted((r1, r2) -> Integer.compare(
                                r2.executionOrder() != null ? r2.executionOrder() : 0,
                                r1.executionOrder() != null ? r1.executionOrder() : 0))
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "highest_discount":
                return conflictingRules.stream()
                        .sorted((r1, r2) -> {
                            BigDecimal v1 = r2.adjustmentValue() != null ? r2.adjustmentValue() : BigDecimal.ZERO;
                            BigDecimal v2 = r1.adjustmentValue() != null ? r1.adjustmentValue() : BigDecimal.ZERO;
                            return v1.compareTo(v2);
                        })
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "combine":
                return new ArrayList<>(conflictingRules);
            
            default:
                return new ArrayList<>(conflictingRules);
        }
    }
    
    @Override
    public RuleExecutionStats getExecutionStats() {
        double avgExecutionTime = totalExecutions > 0 
                ? totalExecutionTimeMs / totalExecutions : 0;
        
        return new RuleExecutionStats(
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                avgExecutionTime,
                new HashMap<>(ruleExecutionCount)
        );
    }
    
    @Override
    public void clearRules() {
        loadedRules.clear();
        loadedConditions.clear();
        ruleExecutionCount.clear();
        totalExecutions = 0;
        successfulExecutions = 0;
        failedExecutions = 0;
        totalExecutionTimeMs = 0;
    }
    
    @Override
    public void reload() {
        clearRules();
    }
}