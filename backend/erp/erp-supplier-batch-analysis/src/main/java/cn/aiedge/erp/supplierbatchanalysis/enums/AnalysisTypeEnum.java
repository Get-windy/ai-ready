package cn.aiedge.erp.supplierbatchanalysis.enums;

import lombok.Getter;

/**
 * 关联分析类型枚举
 *
 * @author team-member
 * @date 2026-04-30
 */
@Getter
public enum AnalysisTypeEnum {
    
    MONTHLY("MONTHLY", "月度分析", "每月自动执行的供应商-批次质量关联分析"),
    QUARTERLY("QUARTERLY", "季度分析", "每季度执行的分析，包含更全面的数据统计"),
    YEARLY("YEARLY", "年度分析", "年度综合评估分析"),
    CUSTOM("CUSTOM", "自定义分析", "用户自定义时间范围和参数的专项分析"),
    ADHOC("ADHOC", "临时分析", "针对特定事件或问题的临时分析"),
    PREDICTIVE("PREDICTIVE", "预测性分析", "基于历史数据的预测模型分析"),
    DIAGNOSTIC("DIAGNOSTIC", "诊断性分析", "针对异常情况的根因分析"),
    CORRELATION("CORRELATION", "相关性分析", "专门计算相关性系数的分析"),
    TREND("TREND", "趋势分析", "供应商和批次质量的变化趋势分析"),
    COMPARATIVE("COMPARATIVE", "对比分析", "多个供应商之间的对比分析");
    
    private final String code;
    private final String description;
    private final String detail;
    
    AnalysisTypeEnum(String code, String description, String detail) {
        this.code = code;
        this.description = description;
        this.detail = detail;
    }
    
    /**
     * 根据code获取枚举
     */
    public static AnalysisTypeEnum getByCode(String code) {
        for (AnalysisTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return CUSTOM;
    }
    
    /**
     * 获取分析频率（天）
     */
    public int getFrequencyDays() {
        switch (this) {
            case MONTHLY: return 30;
            case QUARTERLY: return 90;
            case YEARLY: return 365;
            default: return 0; // 自定义或临时分析无固定频率
        }
    }
    
    /**
     * 是否为定期分析
     */
    public boolean isPeriodic() {
        return this == MONTHLY || this == QUARTERLY || this == YEARLY;
    }
    
    /**
     * 是否需要自动执行
     */
    public boolean isAutoExecute() {
        return isPeriodic();
    }
}