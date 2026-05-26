package cn.aiedge.erp.expense.model.enumeration;

/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    /**
     * 现金
     */
    CASH("现金", "CASH"),
    
    /**
     * 银行转账
     */
    BANK_TRANSFER("银行转账", "BANK"),
    
    /**
     * 支付宝
     */
    ALIPAY("支付宝", "ALI"),
    
    /**
     * 微信支付
     */
    WECHAT_PAY("微信支付", "WECHAT"),
    
    /**
     * 支票
     */
    CHECK("支票", "CHECK"),
    
    /**
     * 信用卡
     */
    CREDIT_CARD("信用卡", "CREDIT"),
    
    /**
     * 员工垫付
     */
    EMPLOYEE_ADVANCE("员工垫付", "ADVANCE"),
    
    /**
     * 公司账户支付
     */
    COMPANY_ACCOUNT("公司账户", "COMPANY"),
    
    /**
     * 其他
     */
    OTHER("其他", "OTHER");
    
    private final String description;
    private final String code;
    
    PaymentMethod(String description, String code) {
        this.description = description;
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 根据code获取枚举
     */
    public static PaymentMethod fromCode(String code) {
        for (PaymentMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return OTHER;
    }
    
    /**
     * 是否需要银行账户信息
     */
    public boolean requiresBankAccount() {
        return this == BANK_TRANSFER || this == COMPANY_ACCOUNT || this == CHECK;
    }
    
    /**
     * 是否需要第三方支付账户
     */
    public boolean requiresThirdPartyAccount() {
        return this == ALIPAY || this == WECHAT_PAY;
    }
    
    /**
     * 是否支持退款
     */
    public boolean supportsRefund() {
        return this == ALIPAY || this == WECHAT_PAY || this == BANK_TRANSFER || this == CREDIT_CARD;
    }
    
    /**
     * 获取支付方式描述列表
     */
    public static String[] getAllDescriptions() {
        PaymentMethod[] values = values();
        String[] descriptions = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            descriptions[i] = values[i].getDescription();
        }
        return descriptions;
    }
}