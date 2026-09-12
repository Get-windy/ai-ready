package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 资产负债表查询条件
 *
 * 对标 ql361：查询方案 / 单-多会计月切换 / 会计月(止) / 科目层级 / 显示为0科目
 */
@Data
public class BalanceSheetQuery {

    /** 会计年度 */
    private Integer fiscalYear;

    /** 会计月(止)：期末时点所在期间 */
    private Integer fiscalPeriod;

    /** 期间模式：single-单会计月 multi-多会计月 */
    private String periodMode = "single";

    /** 多会计月模式的起始期间（单会计月模式为空） */
    private Integer startPeriod;

    /** 科目层级：参与报表汇总的科目最大层级，默认 1（一级科目） */
    private Integer subjectLevel = 1;

    /** 是否显示余额为 0 的项目行，默认显示 */
    private Boolean showZero = Boolean.TRUE;

    public Integer resolveEndPeriod() {
        if (fiscalPeriod != null) {
            return fiscalPeriod;
        }
        return startPeriod != null ? startPeriod : 1;
    }

    public Integer resolveStartPeriod() {
        if (startPeriod != null) {
            return startPeriod;
        }
        return resolveEndPeriod();
    }

    public boolean isMultiPeriod() {
        return "multi".equalsIgnoreCase(periodMode);
    }
}
