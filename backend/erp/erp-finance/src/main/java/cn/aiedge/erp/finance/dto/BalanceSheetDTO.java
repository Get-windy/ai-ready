package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资产负债表DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetDTO {
    private String itemCode;
    private String itemName;
    private String parentCode;
    private Integer level;
    private BigDecimal endBalance;
    private BigDecimal beginBalance;
    private String type; // asset-资产 liability-负债 equity-权益
}
