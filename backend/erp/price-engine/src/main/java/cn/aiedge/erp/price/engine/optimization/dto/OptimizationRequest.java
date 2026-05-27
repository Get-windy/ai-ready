package cn.aiedge.erp.price.engine.optimization.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record OptimizationRequest(
        String requestId,
        OptimizationType optimizationType,
        OptimizationObjective objective,
        List<PriceVariable> priceVariables,
        List<Constraint> constraints,
        Map<String, Object> context,
        OptimizationParameters parameters,
        String algorithm
) {
    public enum OptimizationType {
        SINGLE_PRODUCT,
        MULTI_PRODUCT,
        PORTFOLIO,
        PROMOTIONAL
    }
    
    public enum OptimizationObjective {
        MAXIMIZE_PROFIT,
        MAXIMIZE_REVENUE,
        MAXIMIZE_MARKET_SHARE,
        BALANCED,
        CUSTOM
    }
}

record Constraint(
        String constraintId,
        ConstraintType constraintType,
        String expression,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        String description,
        ConstraintSeverity severity
) {
    enum ConstraintType {
        PRICE_RANGE,
        MARGIN_REQUIREMENT,
        VOLUME_TARGET,
        COMPETITIVE_PRICING,
        REGULATORY,
        CUSTOM
    }
    
    enum ConstraintSeverity {
        HARD,
        SOFT,
        INFORMATIONAL
    }
}

record OptimizationParameters(
        int maxIterations,
        int populationSize,
        double mutationRate,
        double crossoverRate,
        double convergenceThreshold,
        int randomSeed,
        boolean parallelExecution,
        int timeoutSeconds
) {}