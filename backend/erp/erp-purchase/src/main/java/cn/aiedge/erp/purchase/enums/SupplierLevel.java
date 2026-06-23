package cn.aiedge.erp.purchase.enums;

public enum SupplierLevel {
    
    STRATEGIC("STRATEGIC", "战略供应商"),
    IMPORTANT("IMPORTANT", "重要供应商"),
    GENERAL("GENERAL", "普通供应商"),
    ALTERNATIVE("ALTERNATIVE", "备选供应商"),
    TEMPORARY("TEMPORARY", "临时供应商"),
    TRIAL("TRIAL", "试用供应商"),
    BLACKLISTED("BLACKLISTED", "黑名单供应商"),
    SUSPENDED("SUSPENDED", "暂停供应商");
    
    private final String code;
    private final String description;
    
    SupplierLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static SupplierLevel fromCode(String code) {
        for (SupplierLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return GENERAL;
    }
    
    public boolean isFormalSupplier() {
        return this == STRATEGIC || this == IMPORTANT || this == GENERAL;
    }
    
    public boolean isHighRiskSupplier() {
        return this == BLACKLISTED || this == SUSPENDED;
    }
    
    public boolean requiresSpecialApproval() {
        return this == BLACKLISTED || this == SUSPENDED || this == TRIAL;
    }
    
    public String getRecommendedCooperationMode() {
        return switch (this) {
            case STRATEGIC -> "深度合作，建立战略伙伴关系";
            case IMPORTANT -> "重点合作，建立长期稳定关系";
            case GENERAL -> "常规合作，建立标准合作关系";
            case ALTERNATIVE -> "尝试合作，进行小规模试点";
            case TEMPORARY -> "临时合作，满足紧急需求";
            case TRIAL -> "试用合作，进行综合评估";
            case BLACKLISTED -> "禁止合作，需要特殊审批";
            case SUSPENDED -> "暂停合作，等待整改完成";
        };
    }
    
    public String getEvaluationFrequency() {
        return switch (this) {
            case STRATEGIC -> "每季度评估一次";
            case IMPORTANT -> "每半年评估一次";
            case GENERAL -> "每年评估一次";
            case ALTERNATIVE -> "合作前评估一次";
            case TEMPORARY -> "按需评估";
            case TRIAL -> "每月评估一次";
            case BLACKLISTED -> "每年审查一次";
            case SUSPENDED -> "整改完成后评估";
        };
    }
}
