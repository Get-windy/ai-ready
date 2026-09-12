package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预算执行流水（冻结 / 释放 / 消耗）
 */
@Data
public class BudgetExecutionLogVO {

    private Long id;
    private Long budgetId;
    private Long budgetItemId;

    /** 执行类型：freeze 冻结 / unfreeze 释放 / consume 消耗 */
    private String executionType;
    private String executionTypeName;

    /** 来源单据类型：expense 费用单 / manual 手工 */
    private String sourceType;
    private String sourceTypeName;
    private String sourceNo;
    private Long sourceId;

    /** 发生金额（消耗为冲减剩余，冻结为占用） */
    private BigDecimal amount;
    private LocalDate executionDate;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
