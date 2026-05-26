package cn.aiedge.erp.invoice.model.enums;

/**
 * 匹配类型枚举
 */
public enum MatchingType {
    
    /**
     * 自动匹配 - 系统自动匹配发票和付款记录
     */
    AUTO("自动匹配", "AUTO"),
    
    /**
     * 手动匹配 - 用户手动匹配发票和付款记录
     */
    MANUAL("手动匹配", "MANUAL"),
    
    /**
     * 批量匹配 - 批量匹配多个发票
     */
    BATCH("批量匹配", "BATCH"),
    
    /**
     * 部分匹配 - 部分金额匹配
     */
    PARTIAL("部分匹配", "PARTIAL"),
    
    /**
     * 完全匹配 - 全额匹配
     */
    FULL("完全匹配", "FULL"),
    
    /**
     * 采购订单匹配 - 与采购订单匹配
     */
    PURCHASE_ORDER("采购订单匹配", "PURCHASE_ORDER"),
    
    /**
     * 销售订单匹配 - 与销售订单匹配
     */
    SALES_ORDER("销售订单匹配", "SALES_ORDER"),
    
    /**
     * 预付款匹配 - 预付款匹配
     */
    ADVANCE_PAYMENT("预付款匹配", "ADVANCE_PAYMENT"),
    
    /**
     * 退款匹配 - 退款匹配
     */
    REFUND("退款匹配", "REFUND"),
    
    /**
     * 冲红匹配 - 冲红发票匹配
     */
    CREDIT_NOTE("冲红匹配", "CREDIT_NOTE");
    
    private final String displayName;
    private final String code;
    
    MatchingType(String displayName, String code) {
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
     * 是否为自动匹配类型
     */
    public boolean isAuto() {
        return this == AUTO;
    }
    
    /**
     * 是否为手动匹配类型
     */
    public boolean isManual() {
        return this == MANUAL;
    }
    
    /**
     * 是否为批量匹配类型
     */
    public boolean isBatch() {
        return this == BATCH;
    }
    
    /**
     * 是否为部分匹配类型
     */
    public boolean isPartial() {
        return this == PARTIAL;
    }
    
    /**
     * 是否为完全匹配类型
     */
    public boolean isFull() {
        return this == FULL;
    }
    
    /**
     * 是否为订单匹配类型
     */
    public boolean isOrderMatching() {
        return this == PURCHASE_ORDER || this == SALES_ORDER;
    }
    
    /**
     * 从编码获取枚举
     */
    public static MatchingType fromCode(String code) {
        for (MatchingType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的匹配类型编码: " + code);
    }
    
    /**
     * 从显示名称获取枚举
     */
    public static MatchingType fromDisplayName(String displayName) {
        for (MatchingType type : values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的匹配类型名称: " + displayName);
    }
}