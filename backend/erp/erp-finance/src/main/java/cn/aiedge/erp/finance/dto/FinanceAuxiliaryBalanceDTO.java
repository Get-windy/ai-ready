package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 辅助核算余额DTO
 */
@Data
public class FinanceAuxiliaryBalanceDTO {

    private Long id;

    /**
     * 会计期间ID
     */
    private Long accountingPeriodId;

    /**
     * 科目ID
     */
    private Long subjectId;

    /**
     * 辅助核算类型ID
     */
    private Long auxiliaryTypeId;

    /**
     * 辅助核算类型名称
     */
    private String auxiliaryTypeName;

    /**
     * 辅助核算项目ID
     */
    private Long auxiliaryItemId;

    /**
     * 辅助核算项目名称
     */
    private String auxiliaryItemName;

    /**
     * 期初借方
     */
    private BigDecimal beginDebit;

    /**
     * 期初贷方
     */
    private BigDecimal beginCredit;

    /**
     * 本期借方
     */
    private BigDecimal periodDebit;

    /**
     * 本期贷方
     */
    private BigDecimal periodCredit;

    /**
     * 期末借方
     */
    private BigDecimal endDebit;

    /**
     * 期末贷方
     */
    private BigDecimal endCredit;

    /**
     * 本年累计借方
     */
    private BigDecimal yearDebit;

    /**
     * 本年累计贷方
     */
    private BigDecimal yearCredit;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
