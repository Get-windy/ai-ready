package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 采购需求优先级枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum DemandPriority {
    
    /**
     * 低优先级
     */
    LOW(1, "低"),
    
    /**
     * 中优先级
     */
    MEDIUM(2, "中"),
    
    /**
     * 高优先级
     */
    HIGH(3, "高"),
    
    /**
     * 紧急优先级
     */
    URGENT(4, "紧急");
    
    private final int value;
    private final String description;
    
    DemandPriority(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * 根据值获取枚举
     */
    public static DemandPriority fromValue(int value) {
        for (DemandPriority priority : DemandPriority.values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        throw new IllegalArgumentException("无效的需求优先级值: " + value);
    }
    
    /**
     * 检查是否为紧急优先级
     */
    public boolean isUrgent() {
        return this == URGENT;
    }
    
    /**
     * 检查是否为高优先级
     */
    public boolean isHigh() {
        return this == HIGH;
    }
    
    /**
     * 获取优先级数值（用于排序）
     */
    public int getWeight() {
        return value;
    }
}