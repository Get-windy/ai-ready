package cn.aiedge.erp.price.engine.config;

import cn.aiedge.erp.price.engine.config.dto.PriceStrategyConfig;

import java.util.List;

/**
 * 价格配置验证器接口
 */
public interface IPriceConfigValidator {
    
    /**
     * 验证单个配置
     * @param config 价格策略配置
     * @return 验证结果
     */
    ValidationResult validate(PriceStrategyConfig config);
    
    /**
     * 批量验证配置
     * @param configs 配置列表
     * @return 验证结果列表
     */
    List<ValidationResult> validateBatch(List<PriceStrategyConfig> configs);
    
    /**
     * 验证配置完整性
     * @param config 配置
     * @return 完整性检查结果
     */
    CompletenessCheckResult checkCompleteness(PriceStrategyConfig config);
    
    /**
     * 验证配置一致性
     * @param configs 配置列表
     * @return 一致性检查结果
     */
    ConsistencyCheckResult checkConsistency(List<PriceStrategyConfig> configs);
    
    /**
     * 验证业务规则
     * @param config 配置
     * @return 业务规则验证结果
     */
    BusinessRuleValidationResult validateBusinessRules(PriceStrategyConfig config);
    
    /**
     * 获取验证规则
     * @return 验证规则列表
     */
    List<ValidationRule> getValidationRules();
    
    /**
     * 添加自定义验证规则
     * @param rule 验证规则
     */
    void addValidationRule(ValidationRule rule);
    
    /**
     * 验证结果
     */
    record ValidationResult(
            String configId,
            boolean isValid,
            List<ValidationError> errors,
            List<ValidationWarning> warnings,
            String validationTimestamp
    ) {}
    
    /**
     * 验证错误
     */
    record ValidationError(
            String code,
            String message,
            String field,
            Severity severity,
            String suggestion
    ) {}
    
    /**
     * 验证警告
     */
    record ValidationWarning(
            String code,
            String message,
            String field,
            String suggestion
    ) {}
    
    /**
     * 完整性检查结果
     */
    record CompletenessCheckResult(
            boolean isComplete,
            List<String> missingFields,
            double completenessScore,
            String assessment
    ) {}
    
    /**
     * 一致性检查结果
     */
    record ConsistencyCheckResult(
            boolean isConsistent,
            List<Inconsistency> inconsistencies,
            double consistencyScore,
            String recommendation
    ) {}
    
    /**
     * 不一致性
     */
    record Inconsistency(
            String type,
            String description,
            List<String> affectedConfigs,
            Severity severity
    ) {}
    
    /**
     * 业务规则验证结果
     */
    record BusinessRuleValidationResult(
            boolean passesBusinessRules,
            List<BusinessRuleViolation> violations,
            String businessDomain,
            String validationSummary
    ) {}
    
    /**
     * 业务规则违反
     */
    record BusinessRuleViolation(
            String ruleId,
            String ruleDescription,
            String violationDetails,
            Severity severity,
            String correctiveAction
    ) {}
    
    /**
     * 验证规则
     */
    interface ValidationRule {
        String getRuleId();
        String getDescription();
        boolean validate(PriceStrategyConfig config);
        ValidationError getError(PriceStrategyConfig config);
    }
    
    /**
     * 严重程度
     */
    enum Severity {
        CRITICAL,    // 严重
        HIGH,        // 高
        MEDIUM,      // 中
        LOW          // 低
    }
}