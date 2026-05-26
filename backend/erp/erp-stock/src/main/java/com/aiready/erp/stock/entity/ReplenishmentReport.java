package com.aiready.erp.stock.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReplenishmentReport {

    private Long warehouseId;
    private Integer totalSuggestions;
    private Long highPriorityCount;
    private Long mediumPriorityCount;
    private Long lowPriorityCount;
    private BigDecimal totalShortageQty;
    private BigDecimal totalSuggestedQty;
    private BigDecimal estimatedCost;
    private LocalDateTime createTime;
}