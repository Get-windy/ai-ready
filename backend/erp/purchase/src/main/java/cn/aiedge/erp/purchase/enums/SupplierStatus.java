package cn.aiedge.erp.purchase.enums;

public enum SupplierStatus {
    
    PENDING_APPROVAL("PENDING_APPROVAL", "待审核"),
    APPROVED("APPROVED", "已批准"),
    ACTIVE("ACTIVE", "已激活"),
    SUSPENDED("SUSPENDED", "已暂停"),
    BLACKLISTED("BLACKLISTED", "黑名单"),
    TERMINATED("TERMINATED", "已终止"),
    TRIAL("TRIAL", "试用期"),
    UNDER_OBSERVATION("UNDER_OBSERVATION", "观察期"),
    CERTIFICATION_EXPIRED("CERTIFICATION_EXPIRED", "资质过期"),
    INCOMPLETE("INCOMPLETE", "资料不全");
    
    private final String code;
    private final String description;
    
    SupplierStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static SupplierStatus fromCode(String code) {
        for (SupplierStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING_APPROVAL;
    }
    
    public boolean canParticipateInProcurement() {
        return this == APPROVED || this == ACTIVE || this == TRIAL;
    }
    
    public boolean requiresSpecialApproval() {
        return this == SUSPENDED || this == BLACKLISTED || 
               this == UNDER_OBSERVATION || this == CERTIFICATION_EXPIRED || this == INCOMPLETE;
    }
    
    public boolean isNormalCooperationStatus() {
        return this == APPROVED || this == ACTIVE;
    }
    
    public boolean isProblemStatus() {
        return this == SUSPENDED || this == BLACKLISTED || 
               this == UNDER_OBSERVATION || this == CERTIFICATION_EXPIRED || this == INCOMPLETE;
    }
    
    public boolean isTerminatedStatus() {
        return this == TERMINATED;
    }
    
    public static boolean canTransition(SupplierStatus from, SupplierStatus to) {
        return switch (from) {
            case PENDING_APPROVAL -> to == APPROVED || to == BLACKLISTED;
            case APPROVED -> to == ACTIVE || to == SUSPENDED || to == BLACKLISTED || to == TERMINATED;
            case ACTIVE -> to == SUSPENDED || to == BLACKLISTED || to == TERMINATED || to == UNDER_OBSERVATION;
            case SUSPENDED -> to == ACTIVE || to == BLACKLISTED || to == TERMINATED;
            case BLACKLISTED -> to == SUSPENDED || to == TERMINATED;
            case TRIAL -> to == ACTIVE || to == SUSPENDED || to == BLACKLISTED || to == TERMINATED;
            case UNDER_OBSERVATION -> to == ACTIVE || to == SUSPENDED || to == BLACKLISTED;
            case CERTIFICATION_EXPIRED -> to == APPROVED || to == SUSPENDED;
            case INCOMPLETE -> to == PENDING_APPROVAL || to == SUSPENDED;
            default -> false;
        };
    }
    
    public String getStatusDescription() {
        return switch (this) {
            case PENDING_APPROVAL -> "供应商已提交申请，等待审核";
            case APPROVED -> "供应商已通过审核，可以参与采购";
            case ACTIVE -> "供应商处于活跃状态，正常合作中";
            case SUSPENDED -> "供应商已被暂停合作，需要整改";
            case BLACKLISTED -> "供应商已被列入黑名单，禁止合作";
            case TERMINATED -> "供应商合作关系已终止";
            case TRIAL -> "供应商处于试用期，需要评估";
            case UNDER_OBSERVATION -> "供应商处于观察期，加强监控";
            case CERTIFICATION_EXPIRED -> "供应商资质证书已过期";
            case INCOMPLETE -> "供应商资料不完整，需要补充";
        };
    }
    
    public String getStatusColor() {
        return switch (this) {
            case PENDING_APPROVAL -> "orange";
            case APPROVED -> "blue";
            case ACTIVE -> "green";
            case SUSPENDED -> "yellow";
            case BLACKLISTED -> "red";
            case TERMINATED -> "gray";
            case TRIAL -> "cyan";
            case UNDER_OBSERVATION -> "yellow";
            case CERTIFICATION_EXPIRED -> "orange";
            case INCOMPLETE -> "orange";
        };
    }
}
