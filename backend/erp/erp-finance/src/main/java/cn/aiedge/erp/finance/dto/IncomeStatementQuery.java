package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 利润表查询条件
 *
 * 对标 ql361：查询方案 / 单-多会计月切换 / 会计月(止) / 科目层级 / 显示为0科目
 */
@Data
public class IncomeStatementQuery {

    /** 会计年度 */
    private Integer fiscalYear;

    /** 会计月(止)：本期发生额的期止月 */
    private Integer fiscalPeriod;

    /** 期间模式：single-单会计月 multi-多会计月 */
    private String periodMode = "single";

    /** 多会计月模式的起始期间（单会计月模式为空） */
    private Integer startPeriod;

    /** 科目层级：报表项目行展开到的最大科目层级，默认 2（对标默认 2） */
    private Integer subjectLevel = 2;

    /** 是否显示本期与累计均为 0 的科目行（对标默认不勾选 = 隐藏 0 行） */
    private Boolean showZero = Boolean.FALSE;

    public Integer resolveEndPeriod() {
        if (fiscalPeriod != null) {
            return fiscalPeriod;
        }
        return startPeriod != null ? startPeriod : java.time.LocalDate.now().getMonthValue();
    }

    /**
     * 多会计月模式：本期发生额 = [startPeriod, endPeriod] 区间合计，缺省 1 月至止月。
     * 单会计月模式：本期发生额 = 止月当月发生。
     */
    public Integer resolveStartPeriod() {
        if (!isMultiPeriod()) {
            return resolveEndPeriod();
        }
        return startPeriod != null ? startPeriod : 1;
    }

    public boolean isMultiPeriod() {
        return "multi".equalsIgnoreCase(periodMode);
    }
}
