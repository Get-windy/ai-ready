package cn.aiedge.erp.pricing.config.impl;

import cn.aiedge.erp.pricing.config.IPriceConfigValidator;
import cn.aiedge.erp.pricing.config.dto.PriceStrategyConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 价格配置验证器实现
 */
@Component
public class PriceConfigValidatorImpl implements IPriceConfigValidator {

    @Override
    public ValidationResult validate(PriceStrategyConfig config) {
        Objects.requireNonNull(config, "价格策略配置不能为空");

        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // 基本必填字段验证
        if (config.strategyId() == null || config.strategyId().trim().isEmpty()) {
            errors.add(new ValidationError("CONFIG_ID_REQUIRED", "配置ID不能为空", "strategyId", Severity.HIGH, "请为配置指定唯一ID"));
        }

        if (config.strategyName() == null || config.strategyName().trim().isEmpty()) {
            errors.add(new ValidationError("CONFIG_NAME_REQUIRED", "配置名称不能为空", "strategyName", Severity.MEDIUM, "请为配置指定名称"));
        }

        if (config.strategyType() == null) {
            errors.add(new ValidationError("CONFIG_TYPE_REQUIRED", "配置类型不能为空", "strategyType", Severity.HIGH, "请指定配置类型"));
        }

        if (config.effectiveFrom() == null) {
            errors.add(new ValidationError("EFFECTIVE_FROM_REQUIRED", "生效时间不能为空", "effectiveFrom", Severity.MEDIUM, "请指定生效时间"));
        }

        // 验证条件配置
        if (config.conditions() != null && !config.conditions().isEmpty()) {
            for (int i = 0; i < config.conditions().size(); i++) {
                var condition = config.conditions().get(i);
                if (condition.field() == null || condition.field().trim().isEmpty()) {
                    errors.add(new ValidationError("CONDITION_FIELD_REQUIRED", "条件字段不能为空", "conditions[" + i + "].field", Severity.HIGH, "条件必须指定字段"));
                }
                if (condition.operator() == null) {
                    errors.add(new ValidationError("CONDITION_OPERATOR_REQUIRED", "条件操作符不能为空", "conditions[" + i + "].operator", Severity.HIGH, "条件必须指定操作符"));
                }
            }
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(
            config.strategyId(),
            isValid,
            errors,
            warnings,
            String.valueOf(System.currentTimeMillis())
        );
    }

    @Override
    public List<ValidationResult> validateBatch(List<PriceStrategyConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }

        return configs.stream()
            .map(this::validate)
            .collect(Collectors.toList());
    }

    @Override
    public CompletenessCheckResult checkCompleteness(PriceStrategyConfig config) {
        Objects.requireNonNull(config, "价格策略配置不能为空");

        List<String> missingFields = new ArrayList<>();

        if (config.strategyId() == null || config.strategyId().trim().isEmpty()) {
            missingFields.add("strategyId");
        }
        if (config.strategyName() == null || config.strategyName().trim().isEmpty()) {
            missingFields.add("strategyName");
        }
        if (config.strategyType() == null) {
            missingFields.add("strategyType");
        }
        if (config.effectiveFrom() == null) {
            missingFields.add("effectiveFrom");
        }

        // 检查规则配置
        if (config.rules() == null || config.rules().isEmpty()) {
            missingFields.add("rules");
        }

        double completenessScore = 1.0 - (double) missingFields.size() / 20.0; // 假设有20个主要字段
        completenessScore = Math.max(0.0, completenessScore);

        boolean isComplete = missingFields.isEmpty();
        String assessment = isComplete ? "配置完整" : "配置不完整";

        return new CompletenessCheckResult(isComplete, missingFields, completenessScore, assessment);
    }

