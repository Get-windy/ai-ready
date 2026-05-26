package cn.aiedge.erp.supplierbatchanalysis.enums;

import lombok.Getter;

/**
 * 质量等级枚举
 *
 * @author team-member
 * @date 2026-04-30
 */
@Getter
public enum QualityLevelEnum {
    
    EXCELLENT("EXCELLENT", "优秀", 90, 100),
    GOOD("GOOD", "良好", 80, 90),
    ACCEPTABLE("ACCEPTABLE", "合格", 70, 80),
    POOR("POOR", "较差", 60, 70),
    UNACCEPTABLE("UNACCEPTABLE", "不合格", 0, 60);
    
    private final String code;
    private final String description;
    private final int minScore;
    private final int maxScore;
    
    QualityLevelEnum(String code, String description, int minScore, int maxScore) {
        this.code = code;
        this.description = description;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }
    
    /**
     * 根据分数获取质量等级
     */
    public static QualityLevelEnum getByScore(int score) {
        for (QualityLevelEnum level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return UNACCEPTABLE;
    }
    
    /**
     * 获取等级颜色（用于前端展示）
     */
    public String getColor() {
        switch (this) {
            case EXCELLENT: return "#52c41a"; // 绿色
            case GOOD: return "#73d13d";     // 浅绿
            case ACCEPTABLE: return "#fa8c16"; // 橙色
            case POOR: return "#faad14";     // 黄色
            case UNACCEPTABLE: return "#f5222d"; // 红色
            default: return "#d9d9d9";
        }
    }
    
    /**
     * 是否为合格等级
     */
    public boolean isQualified() {
        return this != UNACCEPTABLE;
    }
}