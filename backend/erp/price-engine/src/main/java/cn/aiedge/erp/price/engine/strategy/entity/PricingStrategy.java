package cn.aiedge.erp.price.engine.strategy.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定价策略实体类
 * 记录定价策略的配置信息、参数和生效条件
 */
public class PricingStrategy {
    
    /**
     * 策略ID（主键）
     */
    private String strategyId;
    
    /**
     * 策略名称
     */
    private String strategyName;
    
    /**
     * 策略类型
     */
    private StrategyType strategyType;
    
    /**
     * 策略描述
     */
    private String description;
    
    /**
     * 参数配置（JSON格式，存储策略具体参数）
     */
    private Map<String, Object> parameters;
    
    /**
     * 生效条件表达式
     */
    private String conditionExpression;
    
    /**
     * 优先级（数值越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 生效开始时间
     */
    private LocalDateTime effectiveFrom;
    
    /**
     * 生效结束时间
     */
    private LocalDateTime effectiveTo;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    private String createdBy;
    
    /**
     * 更新人ID
     */
    private String updatedBy;
    
    /**
     * 版本号（用于乐观锁）
     */
    private Integer version;

    // 构造函数
    public PricingStrategy() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enabled = true;
        this.priority = 100; // 默认优先级
        this.version = 1;
    }

    public PricingStrategy(String strategyId, String strategyName, StrategyType strategyType) {
        this();
        this.strategyId = strategyId;
        this.strategyName = strategyName;
        this.strategyType = strategyType;
    }

    // Getters and Setters
    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(StrategyType strategyType) {
        this.strategyType = strategyType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 判断策略是否在有效期内
     */
    public boolean isEffective() {
        if (!Boolean.TRUE.equals(enabled)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }
        
        return true;
    }

    /**
     * 更新策略信息
     */
    public void updateStrategyInfo(String strategyName, StrategyType strategyType, String description) {
        this.strategyName = strategyName;
        this.strategyType = strategyType;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    /**
     * 定价策略类型枚举
     */
    public enum StrategyType {
        COST_BASED("成本加成定价", "基于成本加一定利润率的定价策略"),
        MARKET_BASED("市场定价", "基于市场情况的定价策略"),
        VALUE_BASED("价值定价", "基于客户感知价值的定价策略"),
        COMPETITIVE_BASED("竞争定价", "基于竞争对手价格的定价策略"),
        STANDARD_DISCOUNT("标准折扣", "标准折扣定价策略"),
        PROMOTIONAL_DISCOUNT("促销折扣", "促销活动的折扣策略"),
        VOLUME_DISCOUNT("批量折扣", "基于购买数量的折扣策略"),
        BUNDLE_DISCOUNT("组合折扣", "产品组合的折扣策略");

        private final String displayName;
        private final String description;

        StrategyType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}