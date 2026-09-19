package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 会计期间起止日期保存项 DTO
 *
 * <p>专供「会计期间」页的矩阵批量保存使用：只承载主键与起止日期两个字段，
 * 避免复用 {@link AccountingPeriodDTO} 时被「会计年度/会计月份必填」的校验拦住。
 * 仅更新日历口径（start_date / end_date），不动状态、期间编码等其它列。</p>
 */
@Data
public class AccountingPeriodDateDTO {

    /**
     * 期间ID
     */
    private Long id;

    /**
     * 起始日期
     */
    private LocalDate startDate;

    /**
     * 结账日期
     */
    private LocalDate endDate;
}
