package cn.aiedge.erp.invoice.model.enums;

/**
 * 付款方式枚举
 */
public enum PaymentMethod {
    
    /**
     * 银行转账
     */
    BANK_TRANSFER("银行转账", "BANK_TRANSFER"),
    
    /**
     * 现金
     */
    CASH("现金", "CASH"),
    
    /**
     * 支票
     */
    CHECK("支票", "CHECK"),
    
    /**
     * 信用卡
     */
    CREDIT_CARD("信用卡", "CREDIT_CARD"),
    
    /**
     * 借记卡
     */
    DEBIT_CARD("借记卡", "DEBIT_CARD"),
    
    /**
     * 在线支付
     */
    ONLINE_PAYMENT("在线支付", "ONLINE_PAYMENT"),
    
    /**
     * 支付宝
     */
    ALIPAY("支付宝", "ALIPAY"),
    
    /**
     * 微信支付
     */
    WECHAT_PAY("微信支付", "WECHAT_PAY"),
    
    /**
     * PayPal
     */
    PAYPAL("PayPal", "PAYPAL"),
    
    /**
     * 汇票
     */
    DRAFT("汇票", "DRAFT"),
    
    /**
     * 信用证
     */
    LETTER_OF_CREDIT("信用证", "LETTER_OF_CREDIT"),
    
    /**
     * 承兑汇票
     */
    ACCEPTANCE_BILL("承兑汇票", "ACCEPTANCE_BILL"),
    
    /**
     * 电汇
     */
    TELEGRAPHIC_TRANSFER("电汇", "TELEGRAPHIC_TRANSFER"),
    
    /**
     * 本票
     */
    PROMISSORY_NOTE("本票", "PROMISSORY_NOTE"),
    
    /**
     * 代金券
     */
    VOUCHER("代金券", "VOUCHER"),
    
    /**
     * 积分抵扣
     */
    POINTS_DEDUCTION("积分抵扣", "POINTS_DEDUCTION"),
    
    /**
     * 分期付款
     */
    INSTALLMENT("分期付款", "INSTALLMENT"),
    
    /**
     * 预付款
     */
    ADVANCE_PAYMENT("预付款", "ADVANCE_PAYMENT"),
    
    /**
     * 赊销
     */
    CREDIT_SALE("赊销", "CREDIT_SALE"),
    
    /**
     * 其他
     */
    OTHER("其他", "OTHER");
    
    private final String displayName;
    private final String code;
    
    PaymentMethod(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 是否为电子支付
     */
    public boolean isElectronic() {
        return this == ONLINE_PAYMENT || this == ALIPAY || this == WECHAT_PAY || this == PAYPAL;
    }
    
    /**
     * 是否为现金支付
     */
    public boolean isCash() {
        return this == CASH;
    }
    
    /**
     * 是否为银行支付
     */
    public boolean isBankPayment() {
        return this == BANK_TRANSFER || this == TELEGRAPHIC_TRANSFER;
    }
    
    /**
     * 是否为卡片支付
     */
    public boolean isCardPayment() {
        return this == CREDIT_CARD || this == DEBIT_CARD;
    }
    
    /**
     * 是否为票据支付
     */
    public boolean isInstrumentPayment() {
        return this == CHECK || this == DRAFT || this == ACCEPTANCE_BILL || this == PROMISSORY_NOTE || this == LETTER_OF_CREDIT;
    }
    
    /**
     * 从编码获取枚举
     */
    public static PaymentMethod fromCode(String code) {
        for (PaymentMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未知的付款方式编码: " + code);
    }
    
    /**
     * 从显示名称获取枚举
     */
    public static PaymentMethod fromDisplayName(String displayName) {
        for (PaymentMethod method : values()) {
            if (method.getDisplayName().equals(displayName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("未知的付款方式名称: " + displayName);
    }
}