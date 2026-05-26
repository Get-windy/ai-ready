package cn.aiedge.erp.sales.pricing.enums;

/**
 * 价格策略类型枚举
 */
public enum PriceStrategyType {
    
    /**
     * 标准定价策略 - 基于产品成本、市场价的标准价格计算
     */
    STANDARD("standard", "标准定价"),
    
    /**
     * 客户分级定价策略 - 根据客户等级和历史采购的差异化定价
     */
    CUSTOMER_TIERED("customer_tiered", "客户分级定价"),
    
    /**
     * 促销活动定价策略 - 临时性促销活动的价格规则
     */
    PROMOTIONAL("promotional", "促销活动定价"),
    
    /**
     * 批量采购折扣策略 - 基于采购数量的阶梯式折扣计算
     */
    VOLUME_DISCOUNT("volume_discount", "批量采购折扣"),
    
    /**
     * 特批价格策略 - 需要通过审批的特殊价格
     */
    SPECIAL_APPROVAL("special_approval", "特批价格"),
    
    /**
     * 复合价格策略 - 多个策略的组合
     */
    COMPOSITE("composite", "复合价格策略"),
    
    /**
     * 动态价格策略 - 根据市场条件动态调整的价格
     */
    DYNAMIC("dynamic", "动态价格策略"),
    
    /**
     * 区域价格策略 - 根据地区差异定价
     */
    REGIONAL("regional", "区域价格策略"),
    
    /**
     * 时间价格策略 - 根据时间段定价
     */
    TIME_BASED("time_based", "时间价格策略"),
    
    /**
     * 自定义规则价格策略 - 基于自定义规则定价
     */
    CUSTOM_RULE("custom_rule", "自定义规则价格策略");
    
    private final String code;
    private final String description;
    
    PriceStrategyType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据编码获取枚举值
     */
    public static PriceStrategyType fromCode(String code) {
        for (PriceStrategyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的价格策略类型编码: " + code);
    }
    
    /**
     * 验证是否为有效的策略类型
     */
    public static boolean isValid(String code) {
        for (PriceStrategyType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取所有策略类型编码
     */
    public static String[] getAllCodes() {
        PriceStrategyType[] types = values();
        String[] codes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            codes[i] = types[i].code;
        }
        return codes;
    }
    
    /**
     * 检查是否是基础定价策略类型
     */
    public boolean isBaseStrategyType() {
        return this == STANDARD || this == CUSTOMER_TIERED || this == VOLUME_DISCOUNT;
    }
    
    /**
     * 检查是否需要审批的策略类型
     */
    public boolean requiresApproval() {
        return this == SPECIAL_APPROVAL;
    }
    
    /**
     * 检查是否是促销类策略
     */
    public boolean isPromotional() {
        return this == PROMOTIONAL;
    }
    
    /**
     * 检查是否是动态调整的策略
     */
    public boolean isDynamic() {
        return this == DYNAMIC || this == TIME_BASED;
    }
}