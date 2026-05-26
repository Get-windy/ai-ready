package cn.aiedge.erp.price.engine.config.impl;

import cn.aiedge.erp.price.engine.config.IPriceRuleEngine;
import cn.aiedge.erp.price.engine.config.dto.PriceCondition;
import cn.aiedge.erp.price.engine.config.dto.PriceRule;
import org.springframework.stereotype.Component;

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
        
        String operator = condition.operator();
        Object conditionValue = condition.value();
        
        switch (operator) {
            case "equals":
                return contextValue.equals(conditionValue);
            case "not_equals":
                return !contextValue.equals(conditionValue);
            case "greater_than":
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() > ((Number) conditionValue).doubleValue();
                }
                return false;
            case "less_than":
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() < ((Number) conditionValue).doubleValue();
                }
                return false;
            case "greater_than_or_equal":
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() >= ((Number) conditionValue).doubleValue();
                }
                return false;
            case "less_than_or_equal":
                if (contextValue instanceof Number && conditionValue instanceof Number) {
                    return ((Number) contextValue).doubleValue() <= ((Number) conditionValue).doubleValue();
                }
                return false;
            case "contains":
                if (contextValue instanceof String && conditionValue instanceof String) {
                    return ((String) contextValue).contains((String) conditionValue);
                }
                if (contextValue instanceof Collection && conditionValue != null) {
                    return ((Collection<?>) contextValue).contains(conditionValue);
                }
                return false;
            case "in":
                if (conditionValue instanceof Collection) {
                    return ((Collection<?>) conditionValue).contains(contextValue);
                }
                return false;
            case "not_in":
                if (conditionValue instanceof Collection) {
                    return !((Collection<?>) conditionValue).contains(contextValue);
                }
                return false;
            case "between":
                if (condition.valueRange() != null && contextValue instanceof Number) {
                    double val = ((Number) contextValue).doubleValue();
                    double min = condition.valueRange().get(0) instanceof Number 
                            ? ((Number) condition.valueRange().get(0)).doubleValue() : 0;
                    double max = condition.valueRange().size() > 1 && condition.valueRange().get(1) instanceof Number 
                            ? ((Number) condition.valueRange().get(1)).doubleValue() : Double.MAX_VALUE;
                    return val >= min && val <= max;
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
                    r2.priority() != null ? r2.priority() : 0,
                    r1.priority() != null ? r1.priority() : 0
            ));
            
            for (PriceRule rule : matchingRules) {
                double adjustment = calculateRuleAdjustment(rule, adjustedPrice, context);
                adjustedPrice = applyAdjustment(adjustedPrice, adjustment, rule.adjustmentType());
                
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
        Double adjustmentValue = rule.adjustmentValue();
        if (adjustmentValue == null) {
            return 0;
        }
        
        String adjustmentType = rule.adjustmentType();
        if ("percentage".equals(adjustmentType)) {
            return currentPrice * (adjustmentValue / 100.0);
        } else if ("fixed".equals(adjustmentType)) {
            return adjustmentValue;
        }
        
        return adjustmentValue;
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
        if (rule.effectiveFrom() != null && now.isBefore(rule.effectiveFrom())) {
            return false;
        }
        if (rule.effectiveTo() != null && now.isAfter(rule.effectiveTo())) {
            return false;
        }
        return true;
    }
    
    private boolean matchesRuleConditions(PriceRule rule, Map<String, Object> context) {
        if (rule.conditions() == null || rule.conditions().isEmpty()) {
            return true;
        }
        
        String conditionLogic = rule.conditionLogic() != null ? rule.conditionLogic() : "and";
        
        List<Boolean> results = rule.conditions().stream()
                .map(conditionId -> loadedConditions.stream()
                        .filter(c -> c.conditionId().equals(conditionId))
                        .findFirst()
                        .map(condition -> evaluateSingleCondition(condition, context))
                        .orElse(false))
                .collect(Collectors.toList());
        
        if ("and".equals(conditionLogic)) {
            return results.stream().allMatch(r -> r);
        } else if ("or".equals(conditionLogic)) {
            return results.stream().anyMatch(r -> r);
        }
        
        return results.stream().allMatch(r -> r);
    }
    
    @Override
    public ConflictDetectionResult detectConflicts(List<PriceRule> rules) {
        List<Conflict> conflicts = new ArrayList<>();
        
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                PriceRule rule1 = rules.get(i);
                PriceRule rule2 = rules.get(j);
                
                if (hasOverlap(rule1, rule2)) {
                    if (rule1.exclusive() || rule2.exclusive()) {
                        conflicts.add(new Conflict(rule1, rule2, "exclusive_conflict",
                                "规则互斥，无法同时应用"));
                    }
                    
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
    
    private boolean hasOverlap(PriceRule rule1, PriceRule rule2) {
        if (rule1.effectiveFrom() == null || rule1.effectiveTo() == null ||
            rule2.effectiveFrom() == null || rule2.effectiveTo() == null) {
            return true;
        }
        
        return !(rule1.effectiveTo().isBefore(rule2.effectiveFrom()) ||
                 rule2.effectiveTo().isBefore(rule1.effectiveFrom()));
    }
    
    private boolean hasSameTarget(PriceRule rule1, PriceRule rule2) {
        if (rule1.targetProducts() != null && rule2.targetProducts() != null) {
            return rule1.targetProducts().stream()
                    .anyMatch(p -> rule2.targetProducts().contains(p));
        }
        
        if (rule1.targetCustomers() != null && rule2.targetCustomers() != null) {
            return rule1.targetCustomers().stream()
                    .anyMatch(c -> rule2.targetCustomers().contains(c));
        }
        
        return false;
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
                                r2.priority() != null ? r2.priority() : 0,
                                r1.priority() != null ? r1.priority() : 0))
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "most_recent":
                return conflictingRules.stream()
                        .sorted((r1, r2) -> {
                            LocalDateTime t1 = r1.effectiveFrom() != null ? r1.effectiveFrom() : LocalDateTime.MIN;
                            LocalDateTime t2 = r2.effectiveFrom() != null ? r2.effectiveFrom() : LocalDateTime.MIN;
                            return t2.compareTo(t1);
                        })
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "highest_discount":
                return conflictingRules.stream()
                        .sorted((r1, r2) -> Double.compare(
                                r2.adjustmentValue() != null ? r2.adjustmentValue() : 0,
                                r1.adjustmentValue() != null ? r1.adjustmentValue() : 0))
                        .limit(1)
                        .collect(Collectors.toList());
            
            case "combine":
                return new ArrayList<>(conflictingRules);
            
            default:
                return conflictingRules.stream()
                        .filter(r -> !r.exclusive())
                        .collect(Collectors.toList());
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