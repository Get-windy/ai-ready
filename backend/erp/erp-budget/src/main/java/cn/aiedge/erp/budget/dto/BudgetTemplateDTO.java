package cn.aiedge.erp.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预算模板DTO
 */
@Data
public class BudgetTemplateDTO {
    private Long id;
    private String templateCode;
    private String templateName;
    private Integer fiscalYear;
    private BigDecimal totalAmount;
    private String status;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private List<BudgetTemplateItemDTO> items;
}
