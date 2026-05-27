package cn.aiedge.erp.price.engine.execution.dto;

import java.util.List;
import java.util.Map;

public record ExecutionTarget(
        String targetId,
        TargetType targetType,
        String referenceId,
        Map<String, Object> targetData,
        List<ExecutionAction> actions
) {
    public enum TargetType {
        PRODUCT,
        CUSTOMER,
        ORDER,
        CATEGORY,
        REGION,
        CHANNEL
    }
}