package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 供应商状态枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum SupplierStatus {
    
    /**
     * 待审核
     * - 新注册供应商
     * - 需要资质审核
     * - 不能参与采购
     */
    PENDING_APPROVAL("PENDING_APPROVAL", "待审核"),
    
    /**
     * 已批准
     * - 审核通过
     * - 可以参与采购
     * - 正常合作状态
     */
    APPROVED("APPROVED", "已批准"),
    
    /**
     * 已激活
     * - 已完成首次合作
     * - 建立正式合作关系
     * - 可以参与所有采购
     */
    ACTIVE("ACTIVE", "已激活"),
    
    /**
     * 已暂停
     * - 暂时停止合作
     * - 需要整改
     * - 不能参与新采购
     */
    SUSPENDED("SUSPENDED", "已暂停"),
    
    /**
     * 黑名单
     * - 存在严重问题
     * - 禁止合作
     * - 需要特殊审批
     */
    BLACKLISTED("BLACKLISTED", "黑名单"),
    
    /**
     * 已终止
     * - 合作关系结束
     * - 历史记录保留
     * - 不能参与任何采购
     */
    TERMINATED("TERMINATED", "已终止"),
    
    /**
     * 试用期
     * - 正在试用评估
     * - 限制性合作
     * - 需要定期评估
     */
    TRIAL("TRIAL", "试用期"),
    
    /**
     * 观察期
     * - 存在问题但未到暂停
     * - 加强监控
     * - 限制参与重要采购
     */
    UNDER_OBSERVATION("UNDER_OBSERVATION", "观察期"),
    
    /**
     * 资质过期
     * - 资质证书过期
     * - 需要重新认证
     * - 限制参与采购
     */
    CERTIFICATION_EXPIRED("CERTIFICATION_EXPIRED", "资质过期"),
    
    /**
     * 资料不全
     * - 缺少必要资料
     * - 需要补充材料
     * - 限制参与采购
     */
    INCOMPLETE("INCOMPLETE", "资料不全");
    
    private final String code;
    private final String description;
    
    SupplierStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static SupplierStatus fromCode(String code) {
        for (SupplierStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING_APPROVAL;
    }
    
    /**
     * 判断是否可参与采购
     */
    public boolean canParticipateInProcurement() {
        return this == APPROVED || this == ACTIVE || this == TRIAL;
    }
    
    /**
     * 判断是否需要特殊审批
     */
    public boolean requiresSpecialApproval() {
        return this == SUSPENDED || 
               this == BLACKLISTED || 
               this == UNDER_OBSERVATION ||
               this == CERTIFICATION_EXPIRED ||
               this == INCOMPLETE;
    }
    
    /**
     * 判断是否为正常合作状态
     */
    public boolean isNormalCooperationStatus() {
        return this == APPROVED || this == ACTIVE;
    }
    
    /**
     * 判断是否为问题状态
     */
    public boolean isProblemStatus() {
        return this == SUSPENDED || 
               this == BLACKLISTED || 
               this == UNDER_OBSERVATION ||
               this == CERTIFICATION_EXPIRED ||
               this == INCOMPLETE;
    }
    
    /**
     * 判断是否为终止状态
     */
    public boolean isTerminatedStatus() {
        return this == TERMINATED;
    }
    
    /**
     * 获取状态转换规则
     */
    public static boolean canTransition(SupplierStatus from, SupplierStatus to) {
        // 状态转换规则
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
    
    /**
     * 获取状态描述
     */
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
    
    /**
     * 获取状态颜色（用于UI显示）
     */
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