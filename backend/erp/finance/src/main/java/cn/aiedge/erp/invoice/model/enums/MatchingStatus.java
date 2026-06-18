package cn.aiedge.erp.invoice.model.enums;

/**
 * 发票匹配状态枚举
 */
public enum MatchingStatus {
    
    /**
     * 未匹配 - 发票尚未与任何付款记录匹配
     */
    UNMATCHED("未匹配", "UNMATCHED"),
    
    /**
     * 部分匹配 - 发票已部分匹配付款
     */
    PARTIALLY_MATCHED("部分匹配", "PARTIALLY_MATCHED"),
    
    /**
     * 完全匹配 - 发票已完全匹配付款
     */
    FULLY_MATCHED("完全匹配", "FULLY_MATCHED"),
    
    /**
     * 过度匹配 - 匹配金额超过发票金额
     */
    OVER_MATCHED("过度匹配", "OVER_MATCHED"),
    
    /**
     * 匹配错误 - 匹配过程中出现错误
     */
    MATCHING_ERROR("匹配错误", "MATCHING_ERROR"),
    
    /**
     * 匹配已取消 - 匹配记录已取消
     */
    MATCHING_CANCELLED("匹配已取消", "MATCHING_CANCELLED");
    
    private final String displayName;
    private final String code;
    
    MatchingStatus(String displayName, String code) {
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
     * 获取下一个状态
     */
    public MatchingStatus getNextStatus() {
        switch (this) {
            case UNMATCHED:
                return PARTIALLY_MATCHED;
            case PARTIALLY_MATCHED:
                return FULLY_MATCHED;
            case FULLY_MATCHED:
                return MATCHING_CANCELLED;
            default:
                return this;
        }
    }
    
    /**
     * 是否为未匹配状态
     */
    public boolean isUnmatched() {
        return this == UNMATCHED;
    }
    
    /**
     * 是否为已匹配状态
     */
    public boolean isMatched() {
        return this == PARTIALLY_MATCHED || this == FULLY_MATCHED;
    }
    
    /**
     * 是否为完全匹配状态
     */
    public boolean isFullyMatched() {
        return this == FULLY_MATCHED;
    }
    
    /**
     * 是否为部分匹配状态
     */
    public boolean isPartiallyMatched() {
        return this == PARTIALLY_MATCHED;
    }
    
    /**
     * 是否为错误状态
     */
    public boolean isError() {
        return this == MATCHING_ERROR || this == OVER_MATCHED;
    }
    
    /**
     * 从编码获取枚举
     */
    public static MatchingStatus fromCode(String code) {
        for (MatchingStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的匹配状态编码: " + code);
    }
    
    /**
     * 从显示名称获取枚举
     */
    public static MatchingStatus fromDisplayName(String displayName) {
        for (MatchingStatus status : values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的匹配状态名称: " + displayName);
    }
}