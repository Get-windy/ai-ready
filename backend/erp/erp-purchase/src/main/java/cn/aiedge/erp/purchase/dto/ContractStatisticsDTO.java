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

    /** 合同总数（前端统计卡片「合同总数」；原 DTO 缺失该字段，卡片恒空） */
    private Long totalCount;

    /** 草稿数 */
    private Long draftCount;

    private Long activeCount;

    private Long completedCount;

    private Long pendingCount;

    /** 合同总金额（前端统计卡片「合同总金额」） */
    private BigDecimal totalAmount;

    private BigDecimal activeAmount;

    private BigDecimal completedAmount;
}