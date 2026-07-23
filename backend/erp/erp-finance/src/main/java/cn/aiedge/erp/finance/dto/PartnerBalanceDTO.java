package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 往来余额表DTO
 * 每往来单位一行：应收/应付/预收/预付余额及净额
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerBalanceDTO {

    /**
     * 往来单位类型: customer-客户 supplier-供应商
     */
    private String partnerType;

    /**
     * 往来单位ID
     */
    private String partnerId;

    /**
     * 往来单位名称
     */
    private String partnerName;

    /**
     * 应收余额
     */
    private BigDecimal receivableBalance = BigDecimal.ZERO;

    /**
     * 应付余额
     */
    private BigDecimal payableBalance = BigDecimal.ZERO;

    /**
     * 预收余额
     */
    private BigDecimal preReceiptBalance = BigDecimal.ZERO;

    /**
     * 预付余额
     */
    private BigDecimal prePaymentBalance = BigDecimal.ZERO;

    /**
     * 净额 = 应收余额 - 应付余额 + 预付余额 - 预收余额
     */
    private BigDecimal netBalance = BigDecimal.ZERO;

    /**
     * 最后业务日期
     */
    private LocalDate lastBizDate;
}
