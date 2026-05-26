package cn.aiedge.erp.purchase.enums;

public enum DemandPriority {
    
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    URGENT(4, "紧急");
    
    private final int value;
    private final String description;
    
    DemandPriority(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static DemandPriority fromValue(int value) {
        for (DemandPriority priority : DemandPriority.values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        throw new IllegalArgumentException("无效的需求优先级值: " + value);
    }
    
    public boolean isUrgent() {
        return this == URGENT;
    }
    
    public boolean isHigh() {
        return this == HIGH;
    }
    
    public int getWeight() {
        return value;
    }
}
