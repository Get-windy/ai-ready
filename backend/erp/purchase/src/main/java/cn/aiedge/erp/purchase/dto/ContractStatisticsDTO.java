package cn.aiedge.erp.purchase.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 合同统计DTO
 */
@Data
@Accessors(chain = true)
public class ContractStatisticsDTO {

    private Long activeCount;

    private Long completedCount;

    private Long pendingCount;

    private BigDecimal activeAmount;

    private BigDecimal completedAmount;
}