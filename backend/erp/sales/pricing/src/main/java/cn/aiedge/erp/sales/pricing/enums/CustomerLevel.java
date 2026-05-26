package cn.aiedge.erp.sales.pricing.enums;

/**
 * 客户等级枚举
 */
public enum CustomerLevel {
    
    /**
     * VIP客户 - 最高级别客户，享有最大折扣
     */
    VIP("vip", "VIP客户", 1, 0.90, "享受最大折扣优惠"),
    
    /**
     * 金卡客户 - 高级别客户，享有较大折扣
     */
    GOLD("gold", "金卡客户", 2, 0.92, "享受高级别折扣"),
    
    /**
     * 银卡客户 - 中等级别客户，享有中等折扣
     */
    SILVER("silver", "银卡客户", 3, 0.95, "享受中等折扣"),
    
    /**
     * 普通客户 - 基础级别客户，享有基本折扣
     */
    STANDARD("standard", "普通客户", 4, 0.98, "享受基本折扣"),
    
    /**
     * 新客户 - 首次购买客户，可能有促销折扣
     */
    NEW("new", "新客户", 5, 1.00, "新客户首单优惠"),
    
    /**
     * 试用客户 - 试用期客户，可能有特殊定价
     */
    TRIAL("trial", "试用客户", 6, 1.00, "试用期特殊定价"),
    
    /**
     * 企业客户 - 企业级客户，享有协议价格
     */
    ENTERPRISE("enterprise", "企业客户", 0, 0.85, "企业协议价格"),
    
    /**
     * 政府客户 - 政府机构客户，享有政策优惠
     */
    GOVERNMENT("government", "政府客户", 0, 0.88, "政府采购优惠");
    
    private final String code;
    private final String description;
    private final int priority;
    private final double baseDiscountFactor;
    private final String privilegeDescription;
    
    CustomerLevel(String code, String description, int priority, double baseDiscountFactor, String privilegeDescription) {
        this.code = code;
        this.description = description;
        this.priority = priority;
        this.baseDiscountFactor = baseDiscountFactor;
        this.privilegeDescription = privilegeDescription;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public double getBaseDiscountFactor() {
        return baseDiscountFactor;
    }
    
    public String getPrivilegeDescription() {
        return privilegeDescription;
    }
    
    /**
     * 根据编码获取枚举值
     */
    public static CustomerLevel fromCode(String code) {
        for (CustomerLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return STANDARD; // 默认返回普通客户
    }
    
    /**
     * 验证是否为有效的客户等级
     */
    public static boolean isValid(String code) {
        for (CustomerLevel level : values()) {
            if (level.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取所有客户等级编码
     */
    public static String[] getAllCodes() {
        CustomerLevel[] levels = values();
        String[] codes = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            codes[i] = levels[i].code;
        }
        return codes;
    }
    
    /**
     * 获取高等级客户（VIP、金卡、银卡）
     */
    public static CustomerLevel[] getHighLevels() {
        return new CustomerLevel[]{VIP, GOLD, SILVER, ENTERPRISE};
    }
    
    /**
     * 获取低等级客户（普通、新客户、试用客户）
     */
    public static CustomerLevel[] getLowLevels() {
        return new CustomerLevel[]{STANDARD, NEW, TRIAL};
    }
    
    /**
     * 检查是否为高等级客户
     */
    public boolean isHighLevel() {
        return this == VIP || this == GOLD || this == SILVER || this == ENTERPRISE || this == GOVERNMENT;
    }
    
    /**
     * 检查是否有资格享受折扣
     */
    public boolean isEligibleForDiscount() {
        return this != TRIAL; // 试用客户通常不享受折扣
    }
    
    /**
     * 获取推荐的最低折扣率
     */
    public double getRecommendedMinDiscount() {
        if (this == VIP) return 0.10; // VIP客户至少10%折扣
        if (this == GOLD) return 0.08; // 金卡客户至少8%折扣
        if (this == SILVER) return 0.05; // 银卡客户至少5%折扣
        if (this == ENTERPRISE) return 0.15; // 企业客户至少15%折扣
        if (this == GOVERNMENT) return 0.12; // 政府客户至少12%折扣
        return 0.0; // 其他客户无最低折扣要求
    }
    
    /**
     * 获取最大折扣率限制
     */
    public double getMaxDiscountLimit() {
        if (this == VIP) return 0.40; // VIP客户最大40%折扣
        if (this == GOLD) return 0.30; // 金卡客户最大30%折扣
        if (this == SILVER) return 0.25; // 银卡客户最大25%折扣
        if (this == ENTERPRISE) return 0.50; // 企业客户最大50%折扣
        if (this == GOVERNMENT) return 0.45; // 政府客户最大45%折扣
        return 0.20; // 其他客户最大20%折扣
    }
}