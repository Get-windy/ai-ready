package cn.aiedge.erp.sales.pricing.enums;

/**
 * 促销类型枚举
 */
public enum PromotionType {
    
    /**
     * 直接折扣 - 直接按比例打折
     */
    DIRECT_DISCOUNT("direct_discount", "直接折扣", "price * (1 - discountRate)"),
    
    /**
     * 满减优惠 - 满一定金额减一定金额
     */
    REBATE("rebate", "满减优惠", "if (amount >= conditionAmount) price - rebateAmount"),
    
    /**
     * 满折优惠 - 满一定金额按比例打折
     */
    AMOUNT_DISCOUNT("amount_discount", "满折优惠", "if (amount >= conditionAmount) price * discountRate"),
    
    /**
     * 买赠优惠 - 购买一定数量赠送商品
     */
    BUY_GET_FREE("buy_get_free", "买赠优惠", "if (quantity >= conditionQuantity) freeQuantity"),
    
    /**
     * 阶梯折扣 - 根据购买数量阶梯式打折
     */
    TIERED_DISCOUNT("tiered_discount", "阶梯折扣", "price * tierDiscountRate[quantity]"),
    
    /**
     * 限时抢购 - 特定时间段的优惠
     */
    FLASH_SALE("flash_sale", "限时抢购", "specialPrice during time window"),
    
    /**
     * 组合优惠 - 多个商品组合购买优惠
     */
    BUNDLE_DISCOUNT("bundle_discount", "组合优惠", "bundlePrice for product set"),
    
    /**
     * 会员专享 - 仅限会员享受的优惠
     */
    MEMBER_EXCLUSIVE("member_exclusive", "会员专享", "membersOnly discount"),
    
    /**
     * 首单优惠 - 首次购买享受的优惠
     */
    FIRST_ORDER("first_order", "首单优惠", "discount for first order"),
    
    /**
     * 积分抵扣 - 使用积分抵扣金额
     */
    POINTS_DEDUCTION("points_deduction", "积分抵扣", "price - (points / pointsToMoneyRatio)"),
    
    /**
     * 优惠券抵扣 - 使用优惠券抵扣金额
     */
    COUPON_DEDUCTION("coupon_deduction", "优惠券抵扣", "price - couponValue");
    
    private final String code;
    private final String description;
    private final String formulaDescription;
    
    PromotionType(String code, String description, String formulaDescription) {
        this.code = code;
        this.description = description;
        this.formulaDescription = formulaDescription;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getFormulaDescription() {
        return formulaDescription;
    }
    
    /**
     * 根据编码获取枚举值
     */
    public static PromotionType fromCode(String code) {
        for (PromotionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的促销类型编码: " + code);
    }
    
    /**
     * 验证是否为有效的促销类型
     */
    public static boolean isValid(String code) {
        for (PromotionType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取所有促销类型编码
     */
    public static String[] getAllCodes() {
        PromotionType[] types = values();
        String[] codes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            codes[i] = types[i].code;
        }
        return codes;
    }
    
    /**
     * 检查是否为金额相关的促销类型
     */
    public boolean isAmountBased() {
        return this == REBATE || this == AMOUNT_DISCOUNT;
    }
    
    /**
     * 检查是否为数量相关的促销类型
     */
    public boolean isQuantityBased() {
        return this == BUY_GET_FREE || this == TIERED_DISCOUNT;
    }
    
    /**
     * 检查是否为时间相关的促销类型
     */
    public boolean isTimeBased() {
        return this == FLASH_SALE;
    }
    
    /**
     * 检查是否为会员相关的促销类型
     */
    public boolean isMemberBased() {
        return this == MEMBER_EXCLUSIVE || this == FIRST_ORDER;
    }
    
    /**
     * 检查是否为直接价格调整的促销类型
     */
    public boolean isDirectPriceAdjustment() {
        return this == DIRECT_DISCOUNT || this == REBATE || this == AMOUNT_DISCOUNT;
    }
    
    /**
     * 检查是否需要条件验证的促销类型
     */
    public boolean requiresConditionCheck() {
        return this == REBATE || this == AMOUNT_DISCOUNT || this == BUY_GET_FREE || this == TIERED_DISCOUNT;
    }
    
    /**
     * 获取促销类型的计算复杂度
     */
    public int getCalculationComplexity() {
        switch (this) {
            case DIRECT_DISCOUNT:
                return 1; // 简单计算
            case REBATE:
            case AMOUNT_DISCOUNT:
                return 2; // 条件判断+计算
            case BUY_GET_FREE:
                return 3; // 数量条件+赠品计算
            case TIERED_DISCOUNT:
                return 4; // 阶梯查询+计算
            case FLASH_SALE:
                return 3; // 时间验证+价格计算
            case BUNDLE_DISCOUNT:
                return 5; // 组合商品验证+价格计算
            default:
                return 2;
        }
    }
}