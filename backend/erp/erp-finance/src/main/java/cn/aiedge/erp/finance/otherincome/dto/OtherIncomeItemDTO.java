package cn.aiedge.erp.finance.otherincome.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 其他收入单-收入项明细 DTO
 */
@Data
public class OtherIncomeItemDTO {

    /** 收入编号 */
    private String incomeNo;

    /** 收入名称 */
    private String incomeName;

    /** 金额 */
    private BigDecimal amount;

    /** 收益类科目编码 */
    private String subjectCode;

    /** 备注 */
    private String remark;
}
