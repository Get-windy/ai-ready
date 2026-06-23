package cn.aiedge.erp.purchase.enums;

/**
 * 采购需求状态枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public enum DemandStatus {
    
    /**
     * 草稿状态
     */
    DRAFT(1, "草稿"),
    
    /**
     * 已提交状态
     */
    SUBMITTED(2, "已提交"),
    
    /**
     * 已审批状态
     */
    APPROVED(3, "已审批"),
    
    /**
     * 已转为询价状态
     */
    CONVERTED_TO_INQUIRY(4, "已转为询价"),
    
    /**
     * 已取消状态
     */
    CANCELLED(5, "已取消");
    
    private final int value;
    private final String description;
    
    DemandStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据值获取枚举
     */
    public static DemandStatus fromValue(int value) {
        for (DemandStatus status : DemandStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的需求状态值: " + value);
    }
    
    /**
     * 检查是否为草稿状态
     */
    public boolean isDraft() {
        return this == DRAFT;
    }
    
    /**
     * 检查是否为已提交状态
     */
    public boolean isSubmitted() {
        return this == SUBMITTED;
    }
    
    /**
     * 检查是否为已审批状态
     */
    public boolean isApproved() {
        return this == APPROVED;
    }
    
    /**
     * 检查是否可以转换为询价
     */
    public boolean canConvertToInquiry() {
        return this == APPROVED;
    }
}