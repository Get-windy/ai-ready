package cn.aiedge.erp.purchase.enums;

import lombok.Getter;

/**
 * 供应商级别枚举
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum SupplierLevel {
    
    /**
     * 战略供应商
     * - 提供关键产品或服务
     * - 长期合作关系
     * - 高价值采购
     * - 需要深度协作
     */
    STRATEGIC("STRATEGIC", "战略供应商"),
    
    /**
     * 重要供应商
     * - 提供重要产品或服务
     * - 中期合作关系
     * - 中等价值采购
     * - 需要定期评估
     */
    IMPORTANT("IMPORTANT", "重要供应商"),
    
    /**
     * 普通供应商
     * - 提供常规产品或服务
     * - 短期合作关系
     * - 低价值采购
     * - 按需合作
     */
    GENERAL("GENERAL", "普通供应商"),
    
    /**
     * 备选供应商
     * - 潜在合作对象
     * - 需要进一步评估
     * - 作为备用选择
     */
    ALTERNATIVE("ALTERNATIVE", "备选供应商"),
    
    /**
     * 临时供应商
     * - 一次性采购
     * - 紧急需求
     * - 特殊项目
     */
    TEMPORARY("TEMPORARY", "临时供应商"),
    
    /**
     * 试用供应商
     * - 正在试用期
     * - 需要考核评估
     * - 小批量合作
     */
    TRIAL("TRIAL", "试用供应商"),
    
    /**
     * 黑名单供应商
     * - 存在严重问题
     * - 暂停合作
     * - 需要特别审批
     */
    BLACKLISTED("BLACKLISTED", "黑名单供应商"),
    
    /**
     * 暂停供应商
     * - 暂时停止合作
     * - 需要整改
     * - 观察期
     */
    SUSPENDED("SUSPENDED", "暂停供应商");
    
    private final String code;
    private final String description;
    
    SupplierLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static SupplierLevel fromCode(String code) {
        for (SupplierLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return GENERAL;
    }
    
    /**
     * 判断是否为正式供应商
     */
    public boolean isFormalSupplier() {
        return this == STRATEGIC || this == IMPORTANT || this == GENERAL;
    }
    
    /**
     * 判断是否为高风险供应商
     */
    public boolean isHighRiskSupplier() {
        return this == BLACKLISTED || this == SUSPENDED;
    }
    
    /**
     * 判断是否需要特殊审批
     */
    public boolean requiresSpecialApproval() {
        return this == BLACKLISTED || this == SUSPENDED || this == TRIAL;
    }
    
    /**
     * 获取推荐合作方式
     */
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
    
    /**
     * 获取评估频率建议
     */
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