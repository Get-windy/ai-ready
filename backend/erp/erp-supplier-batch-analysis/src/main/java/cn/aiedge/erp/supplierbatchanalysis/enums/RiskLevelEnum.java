package cn.aiedge.erp.supplierbatchanalysis.enums;

import lombok.Getter;

/**
 * 风险等级枚举
 *
 * @author team-member
 * @date 2026-04-30
 */
@Getter
public enum RiskLevelEnum {
    
    LOW("LOW", "低风险", "风险较低，可正常处理"),
    MEDIUM("MEDIUM", "中风险", "存在一定风险，需要关注"),
    HIGH("HIGH", "高风险", "风险较高，需要立即关注"),
    CRITICAL("CRITICAL", "严重风险", "风险严重，需要紧急处理");
    
    private final String code;
    private final String description;
    private final String detail;
    
    RiskLevelEnum(String code, String description, String detail) {
        this.code = code;
        this.description = description;
        this.detail = detail;
    }
    
    /**
     * 根据风险分数计算风险等级
     * 分数范围: 0-100
     */
    public static RiskLevelEnum getByRiskScore(int score) {
        if (score >= 0 && score <= 30) {
            return LOW;
        } else if (score <= 60) {
            return MEDIUM;
        } else if (score <= 85) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
    
    /**
     * 根据预测合格率计算风险等级
     * 合格率范围: 0-1
     */
    public static RiskLevelEnum getByQualificationRate(double rate) {
        double riskScore = 100 * (1 - rate);
        return getByRiskScore((int) riskScore);
    }
    
    /**
     * 获取风险等级颜色
     */
    public String getColor() {
        switch (this) {
            case LOW: return "#52c41a";     // 绿色
            case MEDIUM: return "#faad14";  // 黄色
            case HIGH: return "#fa8c16";    // 橙色
            case CRITICAL: return "#f5222d"; // 红色
            default: return "#d9d9d9";
        }
    }
    
    /**
     * 是否需要预警
     */
    public boolean requiresWarning() {
        return this == HIGH || this == CRITICAL;
    }
    
    /**
     * 是否需要立即处理
     */
    public boolean requiresImmediateAction() {
        return this == CRITICAL;
    }
    
    /**
     * 获取处理建议
     */
    public String getActionSuggestion() {
        switch (this) {
            case LOW: return "正常处理，无需特殊关注";
            case MEDIUM: return "需要关注，定期检查";
            case HIGH: return "需要立即检查并制定应对措施";
            case CRITICAL: return "需要紧急处理，暂停相关业务";
            default: return "请检查风险状况";
        }
    }
}