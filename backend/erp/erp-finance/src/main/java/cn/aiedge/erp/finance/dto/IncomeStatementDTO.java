package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 利润表DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementDTO {
    private String itemCode;
    private String itemName;
    private String parentCode;
    private Integer level;
    private BigDecimal currentAmount;
    private BigDecimal cumulativeAmount;
}