    @Override
    public ConsistencyCheckResult checkConsistency(List<PriceStrategyConfig> configs) {
        if (configs == null || configs.size() <= 1) {
            return new ConsistencyCheckResult(true, new ArrayList<>(), 1.0, "配置数量不足，无需一致性检查");
        }

        List<Inconsistency> inconsistencies = new ArrayList<>();

        // 检查是否有重复的ID
        List<String> ids = configs.stream()
            .map(PriceStrategyConfig::strategyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<String> duplicateIds = ids.stream()
            .distinct()
            .collect(Collectors.toList());

        if (ids.size() != duplicateIds.size()) {
            List<String> actualDuplicates = new ArrayList<>();
            for (String id : ids) {
                long count = ids.stream().filter(s -> s.equals(id)).count();
                if (count > 1 && !actualDuplicates.contains(id)) {
                    actualDuplicates.add(id);
                }
            }
            inconsistencies.add(new Inconsistency(
                "DUPLICATE_IDS",
                "存在重复的配置ID",
                actualDuplicates,
                Severity.HIGH
            ));
        }

        // 检查同类型配置的结构一致性
        for (var type : configs.stream().map(PriceStrategyConfig::strategyType).distinct().toList()) {
            List<PriceStrategyConfig> sameTypeConfigs = configs.stream()
                .filter(c -> c.strategyType() != null && c.strategyType().equals(type))
                .toList();

            // 检查字段结构一致性（简化）
            if (sameTypeConfigs.size() > 1) {
                // 检查是否有配置包含必填字段而其他没有
                long withRules = sameTypeConfigs.stream()
                    .filter(c -> c.rules() != null && !c.rules().isEmpty())
                    .count();

                if (withRules > 0 && withRules < sameTypeConfigs.size()) {
                    inconsistencies.add(new Inconsistency(
                        "STRUCTURE_INCONSISTENCY",
                        "同类型配置的结构不一致",
                        sameTypeConfigs.stream().map(PriceStrategyConfig::strategyId).collect(Collectors.toList()),
                        Severity.MEDIUM
                    ));
                }
            }
        }

        double consistencyScore = inconsistencies.isEmpty() ? 1.0 : 0.8; // 简化评分

        return new ConsistencyCheckResult(
            inconsistencies.isEmpty(),
            inconsistencies,
            consistencyScore,
            "完成一致性检查"
        );
    }

    @Override
    public BusinessRuleValidationResult validateBusinessRules(PriceStrategyConfig config) {
        Objects.requireNonNull(config, "价格策略配置不能为空");

        List<BusinessRuleViolation> violations = new ArrayList<>();

        // 验证业务规则
        if (config.rules() != null) {
            for (int i = 0; i < config.rules().size(); i++) {
                var rule = config.rules().get(i);

                // 检查规则优先级是否合理
                if (rule.executionOrder() != null && rule.executionOrder() < 0) {
                    violations.add(new BusinessRuleViolation(
                        "INVALID_EXECUTION_ORDER",
                        "规则执行顺序不能为负数",
                        "规则 " + rule.ruleName() + " 执行顺序为 " + rule.executionOrder(),
                        Severity.HIGH,
                        "请设置大于等于0的执行顺序"
                    ));
                }

                // 验证规则的基本参数
                if (rule.ruleType() == null) {
                    violations.add(new BusinessRuleViolation(
                        "RULE_TYPE_REQUIRED",
                        "规则类型不能为空",
                        "规则 " + rule.ruleName() + " 的类型为空",
                        Severity.HIGH,
                        "请选择合适的规则类型"
                    ));
                }

                if (rule.adjustmentType() == null) {
                    violations.add(new BusinessRuleViolation(
                        "ADJUSTMENT_TYPE_REQUIRED",
                        "调整类型不能为空",
                        "规则 " + rule.ruleName() + " 的调整类型为空",
                        Severity.HIGH,
                        "请选择合适的调整类型"
                    ));
                }

                // 验证数值参数的合理性
                if (rule.baseValue() != null && rule.baseValue().compareTo(BigDecimal.ZERO) < 0) {
                    violations.add(new BusinessRuleViolation(
                        "NEGATIVE_BASE_VALUE",
                        "基础值不能为负数",
                        "规则 " + rule.ruleName() + " 的基础值为 " + rule.baseValue(),
                        Severity.HIGH,
                        "请设置正数的基础值"
                    ));
                }

                if (rule.minValue() != null && rule.maxValue() != null &&
                    rule.minValue().compareTo(rule.maxValue()) > 0) {
                    violations.add(new BusinessRuleViolation(
                        "MIN_GREATER_THAN_MAX",
                        "最小值不能大于最大值",
                        "规则 " + rule.ruleName() + " 的最小值(" + rule.minValue() + ") > 最大值(" + rule.maxValue() + ")",
                        Severity.HIGH,
                        "请确保最小值不大于最大值"
                    ));
                }
            }
        }

        return new BusinessRuleValidationResult(
            violations.isEmpty(),
            violations,
            "PRICING",
            violations.isEmpty() ? "所有业务规则验证通过" : "发现 " + violations.size() + " 个业务规则违规"
        );
    }

    @Override
    public List<ValidationRule> getValidationRules() {
        // 返回默认验证规则
        return new ArrayList<>();
    }

    @Override
    public void addValidationRule(ValidationRule rule) {
        // 添加自定义验证规则（简化实现）
    }
}